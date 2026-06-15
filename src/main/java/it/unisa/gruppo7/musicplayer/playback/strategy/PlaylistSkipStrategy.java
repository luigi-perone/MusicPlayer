package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * {@link SkipStrategy} that navigates the queue one playlist block at a time,
 * jumping to the first track of the next/previous block (US-029).
 */
public class PlaylistSkipStrategy implements SkipStrategy {

    /**
     * Skips forward to the first track of the next playlist block.
     *
     * @param queue the playback queue to navigate.
     * @return the first track of the next block, or null if there is no following block.
     */
    @Override
    public Track skipForward(PlaybackList queue) {
        return queue.getNextPlaylistTrack();
    }

    /**
     * Skips backward to the first track of the previous playlist block.
     *
     * @param queue the playback queue to navigate.
     * @return the first track of the previous block, or null if the cursor is in the first block.
     */
    @Override
    public Track skipBackward(PlaybackList queue) {
        return queue.getPreviousPlaylistTrack();
    }
}
