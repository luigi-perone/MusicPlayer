package it.unisa.gruppo7.musicplayer.playback;

/**
 * Enumeration detailing the specific valid operational run states
 * of the application playback subsystem engine.
 * * @author Francesco Lemmo
 */
public enum PlaybackState {
    /**
     * Indicates that the system has now been started up.
     */
    START_UP,

    /**
     * Indicates that a track is actively playing and the time counter is advancing.
     */
    PLAYING,

    /**
     * Indicates that playback is suspended and the time counter is frozen at its current position.
     */
    PAUSED,

    /**
     * Indicates that playback is inactive and the progress metrics are reset to zero.
     */
    STOPPED
}