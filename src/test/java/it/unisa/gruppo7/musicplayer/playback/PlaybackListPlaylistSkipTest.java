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

    /** Builds the three-block queue [A B | C D | E] with the cursor on A. */
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

    /** Tests skipping forward across playlist blocks. */
    @Nested
    class SkippingForward {

        /** Verifies that skipping forward jumps to the first track of each following block. */
        @Test
        void jumpsToFirstTrackOfEachFollowingBlock() {
            assertTrue(queue.hasNextPlaylist());
            assertSame(c, queue.getNextPlaylistTrack(), "next playlist start must be C");

            assertTrue(queue.hasNextPlaylist());
            assertSame(e, queue.getNextPlaylistTrack(), "next playlist start must be E");
        }

        /** Verifies that skipping forward past the last block is blocked. */
        @Test
        void onLastBlock_isBlocked() {
            queue.getNextPlaylistTrack(); // -> C
            queue.getNextPlaylistTrack(); // -> E (last block)

            assertFalse(queue.hasNextPlaylist(), "no block follows the last playlist");
            assertNull(queue.getNextPlaylistTrack(), "skipping past the last playlist is blocked");
        }
    }

    /** Tests skipping backward across playlist blocks. */
    @Nested
    class SkippingBackward {

        /** Verifies that skipping backward always jumps to the start of the previous block. */
        @Test
        void alwaysJumpsToStartOfPreviousBlock() {
            queue.getNextPlaylistTrack(); // -> C
            queue.getNextPlaylistTrack(); // -> E (block 2)

            assertSame(c, queue.getPreviousPlaylistTrack(), "previous block start must be C");
            assertSame(a, queue.getPreviousPlaylistTrack(), "previous block start must be A");
        }

        /** Verifies that skipping backward from mid-block goes to the start of the preceding block. */
        @Test
        void fromMiddleOfBlock_goesToStartOfPrecedingBlock() {
            assertTrue(queue.jumpTo(d), "cursor should be on D (middle of block 1)");
            assertSame(a, queue.getPreviousPlaylistTrack(),
                    "from mid-block-1 the previous block start is A");
        }

        /** Verifies that skipping backward before the first block is blocked. */
        @Test
        void onFirstBlock_isBlocked() {
            assertFalse(queue.hasPreviousPlaylist(), "no block precedes the first playlist");
            assertNull(queue.getPreviousPlaylistTrack(), "skipping before the first playlist is blocked");
        }
    }

    /** Tests that block tracking stays consistent after queue mutations. */
    @Nested
    class BlockTrackingStaysConsistent {

        /** Verifies that block navigation still works after removing a track. */
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
