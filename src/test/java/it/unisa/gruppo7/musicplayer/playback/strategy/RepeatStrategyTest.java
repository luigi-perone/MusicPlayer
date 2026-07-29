package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the repeat-mode {@link RepeatStrategy} implementations, exercised
 * against a real {@link PlaybackList} and a real {@link TrackSkipStrategy} (no mocks),
 * consistent with the rest of the playback test suite.
 */
class RepeatStrategyTest {

    private PlaybackList queue;
    private Track trk1;
    private Track trk2;
    private Track trk3;
    private final SkipStrategy skip = new TrackSkipStrategy();

    /** Builds a three-track queue with the cursor at the first track before each test. */
    @BeforeEach
    void setUp() {
        trk1 = new Track("Song 1", "Artist", 120);
        trk2 = new Track("Song 2", "Artist", 120);
        trk3 = new Track("Song 3", "Artist", 120);

        queue = new PlaybackList();
        List<Track> tracks = Arrays.asList(trk1, trk2, trk3);
        queue.loadTracks(tracks); // cursor starts at index 0
    }

    /** Tests the sequential (no-repeat) advance behaviour. */
    @Nested
    class NoRepeat {

        private final RepeatStrategy strategy = new SequentialStrategy();

        /** Verifies that advancing from the middle returns the next track. */
        @Test
        void advanceFromMiddleReturnsNextTrack() {
            queue.setCurrentIndex(0);
            assertSame(trk2, strategy.nextOnAdvance(queue, trk1));
            assertEquals(1, queue.getCurrentIndex());
        }

        /** Verifies that advancing past the last track returns null. */
        @Test
        void advanceFromLastReturnsNull() {
            queue.setCurrentIndex(2);
            assertNull(strategy.nextOnAdvance(queue, trk3),
                    "Off the end with no repeat must return null so the caller stops");
        }
    }

    /** Tests the repeat-all advance behaviour. */
    @Nested
    class RepeatAll {

        private final RepeatStrategy strategy = new RepeatAllStrategy();

        /** Verifies that advancing from the middle returns the next track. */
        @Test
        void advanceFromMiddleReturnsNextTrack() {
            queue.setCurrentIndex(1);
            assertSame(trk3, strategy.nextOnAdvance(queue, trk2));
            assertEquals(2, queue.getCurrentIndex());
        }

        /** Verifies that advancing past the last track wraps to the first one. */
        @Test
        void advanceFromLastWrapsToFirstTrack() {
            queue.setCurrentIndex(2);
            assertSame(trk1, strategy.nextOnAdvance(queue, trk3),
                    "Reaching the end must wrap to the first track");
            assertEquals(0, queue.getCurrentIndex(),
                    "Wrapping must reset the cursor to the first track");
        }
    }

    /** Tests the repeat-one advance behaviour. */
    @Nested
    class RepeatOne {

        private final RepeatStrategy strategy = new RepeatOneStrategy();

        /** Verifies that advancing returns the same track without moving the cursor. */
        @Test
        void advanceReturnsSameTrackWithoutMovingCursor() {
            queue.setCurrentIndex(1);
            int before = queue.getCurrentIndex();
            assertSame(trk2, strategy.nextOnAdvance(queue, trk2));
            assertEquals(before, queue.getCurrentIndex(),
                    "Repeat-one must not move the queue cursor");
        }
    }

    /** Tests that each {@link RepeatMode} is mapped to its expected strategy. */
    @Nested
    class EnumMapping {

        /** Verifies that each repeat mode carries the matching strategy implementation. */
        @Test
        void eachModeCarriesItsStrategy() {
            assertTrue(RepeatMode.OFF.getStrategy() instanceof SequentialStrategy);
            assertTrue(RepeatMode.REPEAT_PLAYLIST.getStrategy() instanceof RepeatAllStrategy);
            assertTrue(RepeatMode.REPEAT_ONE.getStrategy() instanceof RepeatOneStrategy);
        }
    }
}
