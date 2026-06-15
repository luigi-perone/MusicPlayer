package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Interface defining an observer for tracking application playback events.
 * Provides update hooks for synchronization changes across timing counters, audio track mutations,
 * and operational engine states.
 * * @author Francesco Lemmo
 */
public interface PlaybackObserver {

    /**
     * Invoked periodically on every elapsed simulated second.
     *
     * @param simulatedSeconds The cumulative seconds elapsed since the track started.
     */
    void onTimeTick(int simulatedSeconds);

    /**
     * Invoked when the system switches execution to a different audio track target.
     *
     * @param currentTrack The newly selected track, or null if playback is stopped.
     */
    void onTrackChanged(Track currentTrack);

    /**
     * Invoked when the core engine operational mode state changes.
     *
     * @param newState The updated playback state.
     */
    void onStateChanged(PlaybackState newState);

    /**
     * Invoked when the contents of the playback queue change
     * (track added or removed) without necessarily changing the current track.
     * Default no-op so existing implementors do not need to change.
     */
    default void onQueueChanged() {}

    /**
     * Invoked when a playlist-block skip command (US-029) cannot be honoured,
     * e.g. the user tried to skip past the last playlist in the queue, or shuffle
     * mode is active. Lets the UI disable/signal the skip button.
     * Default no-op so existing implementors do not need to change.
     */
    default void onPlaylistSkipBlocked() {}
}