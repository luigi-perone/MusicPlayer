package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Strategy defining the granularity of a skip operation over the playback queue
 * (skip per single track vs. skip per playlist block).
 *
 * <p>Implementations move the queue cursor and return the track that should now
 * be played, or {@code null} when the skip is blocked (e.g. there is no further
 * playlist block in that direction).</p>
 */
public interface SkipStrategy {

    /**
     * Skips forward, moving the cursor to the next target.
     *
     * @param queue the playback queue to navigate.
     * @return the track to play, or null if the skip is blocked.
     */
    Track skipForward(PlaybackList queue);

    /**
     * Skips backward, moving the cursor to the previous target.
     *
     * @param queue the playback queue to navigate.
     * @return the track to play, or null if the skip is blocked.
     */
    Track skipBackward(PlaybackList queue);
}
