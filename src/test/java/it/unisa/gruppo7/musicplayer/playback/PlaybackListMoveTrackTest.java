package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US-027: reordering a track within {@link PlaybackList#moveTrack(int, int)}.
 *
 * <p>The queue is loaded as a single block [A B C D E]; the cursor is positioned
 * on C (index 2) for the cross-cursor scenarios, so we can assert that the cursor
 * keeps pointing at the same logical track after a move.</p>
 */
class PlaybackListMoveTrackTest {

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

        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c, d, e)));
    }

    @Nested
    class CursorFollowsTheSameTrack {

        @Test
        void movingTheCurrentTrack_cursorFollowsIt() {
            queue.jumpTo(c); // cursor on C (index 2)

            queue.moveTrack(2, 4); // C -> end

            assertEquals(Arrays.asList(a, b, d, e, c), queue.getActiveList());
            assertSame(c, queue.getCurrentTrack(), "the moved current track keeps playing");
            assertEquals(4, queue.getCurrentIndex());
        }

        @Test
        void movingATrackFromBeforeToAfterCursor_keepsCurrentTrack() {
            queue.jumpTo(c); // cursor on C (index 2)

            queue.moveTrack(0, 3); // A (before) -> after C

            assertEquals(Arrays.asList(b, c, d, a, e), queue.getActiveList());
            assertSame(c, queue.getCurrentTrack(), "cursor still points at C");
        }

        @Test
        void movingATrackFromAfterToBeforeCursor_keepsCurrentTrack() {
            queue.jumpTo(c); // cursor on C (index 2)

            queue.moveTrack(4, 0); // E (after) -> before C

            assertEquals(Arrays.asList(e, a, b, c, d), queue.getActiveList());
            assertSame(c, queue.getCurrentTrack(), "cursor still points at C");
        }

        @Test
        void movingTwoTracksThatDoNotCrossTheCursor_leavesCursorUnchanged() {
            queue.jumpTo(c); // cursor on C (index 2)

            queue.moveTrack(0, 1); // both before the cursor

            assertEquals(Arrays.asList(b, a, c, d, e), queue.getActiveList());
            assertSame(c, queue.getCurrentTrack());
            assertEquals(2, queue.getCurrentIndex());
        }
    }

    @Nested
    class BlockIdsStayAligned {

        @Test
        void afterMoving_playlistBlockNavigationStillWorks() {
            // Rebuild as two blocks: [A B | C D E]
            queue.clear();
            queue.loadTracks(new ArrayList<>(Arrays.asList(a, b)));   // block 0
            queue.appendTracks(new ArrayList<>(Arrays.asList(c, d, e))); // block 1

            // Reorder within block 1: move E to the front of its block.
            queue.moveTrack(4, 2); // [A B | E C D]
            assertEquals(Arrays.asList(a, b, e, c, d), queue.getActiveList());

            // Cursor still on A (block 0); next block must start at E.
            assertTrue(queue.hasNextPlaylist());
            assertSame(e, queue.getNextPlaylistTrack(), "block boundary preserved after reorder");
        }
    }

    @Nested
    class InvalidMovesAreNoOps {

        @Test
        void sameIndexDoesNothing() {
            List<Track> before = new ArrayList<>(queue.getActiveList());
            queue.moveTrack(1, 1);
            assertEquals(before, queue.getActiveList());
        }

        @Test
        void outOfRangeIndicesDoNothing() {
            List<Track> before = new ArrayList<>(queue.getActiveList());
            queue.moveTrack(-1, 2);
            queue.moveTrack(2, 99);
            assertEquals(before, queue.getActiveList());
        }
    }
}
