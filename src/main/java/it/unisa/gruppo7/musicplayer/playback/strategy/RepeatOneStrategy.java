package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * {@link RepeatStrategy} for {@link RepeatMode#REPEAT_ONE}: always replays the current
 * track without moving the cursor.
 */
public class RepeatOneStrategy implements RepeatStrategy {

    /**
     * {@inheritDoc}
     *
     * <p>Returns the current track unchanged, leaving the cursor untouched.</p>
     */
    @Override
    public Track nextOnAdvance(PlaybackList queue, Track currentTrack) {
        return currentTrack;
    }
}
