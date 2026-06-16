package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;

import java.time.Year;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Structural Facade that centralizes and coordinates core music player sub-systems
 * including audio playback, library index curation, and custom playlist profiles.
 * Implements the Singleton pattern to guarantee a single unified controller context.
 *
 * @author Francesco Lemmo
 */
public class MusicPlayerFacade implements PlaybackObserver {

    /** Single instance of the MusicPlayerFacade. */
    private static MusicPlayerFacade instance;

    /** The core library database managing all tracks. */
    private final Library library;

    /** The service responsible for managing user playlists. */
    private final PlaylistService playlistService;

    /** The service handling audio streaming and playback state. */
    private final PlaybackService playbackService;

    /** The track currently selected in the UI context. */
    private Track selectedTrack;

    /** The playlist currently providing the playback context. */
    private Playlist activePlaylist;

    /** List of registered observers listening for track data changes. */
    private final List<TrackObserver> observers = new ArrayList<>();

    /** * Executor dedicated to Input/Output file operations.
     * Uses a single thread in background.
     */
    private final ExecutorService ioExecutor;

    /**
     * Private constructor initializing subsystems and registering internal dependencies.
     */
    private MusicPlayerFacade() {
        this.library = Library.getInstance();
        this.playlistService = new PlaylistService();
        this.playbackService = new PlaybackService();

        this.ioExecutor = Executors.newSingleThreadExecutor();

        this.playbackService.addObserver(this);
        this.addObserver(this.playlistService);
        this.addObserver(this.playbackService);

    }

    /**
     * Retrieves the global thread-safe singleton state interface context.
     *
     * @return The active MusicPlayerFacade context runtime.
     */
    public static MusicPlayerFacade getInstance() {
        if (instance == null) {
            instance = new MusicPlayerFacade();
        }
        return instance;
    }

    public void saveLibrary() {
        if (ioExecutor == null || ioExecutor.isShutdown()) {
            return;
        }
        try {
            ioExecutor.submit(() -> {
                library.save();
            });
        } catch (RejectedExecutionException ignored) {
            // Executor is shutting down (e.g. application/test teardown): skip the save.
        }
    }

    /**
     * Erases all managed music tracks currently stored inside the structural library database.
     */
    public void clearLibrary() {
        library.clearLibrary();
    }

    /**
     * Selects the correct Track constructor mapping variant based on structural null field parameters.
     *
     * @param title           The track name.
     * @param author          The creator name.
     * @param duration        The length in seconds.
     * @param genre           The category filter identifier.
     * @param publicationYear The domain year calendar instance.
     * @return A validated track metadata object wrapper.
     */
    private Track createTrack(String title, String author, int duration, String genre, Year publicationYear) {
        boolean isGenreEmpty = (genre == null || genre.trim().isEmpty());
        boolean isYearEmpty = (publicationYear == null);

        if (isGenreEmpty && isYearEmpty) {
            return new Track(title, author, duration);
        } else if (isGenreEmpty) {
            return new Track(title, author, duration, publicationYear);
        } else if (isYearEmpty) {
            return new Track(title, author, duration, genre);
        } else {
            return new Track(title, author, duration, genre, publicationYear);
        }
    }

    /**
     * Sets the shared reference pointer targeting a specific track active in selection layouts.
     *
     * @param track The current track selection view model node.
     */
    public void setSelectedTrack(Track track) {
        this.selectedTrack = track;
    }

    /**
     * Gets the current selection track pointer active across presentation controllers.
     *
     * @return The selected track entity reference, or null.
     */
    public Track getSelectedTrack() {
        return this.selectedTrack;
    }

    /**
     * Retrieves the playlist currently providing the playback context.
     *
     * @return The active playlist, or null if playback was started from the general library.
     */
    public Playlist getActivePlaylist() {
        return this.activePlaylist;
    }

    // --- Library Methods ---

