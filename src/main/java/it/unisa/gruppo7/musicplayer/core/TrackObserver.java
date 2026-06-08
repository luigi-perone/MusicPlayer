package it.unisa.gruppo7.musicplayer.core;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Observer interface for tracking lifecycle adjustments of track records.
 * Subscribed components implement this contract to receive status updates
 * when track components change or leave the repository tracking index.
 *
 * @author Francesco Lemmo
 */
public interface TrackObserver {
    /**
     * Invoked automatically when a specific track is completely removed or purged from the core library index.
     *
     * @param track The track database model profile that was deleted.
     */
    void onTrackDeleted(Track track);
}