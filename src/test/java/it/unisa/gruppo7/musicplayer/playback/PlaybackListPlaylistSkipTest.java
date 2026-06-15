package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US-029: playlist-block navigation in {@link PlaybackList}.
 *
 * <p>Queue layout built in {@link #setUp()}:</p>
 * <pre>
 *   index : 0   1   2   3   4
 *   track : A   B   C   D   E
 *   block : 0   0   1   1   2
 * </pre>
 */
class PlaybackListPlaylistSkipTest {

    private PlaybackList queue;
    private Track a, b, c, d, e;

    @BeforeEach
    void setUp() {
        queue = new PlaybackList();
        a = new Track("A", "Artist", 100, "Rock", Year.of(2000));
        b = new Track("B", "Artist", 100, "Rock", Year.of(2000));
        c = new Track("C", "Artist", 100, "Rock", Year.of(2000));
        d = new Track("D", "Artist", 100, "Rock", Year.of(2000));
        e = new Track("E", "Artist", 100, "Rock", Year.of(2000));

        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b)));   // block 0
        queue.appendTracks(new ArrayList<>(Arrays.asList(c, d))); // block 1
        queue.appendTracks(new ArrayList<>(Arrays.asList(e)));    // block 2
        // loadTracks left the cursor on index 0 (track A, block 0).
    }

    @Nested
    class SkippingForward {

        @Test
        void jumpsToFirstTrackOfEachFollowingBlock() {
            assertTrue(queue.hasNextPlaylist());
            assertSame(c, queue.getNextPlaylistTrack(), "next playlist start must be C");

            assertTrue(queue.hasNextPlaylist());
            assertSame(e, queue.getNextPlaylistTrack(), "next playlist start must be E");
        }

        @Test
        void onLastBlock_isBlocked() {
            queue.getNextPlaylistTrack(); // -> C
            queue.getNextPlaylistTrack(); // -> E (last block)

            assertFalse(queue.hasNextPlaylist(), "no block follows the last playlist");
            assertNull(queue.getNextPlaylistTrack(), "skipping past the last playlist is blocked");
        }
    }

    @Nested
    class SkippingBackward {

        @Test
        void alwaysJumpsToStartOfPreviousBlock() {
            queue.getNextPlaylistTrack(); // -> C
            queue.getNextPlaylistTrack(); // -> E (block 2)

            assertSame(c, queue.getPreviousPlaylistTrack(), "previous block start must be C");
            assertSame(a, queue.getPreviousPlaylistTrack(), "previous block start must be A");
        }

        @Test
        void fromMiddleOfBlock_goesToStartOfPrecedingBlock() {
            assertTrue(queue.jumpTo(d), "cursor should be on D (middle of block 1)");
            assertSame(a, queue.getPreviousPlaylistTrack(),
                    "from mid-block-1 the previous block start is A");
        }

        @Test
        void onFirstBlock_isBlocked() {
            assertFalse(queue.hasPreviousPlaylist(), "no block precedes the first playlist");
            assertNull(queue.getPreviousPlaylistTrack(), "skipping before the first playlist is blocked");
        }
    }

    @Nested
    class BlockTrackingStaysConsistent {

        @Test
        void afterRemovingATrack_navigationStillJumpsBetweenBlocks() {
            assertTrue(queue.removeTrack(b)); // remove B from block 0 -> [A | C D | E]
            // cursor still on A (index 0, block 0)
            assertSame(c, queue.getNextPlaylistTrack(), "still jumps to start of block 1");
            assertSame(e, queue.getNextPlaylistTrack(), "still jumps to start of block 2");
            assertNull(queue.getNextPlaylistTrack());
        }
    }
}
