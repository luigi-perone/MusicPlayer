package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * {@link RepeatStrategy} for {@link RepeatMode#REPEAT_PLAYLIST}: when the queue is
 * exhausted it wraps back to the first track of the active list.
 */
public class RepeatAllStrategy implements RepeatStrategy {

    /**
     * {@inheritDoc}
     *
     * <p>Steps the cursor forward; if that runs off the end, resets the cursor to the
     * first track of the active list. Returns {@code null} only when the queue is empty.</p>
     */
    @Override
    public Track nextOnAdvance(PlaybackList queue, Track currentTrack, SkipStrategy skipStrategy) {
        Track next = skipStrategy.skipForward(queue);
        if (next != null) {
            return next;
        }
        Track first = queue.getFirstTrack();
        if (first != null) {
            queue.setCurrentIndex(0);
        }
        return first;
    }
}
