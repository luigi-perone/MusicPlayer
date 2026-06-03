package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * @author francescoLemmo
 */

public interface PlaybackObserver {
    // Invoked on every tick
    void onTimeTick(int simulatedSeconds);

    // Invoked on track change
    void onTrackChanged(Track currentTrack);

    // Invoked on state change
    void onStateChanged(PlaybackState newState);
}