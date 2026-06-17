package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatAllStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatOneStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.SequentialStrategy;

/**
 * Available repeat modes, each backed by the {@link RepeatStrategy} that
 * implements how playback advances at the end of a track.
 *
 * @author francescoLemmo
 */
public enum RepeatMode {
    /** Repeat disabled: playback advances sequentially and stops at the end. */
    OFF             (new SequentialStrategy()),
    /** Repeat the whole playlist: advancing past the last track wraps to the first. */
    REPEAT_PLAYLIST (new RepeatAllStrategy()),
    /** Repeat the current track on every advance. */
    REPEAT_ONE      (new RepeatOneStrategy());

    private final RepeatStrategy strategy;

    /**
     * Associates the repeat mode with the strategy implementing its behaviour.
     *
     * @param strategy the advance strategy for this mode
     */
    RepeatMode(RepeatStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @return the {@link RepeatStrategy} that implements this mode's advance behaviour.
     */
    public RepeatStrategy getStrategy() {
        return strategy;
    }

    /**
     * Returns the next repeat mode in the cycle
     * OFF &rarr; REPEAT_PLAYLIST &rarr; REPEAT_ONE &rarr; OFF.
     *
     * @return the next repeat mode
     */
    public RepeatMode next() {
        RepeatMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
