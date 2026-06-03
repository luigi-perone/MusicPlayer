package it.unisa.gruppo7.musicplayer.core;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * @author francescoLemmo
 */
public interface TrackObserver {
    /**
     * Method used when the track state changes
     */
    void onTrackDeleted(Track track);
}