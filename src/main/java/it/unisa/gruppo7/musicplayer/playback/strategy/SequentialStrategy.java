package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * {@link RepeatStrategy} for {@link RepeatMode#OFF}: plain sequential playback that
 * stops once the queue is exhausted.
 */
public class SequentialStrategy implements RepeatStrategy {

    /**
     * {@inheritDoc}
     *
     * <p>Steps the cursor forward; a {@code null} result means the cursor was on the
     * last track, so the caller stops playback.</p>
     */
    @Override
    public Track nextOnAdvance(PlaybackList queue, Track currentTrack, SkipStrategy skipStrategy) {
        return skipStrategy.skipForward(queue);
    }
}
