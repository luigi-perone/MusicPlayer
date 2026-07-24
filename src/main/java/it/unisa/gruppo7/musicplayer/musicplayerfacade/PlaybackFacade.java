package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.util.List;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.QueueMemento;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Playback-facing view of the facade (Interface Segregation Principle).
 * <p>
 * Groups the playback- and queue-control commands, so playback consumers can
 * depend on this narrow contract instead of the whole {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface PlaybackFacade {

    /** Starts playing the given track. */
    void playTrack(Track track);

    /** Pauses the current playback. */
    void pauseTrack();

    /** Resumes a paused playback. */
    void resumeTrack();

    /** Starts playing the whole library. */
    void playFromLibrary();

    /** Starts playing the given playlist. */
    void playFromPlaylist(Playlist playlist);

    /** Starts playing the library from the given track onwards. */
    void playFromLibraryFrom(Track track);

    /** Starts playing the given playlist from the given track onwards. */
    void playFromPlaylistFrom(Playlist playlist, Track track);

    /** Jumps to and plays a track already present in the queue. */
    void playFromQueue(Track track);

    /** Appends all tracks of a playlist to the end of the queue. */
    void appendPlaylistToQueue(Playlist playlist);

    /** Appends a single track to the end of the queue. */
    void appendTrackToQueue(Track track);

    /** Returns the tracks queued after the current one. */
    List<Track> getUpNextQueueFrom();

    /** Skips forward to the first track of the next playlist block in the queue. */
    void skipToNextPlaylist();

    /** Skips back to the first track of the previous playlist block in the queue. */
    void skipToPreviousPlaylist();

    /** @return true if a following playlist block exists in the queue. */
    boolean hasNextPlaylist();

    /** @return true if a preceding playlist block exists in the queue. */
    boolean hasPreviousPlaylist();

    /** Toggles shuffle, keeping the given track as the current one. */
    void shuffleQueue(boolean shuffleState, Track track);

    /** @return true if shuffle is currently active. */
    boolean isShuffleActive();

    /** @return the current repeat mode. */
    RepeatMode getCurrentRepeatMode();

    /** Cycles to the next repeat mode. */
    void changeRepeatMode();

    /** @return the current playback state. */
    PlaybackState getPlaybackState();

    /** @return the track currently playing, or null if none. */
    Track getCurrentPlayingTrack();

    /** Seeks to a specific position within the current track. */
    void seekTo(int seconds);

    /** Registers a playback observer without exposing the underlying service. */
    void addPlaybackObserver(PlaybackObserver observer);

    /** Unregisters a previously registered playback observer. */
    void removePlaybackObserver(PlaybackObserver observer);

    /** Advances playback to the next track in the queue. */
    void playNext();

    /** Returns playback to the previous track in the queue. */
    void playPrevious();

    /** Captures the current playback-queue state for later restoration on undo. */
    QueueMemento captureQueueState();

    /** Restores the playback queue to a previously captured state. */
    void restoreQueueState(QueueMemento memento);

    /** Exposes the underlying playback service. */
    PlaybackService getPlaybackService();
}
