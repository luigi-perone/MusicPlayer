package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.ArrayList;
import java.util.List;
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


    /**
     * Constructs a new PlaybackService and allocates resource executors
     * required for multi-threaded time tracking simulation.
     */
    public PlaybackService() {
        this.currentState = PlaybackState.START_UP;
        this.simulatedTimeSeconds = new AtomicInteger(0);
        this.timer = Executors.newScheduledThreadPool(1);
        this.queue = new PlaybackList();
        this.observers = new ArrayList<>();
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
        if (track == null) return;
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
    }

    /**
     * Advances playback to the next track in the current queue.
     * Stops playback entirely if there are no remaining tracks.
     */
    public void playNext() {
        if (repeatMode == RepeatMode.REPEAT_ONE) {
            this.play(this.currentTrack);
            return;
        }

        Track nextTrack = this.queue.getNextTrack();

        if (nextTrack != null) {
            this.play(nextTrack);
        } else if (repeatMode == RepeatMode.REPEAT_PLAYLIST) {
            Track first = this.queue.getFirstTrack();
            if (first != null) {
                this.queue.setCurrentIndex(0);
                this.play(first);
            }
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
            List<Track> tracks = this.queue.getActiveList(); // make getActiveList() package-private or add a helper
            if (tracks != null && !tracks.isEmpty()) {
                int lastIndex = tracks.size() - 1;
                this.queue.setCurrentIndex(lastIndex);
                this.play(tracks.get(lastIndex));
            }
            return;
        }
        Track previousTrack = this.queue.getPreviousTrack();
        if (previousTrack != null) {
            this.play(previousTrack);
        }
    }

    /**
     * Plays a specific track directly from the existing queue context.
     *
     * @param track The target track to play.
     */
    public void playFromQueue(Track track) {
        this.queue.setCurrentIndex(0);
        this.play(track);
    }

    /**
     * Overwrites the current queue with a new list of tracks and immediately begins
     * playing the first track in the provided list.
     *
     * @param tracks The new data source to load.
     */
    public void loadSource(List<Track> tracks) {
        this.queue.loadTracks(tracks);
        if (!tracks.isEmpty()) {
            this.queue.setCurrentIndex(0);
            this.play(tracks.get(0));
        }
    }

    /**
     * Overwrites the current queue with a new list of tracks and begins playback
     * starting from the specified track.
     *
     * @param tracks    The new data source to load.
     * @param startFrom The specific track to begin playing initially.
     */
    public void loadSourceFrom(List<Track> tracks, Track startFrom) {
        this.queue.loadTracks(tracks);
        this.queue.setCurrentIndex(0);
        this.play(startFrom);
    }

    /**
     * Appends a new list of tracks to the end of the existing active queue.
     *
     * @param tracks The sequence of tracks to add.
     */
    public void appendSource(List<Track> tracks) {
        boolean wasEmpty = queue.getTrackCount() == 0;
        queue.appendTracks(tracks);
        if (wasEmpty && !tracks.isEmpty()) {
            queue.setCurrentIndex(0);
            play(tracks.get(0));
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
            int currentTime = this.simulatedTimeSeconds.incrementAndGet();
            notifyTimeTick(currentTime);

            if (currentTime >= currentTrack.getDuration()) {
                if (repeatMode == RepeatMode.REPEAT_ONE) {
                    this.play(currentTrack);
                } else {
                    this.playNext();
                }
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
        if(this.currentTrack != null && this.currentTrack.equals(track)){
            this.playNext();
        }

        this.queue.removeTrack(track);
    }

    // In PlaybackService.java

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
     * Removes a track from the live queue.
     * If the removed track is currently playing, advances to the next track automatically.
     *
     * @param track The track to remove.
     */
    public void removeTrackFromQueue(Track track) {
        boolean isCurrentTrack = track.equals(this.currentTrack);
        if (isCurrentTrack) {
            Track next = queue.getNextTrack();
            queue.removeTrack(track);
            if (next != null) {
                play(next);
            } else {
                stop();
                notifyTrackChanged(null);
            }
        } else {
            queue.removeTrack(track);
            notifyQueueChanged();
        }
    }

    /**
     * Broadcasts a notification to all registered observers indicating that
     * the playback queue's structural sequence or content has changed.
     */
    private void notifyQueueChanged() {
        for (PlaybackObserver obs : observers) obs.onQueueChanged();
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
}