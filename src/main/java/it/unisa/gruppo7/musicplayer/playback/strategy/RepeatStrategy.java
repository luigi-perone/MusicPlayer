package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Strategy describing the repeat mode, i.e. the END-OF-QUEUE / advance behaviour
 * of the playback engine: what happens when the queue advances or a track ends
 * (stop, wrap back to the start, or replay the same track).
 *
 * <p>Advancing is always TRACK-granular: it is triggered by a track reaching its end,
 * so implementations step the cursor one track at a time. Block-level navigation is a
 * separate, explicit user action handled by {@link SkipStrategy}, which deliberately
 * bypasses the repeat modes.</p>
 */
public interface RepeatStrategy {

    /**
     * Computes the track to play when the queue advances or a track ends.
     *
     * @param queue        the playback queue (owns the cursor).
     * @param currentTrack the track that was playing, or null.
     * @return the track to play next, or null to stop playback.
     */
    Track nextOnAdvance(PlaybackList queue, Track currentTrack);
}
