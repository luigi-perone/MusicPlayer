package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistSkipStrategyTest {

    private PlaybackList queue;
    private final PlaylistSkipStrategy strategy = new PlaylistSkipStrategy();

    private Track a, b, c, d, e;

    @BeforeEach
    void setUp() {
        queue = new PlaybackList();
        a = new Track("A", "Artist", 100, "Rock", Year.of(2000));
        b = new Track("B", "Artist", 100, "Rock", Year.of(2000));
        c = new Track("C", "Artist", 100, "Rock", Year.of(2000));
        d = new Track("D", "Artist", 100, "Rock", Year.of(2000));
        e = new Track("E", "Artist", 100, "Rock", Year.of(2000));

        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b)));
        queue.appendTracks(new ArrayList<>(Arrays.asList(c, d)));
        queue.appendTracks(new ArrayList<>(Arrays.asList(e)));
    }

    @Nested
    class SkipForward {

        @Test
        void fromFirstBlockJumpsToSecondBlock() {
            assertSame(c, strategy.skipForward(queue));
        }

        @Test
        void fromSecondBlockJumpsToThirdBlock() {
            strategy.skipForward(queue);
            assertSame(e, strategy.skipForward(queue));
        }

        @Test
        void fromLastBlockReturnsNull() {
            strategy.skipForward(queue);
            strategy.skipForward(queue);
            assertNull(strategy.skipForward(queue));
        }
    }

    @Nested
    class SkipBackward {

        @Test
        void fromLastBlockJumpsToSecondBlock() {
            strategy.skipForward(queue);
            strategy.skipForward(queue);
            assertSame(c, strategy.skipBackward(queue));
        }

        @Test
        void fromSecondBlockJumpsToFirstBlock() {
            strategy.skipForward(queue);
            assertSame(a, strategy.skipBackward(queue));
        }

        @Test
        void fromFirstBlockReturnsNull() {
            assertNull(strategy.skipBackward(queue));
        }

        @Test
        void fromMiddleOfBlockGoesToStartOfPrecedingBlock() {
            assertTrue(queue.jumpTo(d));
            assertSame(a, strategy.skipBackward(queue));
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void onEmptyQueueSkipForwardReturnsNull() {
            queue = new PlaybackList();
            assertNull(strategy.skipForward(queue));
        }

        @Test
        void onEmptyQueueSkipBackwardReturnsNull() {
            queue = new PlaybackList();
            assertNull(strategy.skipBackward(queue));
        }

        @Test
        void onSingleBlockSkipForwardReturnsNull() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(a, b, c));
            assertNull(strategy.skipForward(queue));
        }

        @Test
        void onSingleBlockSkipBackwardReturnsNull() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(a, b, c));
            assertNull(strategy.skipBackward(queue));
        }
    }
}
