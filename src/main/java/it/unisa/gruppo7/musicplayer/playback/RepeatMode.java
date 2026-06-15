package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatAllStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatOneStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.RepeatStrategy;
import it.unisa.gruppo7.musicplayer.playback.strategy.SequentialStrategy;

/**
 * @author francescoLemmo
 */
public enum RepeatMode {
    OFF             (new SequentialStrategy()),
    REPEAT_PLAYLIST (new RepeatAllStrategy()),
    REPEAT_ONE      (new RepeatOneStrategy());

    private final RepeatStrategy strategy;

    RepeatMode(RepeatStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @return the {@link RepeatStrategy} that implements this mode's advance behaviour.
     */
    public RepeatStrategy getStrategy() {
        return strategy;
    }
}
