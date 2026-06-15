package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Strategy describing the repeat mode, i.e. the END-OF-QUEUE / advance behaviour
 * of the playback engine.
 *
 * <p>This is a different axis from {@link SkipStrategy}: {@code SkipStrategy} decides
 * the GRANULARITY of a forward/backward step (single track vs. playlist block), while
 * {@code RepeatStrategy} decides WHAT HAPPENS when the queue advances or a track ends
 * (stop, wrap back to the start, or replay the same track).</p>
 *
 * <p>Implementations reuse the supplied {@link SkipStrategy} to move the cursor, so
 * the navigation logic is never duplicated here.</p>
 */
public interface RepeatStrategy {

    /**
     * Computes the track to play when the queue advances or a track ends.
     *
     * @param queue        the playback queue (owns the cursor).
     * @param currentTrack the track that was playing, or null.
     * @param skipStrategy the navigation strategy used to step the cursor forward.
     * @return the track to play next, or null to stop playback.
     */
    Track nextOnAdvance(PlaybackList queue, Track currentTrack, SkipStrategy skipStrategy);
}
