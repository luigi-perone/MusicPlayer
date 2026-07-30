package it.unisa.gruppo7.musicplayer.playback.strategy;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TrackSkipStrategyTest {

    private PlaybackList queue;
    private final TrackSkipStrategy strategy = new TrackSkipStrategy();

    private Track trk1;
    private Track trk2;
    private Track trk3;

    @BeforeEach
    void setUp() {
        trk1 = new Track("Song 1", "Artist", 120);
        trk2 = new Track("Song 2", "Artist", 120);
        trk3 = new Track("Song 3", "Artist", 120);
    }

    @Nested
    class SkipForward {

        @Test
        void fromFirstTrackReturnsSecond() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(0);

            assertSame(trk2, strategy.skipForward(queue));
            assertEquals(1, queue.getCurrentIndex());
        }

        @Test
        void fromMiddleReturnsNext() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(1);

            assertSame(trk3, strategy.skipForward(queue));
            assertEquals(2, queue.getCurrentIndex());
        }

        @Test
        void fromLastReturnsNullAndDoesNotMoveCursor() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(2);

            assertNull(strategy.skipForward(queue));
            assertEquals(2, queue.getCurrentIndex());
        }

        @Test
        void onEmptyQueueReturnsNull() {
            queue = new PlaybackList();

            assertNull(strategy.skipForward(queue));
            assertEquals(-1, queue.getCurrentIndex());
        }

        @Test
        void onSingleTrackReturnsNull() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1));
            queue.setCurrentIndex(0);

            assertNull(strategy.skipForward(queue));
            assertEquals(0, queue.getCurrentIndex());
        }
    }

    @Nested
    class SkipBackward {

        @Test
        void fromLastTrackReturnsMiddle() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(2);

            assertSame(trk2, strategy.skipBackward(queue));
            assertEquals(1, queue.getCurrentIndex());
        }

        @Test
        void fromMiddleReturnsFirst() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(1);

            assertSame(trk1, strategy.skipBackward(queue));
            assertEquals(0, queue.getCurrentIndex());
        }

        @Test
        void fromFirstReturnsNullAndDoesNotMoveCursor() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1, trk2, trk3));
            queue.setCurrentIndex(0);

            assertNull(strategy.skipBackward(queue));
            assertEquals(0, queue.getCurrentIndex());
        }

        @Test
        void onEmptyQueueReturnsNull() {
            queue = new PlaybackList();

            assertNull(strategy.skipBackward(queue));
            assertEquals(-1, queue.getCurrentIndex());
        }

        @Test
        void onSingleTrackReturnsNull() {
            queue = new PlaybackList();
            queue.loadTracks(Arrays.asList(trk1));
            queue.setCurrentIndex(0);

            assertNull(strategy.skipBackward(queue));
            assertEquals(0, queue.getCurrentIndex());
        }
    }
}
