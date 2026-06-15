package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * {@link SkipStrategy} that navigates the queue one track at a time,
 * mirroring the classic next/previous behaviour of {@link PlaybackList}.
 */
public class TrackSkipStrategy implements SkipStrategy {

    /**
     * Skips forward by a single track.
     *
     * @param queue the playback queue to navigate.
     * @return the next track, or null if the cursor is already on the last track.
     */
    @Override
    public Track skipForward(PlaybackList queue) {
        return queue.getNextTrack();
    }

    /**
     * Skips backward by a single track.
     *
     * @param queue the playback queue to navigate.
     * @return the previous track, or null if the cursor is already on the first track.
     */
    @Override
    public Track skipBackward(PlaybackList queue) {
        return queue.getPreviousTrack();
    }
}
