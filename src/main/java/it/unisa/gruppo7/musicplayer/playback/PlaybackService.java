package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.strategy.PlaylistSkipStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.SkipStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.TrackSkipStrategy;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for managing media playback simulation, state transitions,
 * background thread timer tasks, and notifying registered structural observers.
 * It provides core routing facilities for track navigation and queue progression.
 *
 * @author Francesco Lemmo
 */
public class PlaybackService implements TrackObserver{

    /** The track currently loaded in the playback engine. */
    private Track currentTrack;

    /** The active operational state of the playback engine. */
    private PlaybackState currentState;

    /** The queue managing the sequence of tracks to be played. */
    private PlaybackList queue;

    /** Atomic counter tracking the elapsed simulated playback time in seconds. */
    private AtomicInteger simulatedTimeSeconds;

    /** Service used to schedule and run background timer tasks. */
    private ScheduledExecutorService timer;

    /** Handle used to monitor or cancel the active timer task. */
    private ScheduledFuture<?> timerHandle;

    /** List of observers listening to playback state and time changes. */
    private final List<PlaybackObserver> observers;

    /** The current repeat mode setting, defaulting to OFF. */
    private RepeatMode repeatMode;

    /** Strategy used to skip the queue one track at a time. */
    private final SkipStrategy trackSkipStrategy = new TrackSkipStrategy();

    /** Strategy used to skip the queue one playlist block at a time (US-029). */
    private final SkipStrategy playlistSkipStrategy = new PlaylistSkipStrategy();


    /**
     * Constructs a new PlaybackService and allocates resource executors
     * required for multi-threaded time tracking simulation.
     */
    public PlaybackService() {
        this.currentState = PlaybackState.START_UP;
        this.simulatedTimeSeconds = new AtomicInteger(0);
        this.timer = Executors.newScheduledThreadPool(1);
        this.queue = new PlaybackList();
        this.observers = new CopyOnWriteArrayList<>();
        this.repeatMode = RepeatMode.OFF;
    }

    // -- observer methods --