    /**
     * Instantiates a track entry data footprint and appends it to the user database library.
     *
     * @param title           The track title name.
     * @param author          The creator name string.
     * @param duration        The audio layout length in seconds.
     * @param genre           The music genre style categorization.
     * @param publicationYear The structured release calendar Year metadata object.
     * @return true if added successfully, false otherwise.
     * @throws IllegalArgumentException If argument business fields fail structural boundary constraints.
     */
    public boolean addNewTrackToLibrary(String title, String author, int duration, String genre, Year publicationYear) {
        try {
            Track newTrack = this.createTrack(title, author, duration, genre, publicationYear);

            boolean success = library.addTrack(newTrack);
            if (success) {
                saveLibrary();
            }
            return success;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Permanently drops a track configuration out of the library indexing systems
     * and broadcasts a notification to observers.
     *
     * @param track The targeted track object wrapper mapping instance to drop.
     * @return true if structural record removal succeeded.
     */
    public boolean removeTrackFromLibrary(Track track) {
        boolean success = library.removeTrack(track);
        if (success) {
            saveLibrary();
            this.notifyTrackDeleted(track);
        }
        return success;
    }

    /**
     * Overwrites mutable attribute data inside a target track registry tracking record index.
     *
     * @param track              The target database object map structure reference being modified.
     * @param newTitle           The adjusted track title.
     * @param newAuthor          The adjusted track artist creator.
     * @param newDuration        The modified duration metric length in seconds.
     * @param newGenre           The updated genre identity.
     * @param newPublicationYear The updated release year entity calendar timestamp.
     * @return true if the modifications were cleanly written to persistence storage.
     */
    public boolean modifyTrack(Track track, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear) {
        boolean success = library.modifyTrackInLibrary(track, newTitle, newAuthor, newDuration, newGenre, newPublicationYear);
        if (success) {
            saveLibrary();
            notifyTrackEdit(track);
        }
        return success;
    }
    
    /**
     * Updates the predefined visual tags assigned to a track and persists the library.
     *
     * @param track The track to update.
     * @param tags  The selected predefined tags.
     */
    public void updateTrackTags(Track track, Set<TrackTag> tags) {
        if (track == null) {
            return;
        }
        track.setTags(tags);
        library.save();
        notifyTrackEdit(track);
    }

    /**
     * Searches structural database files to fetch a single track entity via precise UUID.
     *
     * @param id The global unique identifier signature lookup tag.
     * @return The matching Track wrapper database profile, or null.
     */
    public Track getTrackFromLibrary(UUID id) {
        return library.getTrackById(id);
    }

    /**
     * Collects all tracks currently cataloged in the core music manager index structure.
     *
     * @return A collection view containing the complete track library index dataset.
     */
    public Collection<Track> getTracksFromLibrary() {
        return library.getTracks();
    }

    /**
     * Serializes the structural library tracks dataset out to an explicit printable layout string.
     *
     * @return A formatted text breakdown reporting current storage parameters.
     */
    public String printLibrary() {
        return library.toString();
    }

    /**
     * Instantiates a new playlist grouping binder.
     *
     * @param name The identification name label tag string.
     * @return The freshly allocated structural Playlist mapping file wrapper.
     */
    public Playlist createPlaylist(String name) {
        return playlistService.createPlaylist(name);
    }

    /**
     * Removes an entire playlist record permanently from the application profile records.
     *
     * @param playlist The playlist profile record map to truncate.
     * @return An Optional detailing failure log messages, or empty if dropped cleanly.
     */
    public Optional<String> deletePlaylist(Playlist playlist) {
        return playlistService.deletePlaylist(playlist);
    }

    /**
     * Structural renaming mutator utility for renaming custom user playlists.
     *
     * @param playlist The target playlist data mapping context file.
     * @param newName  The target updated name string to apply.
     * @return An Optional detailing failure logs, or empty if renamed cleanly.
     */
    public Optional<String> renamePlaylist(Playlist playlist, String newName) {
        return playlistService.renamePlaylist(playlist, newName);
    }

    /**
     * Gathers all custom track compilation playlists stored inside the persistence profile registry.
     *
     * @return A collection list containing all active playlist configurations.
     */
    public List<Playlist> getPlaylists() {
        return playlistService.getPlaylists();
    }

    /**
     * Resolves a custom playlist reference tracking record look-up using an exact string match query.
     *
     * @param name The identity name query parameter.
     * @return The located playlist instance, or null if unmapped.
     */
    public Playlist getPlaylist(String name) {
        return playlistService.getPlaylist(name);
    }

    /**
     * Gets the encapsulated structural application business logic service for playlist persistence fields.
     *
     * @return The integrated backend playlist service entity handle.
     */
    public PlaylistService getPlaylistService(){
        return playlistService;
    }

    /**
     * Serializes current user playlist tracking parameters to disk storage.
     */
    public void savePlaylists() {
        if (ioExecutor == null || ioExecutor.isShutdown()) {
            return;
        }
        try {
            ioExecutor.submit(() -> {
                playlistService.save();
            });
        } catch (RejectedExecutionException ignored) {
            // Executor is shutting down (e.g. application/test teardown): skip the save.
        }
    }

    /**
     * Generates and saves automatically a playlist (uses Strategy Pattern)
     */
    public Playlist createAutoPlaylist(String playlistName, PlaylistGenerationStrategy strategy) {
        // Apply filter to the library
        List<Track> selectedTracks = strategy.generate(this.getTracksFromLibrary());

        // If no tracks remain after the filtering, throw an exception
        if (selectedTracks.isEmpty()) {
            throw new IllegalArgumentException("Nessuna traccia trovata");
        }

        // otherwise, create the playlist with the selected tracks
        Playlist newPlaylist = playlistService.createPlaylist(playlistName);
        playlistService.addTracksToPlaylist(newPlaylist, selectedTracks);

        // Saves the new playlist
        savePlaylists();

        return newPlaylist;
    }


    /**
     * Formats a raw numerical integer seconds index into a standard user-readable "MM:SS" time layout.
     *
     * @param totalSeconds Total aggregated track duration length in seconds.
     * @return A padded string structured format presentation.
     */
    public String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Loads a specific target track model pointer directly into processing hardware stream buffers.
     *
     * @param track The music file container to parse and play.
     */
    public void playTrack(Track track) {
        playbackService.play(track);
    }

    /**
     * Halts active layout audio stream output processing and preserves structural index markers.
     */
    public void pauseTrack() {
        playbackService.pause();
    }

    /**
     * Resumes playback operations processing from preserved layout marker points.
     */
    public void resumeTrack() {
        playbackService.resume();
    }

    /**
     * Initiates playback using the entire global library as the source,
     * clearing any active playlist context.
     */
    public void playFromLibrary() {
        this.activePlaylist = null;
        List<Track> tracks = new ArrayList<>(library.getTracks());
        playbackService.loadSource(tracks);
    }

    /**
     * Initiates playback using a specific playlist as the source,
     * setting it as the active playlist context.
     *
     * @param playlist The playlist to play.
     * @throws IllegalArgumentException If the playlist is empty.
     */
    public void playFromPlaylist(Playlist playlist) {
        if (playlist.getTracks().isEmpty()) {
            throw new IllegalArgumentException("La playlist \"" + playlist.getName() + "\" non contiene brani.");
        }
        this.activePlaylist = playlist;
        playlist.incrementPlayCount();
        savePlaylists();
        List<Track> tracks = new ArrayList<>(playlist.getTracks());
        playbackService.loadSource(tracks, playlist);
    }

    /**
     * Appends all tracks from the specified playlist to the end of the current playback queue.
     *
     * @param playlist The playlist containing tracks to append.
     */
    public void appendPlaylistToQueue(Playlist playlist) {
        List<Track> tracks = new ArrayList<>(playlist.getTracks());
        playbackService.appendSource(tracks, playlist);
    }

    /**
     * Appends a single track to the end of the current playback queue.
     *
     * @param track The track to append.
     */
    public void appendTrackToQueue(Track track) {
        playbackService.addTrackToQueue(track);
    }

    /**
     * Initiates playback from the global library starting at a specific track,
     * clearing any active playlist context.
     *
     * @param track The track to start playback from.
     */
    public void playFromLibraryFrom(Track track) {
        this.activePlaylist = null;
        List<Track> tracks = new ArrayList<>(library.getTracks());
        playbackService.loadSourceFrom(tracks, track);
    }

    /**
     * Initiates playback from a specific playlist starting at a given track,
     * setting it as the active playlist context.
     *
     * @param playlist The playlist providing the context.
     * @param track    The track to start playback from.
     */
    public void playFromPlaylistFrom(Playlist playlist, Track track) {
        if (this.activePlaylist == null || !this.activePlaylist.equals(playlist)) {
            playlist.incrementPlayCount();
            savePlaylists();
        }
        this.activePlaylist = playlist;
        List<Track> tracks = new ArrayList<>(playlist.getTracks());
        playbackService.loadSourceFrom(tracks, track, playlist);
    }

    /**
     * Jumps to a specific track currently loaded within the playback queue.
     *
     * @param track The target track to play.
     */
    public void playFromQueue(Track track) {
        playbackService.playFromQueue(track);
    }

    public List<Track> getUpNextQueueFrom() {
        return playbackService.getQueue().getUpNextQueue();
    }

    /**
     * Skips to the first track of the next playlist block in the queue (US-029).
     * Ignored (and observers notified) when already on the last playlist.
     */
    public void skipToNextPlaylist() {
        playbackService.skipToNextPlaylist();
    }

    /**
     * Skips to the first track of the previous playlist block in the queue (US-029).
     * Ignored (and observers notified) when already on the first playlist.
     */
    public void skipToPreviousPlaylist() {
        playbackService.skipToPreviousPlaylist();
    }

    /**
     * @return true if there is a following playlist block to skip to.
     */
    public boolean hasNextPlaylist() {
        return playbackService.getQueue().hasNextPlaylist();
    }

    /**
     * @return true if there is a preceding playlist block to skip to.
     */
    public boolean hasPreviousPlaylist() {
        return playbackService.getQueue().hasPreviousPlaylist();
    }

    // --- Playback mode methods ---

    /**
     * Toggles the shuffle state for the playback queue and reorganizes it relative to the given track.
     *
     * @param shuffleState true to enable shuffle, false to disable.
     * @param track        The currently active track to base the shuffle operations around.
     */
    public void shuffleQueue(boolean shuffleState, Track track) {
        playbackService.getQueue().setShuffle(shuffleState, track);
    }

    /**
     * Checks whether the playback queue is currently in shuffle mode.
     *
     * @return true if shuffle is active, false otherwise.
     */
    public boolean isShuffleActive() {
        return playbackService.getQueue().isShuffleActive();
    }

    /**
     * Retrieves the current repeat mode of the playback engine.
     *
     * @return The active RepeatMode state.
     */
    public RepeatMode getCurrentRepeatMode() {
        return playbackService.getRepeatMode();
    }

    /**
     * Cycles the playback repeat mode through its available states: OFF, REPEAT_PLAYLIST, and REPEAT_ONE.
     */
    public void changeRepeatMode() {
        RepeatMode currentRepeatMode = playbackService.getRepeatMode();
        if (currentRepeatMode == RepeatMode.OFF) {
            playbackService.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);
        } else if (currentRepeatMode == RepeatMode.REPEAT_PLAYLIST) {
            playbackService.setRepeatMode(RepeatMode.REPEAT_ONE);
        } else if (currentRepeatMode == RepeatMode.REPEAT_ONE) {
            playbackService.setRepeatMode(RepeatMode.OFF);
        }
    }

    /**
     * Returns the structural subsystem instance managing low-level audio tracking streams.
     *
     * @return The background media playback tracking context handle.
     */
    public PlaybackService getPlaybackService() {
        return playbackService;
    }

    /**
     * Fetches the ongoing state indicator flags mapping out device operational conditions.
     *
     * @return The structural PlaybackState enum tracking device actions.
     */
    public PlaybackState getPlaybackState() {
        return playbackService.getCurrentState();
    }

    /**
     * Retrieves the track currently being processed by the playback engine.
     *
     * @return The active playing track, or null if no track is currently loaded or playing.
     */
    public Track getCurrentPlayingTrack() {
        return playbackService.getCurrentTrack();
    }

    /**
     * Allows skipping to a specific point in the currently playing track,
     * delegating the operation to the PlaybackService.
     *
     * @param seconds The target time in seconds to skip to
     */
    public void seekTo(int seconds) {
        if (playbackService != null) {
            playbackService.seekTo(seconds);
        }
    }

    /**
     * Safely triggers hardware timer sequence destruction tasks on application exit boundaries.
     */
    public void shutdownPlayback() {
        if (playbackService != null) {
            playbackService.shutdownTimer();
        }

        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            try {
                // Drain pending saves so no background write touches the data files
                // after this point (prevents I/O races on shutdown/teardown).
                ioExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Synchronizes the live playback queue after a track has been added to a playlist.
     * If the playlist is currently the active playback source, the track is appended to
     * the queue (or inserted at a random position when shuffle is active).
     *
     * @param playlist The playlist that received the new track.
     * @param track    The track that was added.
     */
    public void onTrackAddedToPlaylist(Playlist playlist, Track track) {
        if (playlist != null && playlist.equals(this.activePlaylist)) {
            playbackService.addTrackToQueue(track);
        }
    }

    /**
     * Synchronizes the live playback queue after a track has been removed from a playlist.
     * If the playlist is currently the active playback source, the track is removed from
     * the queue. If it was playing, playback advances to the next track automatically.
     *
     * @param playlist The playlist from which the track was removed.
     * @param track    The track that was removed.
     */
    public void onTrackRemovedFromPlaylist(Playlist playlist, Track track) {
        if (playlist != null && playlist.equals(this.activePlaylist)) {
            playbackService.removeTrackFromQueue(track);
        }
    }

    /**
     * Synchronises the live playback queue after a track has been reordered within
     * a playlist (US-027). The move is propagated to <b>every</b> block in the queue
     * that was added from this playlist — the active playback source as well as any
     * copies appended to the queue — so all queued instances stay aligned with the
     * playlist. The currently playing track keeps playing without interruption.
     *
     * <p>When shuffle is active the queue order is randomised and the acceptance
     * criteria concern sequential playback only, so the queue is left untouched
     * (the new order is still persisted on disk).</p>
     *
     * @param playlist The playlist whose tracks were reordered.
     * @param from     The previous index of the moved track.
     * @param to       The new index of the moved track.
     */
    public void onTrackReorderedInPlaylist(Playlist playlist, int from, int to) {
        if (playlist != null && !isShuffleActive()) {
            playbackService.reorderInPlaylistBlocks(playlist, from, to);
        }
    }

    /**
     * Hooks up an update subscriber interface onto tracking collection registries.
     *
     * @param observer The target dynamic subscriber tracking module implementation.
     */
    public void addObserver(TrackObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Breaks off an update subscriber hook from observation pipelines.
     *
     * @param observer The tracking receiver interface target to drop.
     */
    public void removeObserver(TrackObserver observer) {
        observers.remove(observer);
    }

    /**
     * Iterates through active pipeline subscribers to execute deletion notification parameters.
     *
     * @param track The dropped entity metadata profile.
     */
    private void notifyTrackDeleted(Track track) {
        for (TrackObserver observer : observers) {
            observer.onTrackDeleted(track);
        }
    }

    /**
     * Iterates through active pipeline subscribers to execute modification notification parameters.
     *
     * @param track The edited entity metadata profile.
     */
    private void notifyTrackEdit(Track track) {
        for (TrackObserver observer : observers) {
            observer.onTrackEdit(track);
        }
    }

    // HomePage methods

    /**
     * Returns the list with the most played tracks.
     * @param limit maximum number of tracks to return (es. 5 o 10)
     */
    public List<Track> getMostPlayedTracks(int limit) {
        return library.getTracks().stream()
                .filter(track -> track.getPlayCount() > 0) // Ignores the tracks never played
                .sorted(Comparator.comparingInt(Track::getPlayCount).reversed()) // Decreasing order
                .limit(limit) // Take the first N (limit) tracks
                .collect(Collectors.toList());
    }

    /**
     * Returns the list with the most played playlists.
     * @param limit maximum number of playlists to return (es. 5 o 10)
     */
    public List<Playlist> getMostPlayedPlaylists(int limit) {
        return playlistService.getPlaylists().stream()
                .filter(playlist -> playlist.getPlayCount() > 0)
                .sorted(Comparator.comparingInt(Playlist::getPlayCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    // -- Playback Observer methods --

    @Override
    public void onTrackChanged(Track currentTrack) {
        if (currentTrack != null){
            currentTrack.incrementPlayCount();
        }
        saveLibrary();
    }

}