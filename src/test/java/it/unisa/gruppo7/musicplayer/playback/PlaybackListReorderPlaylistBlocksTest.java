package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlaybackList#reorderWithinPlaylistBlocks(Playlist, int, int)}.
 *
 * <p>A playlist can sit in the queue more than once (its active block plus copies
 * appended via "add to queue"). A reorder performed on the playlist must propagate
 * to every one of those blocks, while leaving blocks from other playlists untouched.</p>
 */
class PlaybackListReorderPlaylistBlocksTest {

    private PlaybackList queue;
    private Playlist p1, p2;
    private Track a, b, c, d, e;

    @BeforeEach
    void setUp() {
        queue = new PlaybackList();
        p1 = new Playlist("P1", new ArrayList<>());
        p2 = new Playlist("P2", new ArrayList<>());

        a = new Track("A", "Artist", 100, "Rock", Year.of(2000));
        b = new Track("B", "Artist", 100, "Rock", Year.of(2000));
        c = new Track("C", "Artist", 100, "Rock", Year.of(2000));
        d = new Track("D", "Artist", 100, "Rock", Year.of(2000));
        e = new Track("E", "Artist", 100, "Rock", Year.of(2000));
    }

    @Test
    void reorderPropagatesToEveryCopyOfThePlaylistInTheQueue() {
        // [A B C | A B C | D E]  ->  two P1 blocks plus one P2 block
        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1);   // block 0 (P1)
        queue.appendTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1); // block 1 (P1)
        queue.appendTracks(new ArrayList<>(Arrays.asList(d, e)), p2);    // block 2 (P2)

        // Move A (index 0) to index 2 within P1: each P1 block becomes [B C A].
        queue.reorderWithinPlaylistBlocks(p1, 0, 2);

        assertEquals(Arrays.asList(b, c, a, b, c, a, d, e), queue.getActiveList(),
                "the reorder must apply to every P1 block, leaving the P2 block untouched");
    }

    @Test
    void reorderLeavesOtherPlaylistsUntouched() {
        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1);
        queue.appendTracks(new ArrayList<>(Arrays.asList(d, e)), p2);

        // Reordering P2 must not touch the P1 block.
        queue.reorderWithinPlaylistBlocks(p2, 0, 1); // [D E] -> [E D]

        assertEquals(Arrays.asList(a, b, c, e, d), queue.getActiveList());
    }

    @Test
    void cursorFollowsThePlayingTrackInsideAReorderedBlock() {
        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1); // block 0
        queue.appendTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1); // block 1
        queue.jumpTo(c); // cursor on the first C (index 2)

        queue.reorderWithinPlaylistBlocks(p1, 2, 0); // move C to the front of each block

        assertEquals(Arrays.asList(c, a, b, c, a, b), queue.getActiveList());
        assertSame(c, queue.getCurrentTrack(), "the playing track keeps playing");
        assertEquals(0, queue.getCurrentIndex());
    }

    @Test
    void blocksThatDoNotCoverBothIndicesAreSkipped() {
        // Two P1 blocks of different sizes: [A B C | A B]
        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1);
        queue.appendTracks(new ArrayList<>(Arrays.asList(a, b)), p1);

        // index 2 is out of range for the 2-track block, which must be left alone.
        queue.reorderWithinPlaylistBlocks(p1, 0, 2);

        assertEquals(Arrays.asList(b, c, a, a, b), queue.getActiveList());
    }

    @Test
    void nullSourceOrNoOpMoveDoesNothing() {
        queue.loadTracks(new ArrayList<>(Arrays.asList(a, b, c)), p1);

        queue.reorderWithinPlaylistBlocks(null, 0, 2);
        queue.reorderWithinPlaylistBlocks(p1, 1, 1);

        assertEquals(Arrays.asList(a, b, c), queue.getActiveList());
    }
}