    /**
     * Appends a validation listener to the playback events notification registry pipeline.
     *
     * @param observer The target playback observer implementation.
     */
    public void addObserver(PlaybackObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    /**
     * Detaches a previously registered observer so it stops receiving playback
     * notifications. Safe to call with an observer that was never registered.
     *
     * @param observer the observer to remove.
     */
    public void removeObserver(PlaybackObserver observer) {
        observers.remove(observer);
    }

    /**
     * Dispatches timed tick milestones to all registered observer components.
     *
     * @param seconds The current simulated progression counter position.
     */
    private void notifyTimeTick(int seconds) {
        for (PlaybackObserver obs : observers) obs.onTimeTick(seconds);
    }

    /**
     * Broadcasts target active track selection changes to all registered observer components.
     *
     * @param track The current track object reference, or null.
     */
    private void notifyTrackChanged(Track track) {
        for (PlaybackObserver obs : observers) obs.onTrackChanged(track);
    }

    /**
     * Emits state transition notifications to all registered observer components.
     *
     * @param newState The newly applied execution mode.
     */
    private void notifyStateChanged(PlaybackState newState) {
        for (PlaybackObserver obs : observers) obs.onStateChanged(newState);
    }

    // -- Playback methods --

    /**
     * Begins or overrides execution context tracking to load and play a specified target track.
     *
     * @param track The targeted track object wrapper to launch.
     */
    public void play(Track track) {
        if (track == null) {
            return;
        }
        this.stopTimer();
        this.currentTrack = track;
        this.currentState = PlaybackState.PLAYING;
        this.simulatedTimeSeconds.set(0);

        notifyStateChanged(this.currentState);
        notifyTrackChanged(track);

        this.startTimer();
    }

    /**
     * Suspends the playback stream loop, preserving current execution index points.
     */
    public void pause() {
        if (this.currentState == PlaybackState.PLAYING) {
            this.currentState = PlaybackState.PAUSED;
            notifyStateChanged(this.currentState);
            this.stopTimer();
        }
    }

    /**
     * Restores thread loop sequence processing on a previously paused session tracking block.
     */
    public void resume() {
        if (this.currentState == PlaybackState.PAUSED) {
            this.currentState = PlaybackState.PLAYING;
            notifyStateChanged(this.currentState);
            this.startTimer();
        }
    }

    /**
     * Aborts playback streams entirely, resetting system progress values back to base defaults.
     */
    public void stop() {
        this.currentState = PlaybackState.STOPPED;
        notifyStateChanged(this.currentState);
        this.simulatedTimeSeconds.set(0);
        notifyTimeTick(0);
        this.stopTimer();
        this.currentTrack = null;
        notifyTrackChanged(null);
    }

    /**
     * Advances playback to the next track in the current queue.
     * Stops playback entirely if there are no remaining tracks.
     */
    public void playNext() {
        Track next = repeatMode.getStrategy()
                .nextOnAdvance(this.queue, this.currentTrack, this.trackSkipStrategy);

        if (next != null) {
            this.play(next);
        } else {
            this.stop();
        }
    }

    /**
     * Reverts playback to the previous track in the queue.
     * Restarts the current track if it is the first one, or plays the last track
     * if the engine is currently stopped but the queue is populated.
     */
    public void playPrevious() {
        if (this.currentState == PlaybackState.STOPPED) {
            // No current track: jump to the last track in the queue
            List<Track> tracks = this.queue.getActiveList();
            if (tracks != null && !tracks.isEmpty()) {
                int lastIndex = tracks.size() - 1;
                this.queue.setCurrentIndex(lastIndex);
                this.play(tracks.get(lastIndex));
            }
            return;
        }
        Track previousTrack = trackSkipStrategy.skipBackward(this.queue);
        if (previousTrack != null) {
            this.play(previousTrack);
        }
    }

    /**
     * Skips forward to the first track of the next playlist block in the queue (US-029).
     *
     * <p>This is an explicit user navigation: it bypasses the repeat modes and jumps
     * straight to the next block. If shuffle is active or there is no following block,
     * the command is ignored and observers are notified via
     * {@link PlaybackObserver#onPlaylistSkipBlocked()}.</p>
     */
    public void skipToNextPlaylist() {
        if (queue.isShuffleActive()) {
            notifyPlaylistSkipBlocked();
            return;
        }
        Track target = playlistSkipStrategy.skipForward(queue);
        if (target != null) {
            this.play(target);
        } else {
            notifyPlaylistSkipBlocked();
        }
    }

    /**
     * Skips backward to the first track of the previous playlist block in the queue (US-029).
     *
     * <p>Same contract as {@link #skipToNextPlaylist()} in the opposite direction.</p>
     */
    public void skipToPreviousPlaylist() {
        if (queue.isShuffleActive()) {
            notifyPlaylistSkipBlocked();
            return;
        }
        Track target = playlistSkipStrategy.skipBackward(queue);
        if (target != null) {
            this.play(target);
        } else {
            notifyPlaylistSkipBlocked();
        }
    }

    /**
     * Plays a specific track directly from the existing queue context.
     * The cursor is moved to that track so that next/previous navigation stays correct.
     *
     * @param track The target track to play.
     */
    public void playFromQueue(Track track) {
        this.queue.jumpTo(track);
        this.play(track);
    }

    /**
     * Overwrites the current queue with a new list of tracks and immediately begins
     * playing the first track in the provided list.
     *
     * @param tracks The new data source to load.
     */
    public void loadSource(List<Track> tracks) {
        loadSource(tracks, null);
    }

    /**
     * Overwrites the current queue with a new list of tracks coming from the given
     * playlist and immediately begins playing the first track. Tagging the resulting
     * block with its source playlist lets later playlist reorders propagate to it.
     *
     * @param tracks The new data source to load.
     * @param source The playlist the tracks come from, or null for an ad-hoc load.
     */
    public void loadSource(List<Track> tracks, Playlist source) {
        this.queue.loadTracks(tracks, source);
        if (!tracks.isEmpty()) {
            Track first = tracks.get(0);
            this.queue.jumpTo(first);
            this.play(first);
        }
    }

    /**
     * Overwrites the current queue with a new list of tracks and begins playback
     * starting from the specified track. The cursor is positioned on {@code startFrom}
     * so that next/previous navigation continues correctly from that point.
     *
     * @param tracks    The new data source to load.
     * @param startFrom The specific track to begin playing initially.
     */
    public void loadSourceFrom(List<Track> tracks, Track startFrom) {
        loadSourceFrom(tracks, startFrom, null);
    }

    /**
     * Overwrites the current queue with a new list of tracks coming from the given
     * playlist and begins playback from {@code startFrom}. Tagging the resulting block
     * with its source playlist lets later playlist reorders propagate to it.
     *
     * @param tracks    The new data source to load.
     * @param startFrom The specific track to begin playing initially.
     * @param source    The playlist the tracks come from, or null for an ad-hoc load.
     */
    public void loadSourceFrom(List<Track> tracks, Track startFrom, Playlist source) {
        this.queue.loadTracks(tracks, source);
        this.queue.jumpTo(startFrom);
        this.play(startFrom);
    }

    /**
     * Appends a new list of tracks to the end of the existing active queue.
     *
     * @param tracks The sequence of tracks to add.
     */
    public void appendSource(List<Track> tracks) {
        appendSource(tracks, null);
    }

    /**
     * Appends a new list of tracks coming from the given playlist to the end of the
     * existing active queue. Tagging the resulting block with its source playlist lets
     * later playlist reorders propagate to it.
     *
     * @param tracks The sequence of tracks to add.
     * @param source The playlist the tracks come from, or null for an ad-hoc append.
     */
    public void appendSource(List<Track> tracks, Playlist source) {
        boolean wasEmpty = queue.getTrackCount() == 0;
        queue.appendTracks(tracks, source);
        if (wasEmpty && !tracks.isEmpty()) {
            queue.setCurrentIndex(0);
            play(tracks.get(0));
        } else {
            // Appending to a non-empty queue: the current track is unchanged, so notify
            // observers explicitly so the UI (skip-playlist buttons, up-next panel) refreshes.
            notifyQueueChanged();
        }
    }

    // -- timer methods --

    /**
     * Allocates a recurring background thread schedule routine mapping out incremental progress ticks.
     * Triggers safety termination automatically when progress matches the maximum track limits.
     */
    public void startTimer() {
        if (this.getCurrentState() == PlaybackState.STOPPED) {
            this.simulatedTimeSeconds.set(0);
            notifyTimeTick(0);
        }

        this.timerHandle = this.timer.scheduleAtFixedRate(() -> {
            // Snapshot the current track: stop()/removeTrack() may null it out
            // from another thread between ticks.
            Track track = this.currentTrack;
            if (track == null) {
                return;
            }

            int currentTime = this.simulatedTimeSeconds.incrementAndGet();
            notifyTimeTick(currentTime);

            if (currentTime >= track.getDuration()) {
                // repeat-one is handled inside playNext() via the active RepeatStrategy
                this.playNext();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Intercepts and stops ongoing background timer loop processes safely.
     */
    public void stopTimer() {
        if (timerHandle != null && !timerHandle.isCancelled()) {
            timerHandle.cancel(false);
        }
    }

    /**
     * Completely shuts down the internal executor pool service structures.
     * Cleans up background threads safely upon application close boundaries.
     */
    public void shutdownTimer() {
        this.stop();

        if (timer != null && !timer.isShutdown()) {
            timer.shutdownNow();
        }
    }

    // --- getter & setter ---

    /**
     * Returns the active playback list acting as the service queue.
     *
     * @return The PlaybackList object governing sequential progression.
     */
    public PlaybackList getQueue() {
        return this.queue;
    }

    /**
     * Gets the track that is currently loaded into the playback engine.
     *
     * @return The active Track wrapper, or null if inactive.
     */
    public Track getCurrentTrack() {
        return currentTrack;
    }

    /**
     * Explicitly sets the current track context reference pointer.
     *
     * @param currentTrack The target track reference.
     */
    public void setCurrentTrack(Track currentTrack) {
        this.currentTrack = currentTrack;
    }

    /**
     * Gets the active operational execution state flag.
     *
     * @return The active PlaybackState status indicator.
     */
    public PlaybackState getCurrentState() {
        return currentState;
    }

    /**
     * Explicitly overrides the system status playback operational flag.
     *
     * @param currentState The target updated PlaybackState status.
     */
    public void setCurrentState(PlaybackState currentState) {
        this.currentState = currentState;
    }

    /**
     * Retrieves the structural counter tracking current tracking elapsed seconds.
     *
     * @return The atomic integer counter instance mapping active elapsed time.
     */
    public AtomicInteger getSimulatedTimeSeconds() {
        return simulatedTimeSeconds;
    }

    /**
     * Injects an atomic progress tracking container counter wrapper reference.
     *
     * @param simulatedTimeSeconds The target progress index container reference.
     */
    public void setSimulatedTimeSeconds(AtomicInteger simulatedTimeSeconds) {
        this.simulatedTimeSeconds = simulatedTimeSeconds;
    }

    /**
     * Returns the service manager instance scheduling background sequence tasks.
     *
     * @return The active ScheduledExecutorService engine reference handle.
     */
    public ScheduledExecutorService getTimer() {
        return timer;
    }

    /**
     * Injects a specialized custom thread task executor scheduler onto the engine pipeline.
     *
     * @param timer The executor service infrastructure tool.
     */
    public void setTimer(ScheduledExecutorService timer) {
        this.timer = timer;
    }

    /**
     * Returns the future task handle context tracking ongoing active loop sequences.
     *
     * @return The active ScheduledFuture tracking parameter, or null.
     */
    public ScheduledFuture<?> getTimerHandle() {
        return timerHandle;
    }

    /**
     * Links a targeted feature handling token pointer context onto execution trackers.
     *
     * @param timerHandle The targeted scheduling track context loop descriptor.
     */
    public void setTimerHandle(ScheduledFuture<?> timerHandle) {
        this.timerHandle = timerHandle;
    }

    /**
     * Retrieves the current repeat mode configuration.
     *
     * @return The active RepeatMode state.
     */
    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    /**
     * Sets the repeat mode for the playback queue.
     *
     * @param repeatMode The new RepeatMode to be applied.
     */
    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }

    /**
     * Observes external deletion events to maintain engine safety and queue integrity.
     * If the deleted track is currently playing, execution halts safely.
     *
     * @param track The tracked entity actively removed from source contexts.
     */
    @Override
    public void onTrackDeleted(Track track) {
        this.removeTrackFromQueue(track);
    }

    /**
     * Appends or randomly inserts a track into the live queue depending on shuffle state.
     * Does not start playback; the track simply becomes reachable via next/prev.
     *
     * @param track The track to add.
     */
    public void addTrackToQueue(Track track) {
        boolean wasEmpty = queue.getTrackCount() == 0;
        queue.addToCanonicalList(track);
        if (queue.isShuffleActive()) {
            queue.insertTrackAtRandom(track);
        }
        if (wasEmpty) {
            queue.setCurrentIndex(0);
            play(track);
        } else {
            notifyQueueChanged();
        }
    }


    /**
     * Removes a track from the live queue (every occurrence of it).
     * If the removed track was the one playing, playback advances to whatever
     * the cursor now points at, or stops if the queue ran off the end.
     *
     * @param track The track to remove.
     */
    public void removeTrackFromQueue(Track track) {
        if (track == null) {
            return;
        }

        boolean wasCurrent = track.equals(this.currentTrack);

        // The queue owns the cursor: it removes all occurrences and repositions it.
        queue.removeTrack(track);

        if (wasCurrent) {
            Track nowPlaying = queue.getCurrentTrack();
            if (nowPlaying != null) {
                play(nowPlaying);
            } else {
                stop(); // stop() already notifies observers with a null track
            }
        } else {
            notifyQueueChanged();
        }
    }

    /**
     * Moves a track within the live queue from one position to another (US-027).
     *
     * <p>The currently playing track and the timer are left untouched, so playback
     * continues without interruption; the queue owns the cursor and repositions it
     * to keep pointing at the same logical track. Observers are notified so the
     * "up next" panel and row styling refresh.</p>
     *
     * @param from the current index of the track in the queue.
     * @param to   the target index.
     */
    public void moveTrackInQueue(int from, int to) {
        queue.moveTrack(from, to);
        notifyQueueChanged();
    }

    /**
     * Propagates a playlist reorder (US-027) to every queue block that was added from
     * the given playlist, moving the track at block-relative index {@code from} to
     * {@code to} in each. The currently playing track and the timer are untouched, so
     * playback continues without interruption; observers are notified so the "up next"
     * panel and row styling refresh.
     *
     * @param source the playlist whose queued blocks must be reordered.
     * @param from   the block-relative index of the moved track.
     * @param to     the block-relative target index.
     */
    public void reorderInPlaylistBlocks(Playlist source, int from, int to) {
        queue.reorderWithinPlaylistBlocks(source, from, to);
        notifyQueueChanged();
    }

    /**
     * Broadcasts a notification to all registered observers indicating that
     * the playback queue's structural sequence or content has changed.
     */
    private void notifyQueueChanged() {
        for (PlaybackObserver obs : observers) obs.onQueueChanged();
    }

    /**
     * Broadcasts a notification to all registered observers indicating that a
     * playlist-block skip command could not be honoured (queue limit reached or
     * shuffle active), so the UI can ignore/disable the corresponding control.
     */
    private void notifyPlaylistSkipBlocked() {
        for (PlaybackObserver obs : observers) obs.onPlaylistSkipBlocked();
    }

    /**
     * Triggered when a track's metadata is modified.
     * Currently, a no-op as the playback service relies on object references
     * and does not directly manage metadata views.
     *
     * @param track The track that was edited.
     */
    @Override
    public void onTrackEdit(Track track) {
    }

    /**
     * Jumps to a specified time during the playback.
     *
     * @param targetSeconds The exact second to jump.
     */
    public void seekTo(int targetSeconds) {
        if (this.currentTrack == null) {
            return;
        }

        // Security Check: The user must not jump over the track boundaries
        if (targetSeconds < 0) {
            targetSeconds = 0;
        } else if (targetSeconds > currentTrack.getDuration()) {
            targetSeconds = currentTrack.getDuration();
        }

        this.simulatedTimeSeconds.set(targetSeconds);

        notifyTimeTick(targetSeconds);
    }

    /**
     * Captures the current state of the playback queue for later restoration.
     *
     * @return a snapshot of the queue.
     */
    public QueueMemento captureQueueState() {
        return queue.snapshot();
    }

    /**
     * Restores the playback queue to a previously captured state and notifies
     * observers so the queue views refresh. The track currently playing is left
     * untouched (only the queue structure and cursor are restored).
     *
     * @param memento the queue state to restore; ignored if null.
     */
    public void restoreQueueState(QueueMemento memento) {
        if (memento == null) return;
        queue.restore(memento);
        notifyQueueChanged();
    }
}
