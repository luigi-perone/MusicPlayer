package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AddToQueueCommand}, the undoable command the controllers
 * actually invoke when enqueuing a track or a whole playlist. Covers both the append
 * it delegates to and the restoration performed by its undo.
 */
class AddToQueueCommandTest {

    private PlaybackService service;

    private Track trackA;
    private Track trackB;
    private Track trackC;
    private Track trackNew;
    private Track trackExtra;

    /** Builds a service playing A out of an A → B → C queue. */
    @BeforeEach
    void setUp() {
        service = new PlaybackService();

        trackA     = new Track("Song A", "Artist", 120);
        trackB     = new Track("Song B", "Artist", 120);
        trackC     = new Track("Song C", "Artist", 120);
        trackNew   = new Track("New Song", "Artist", 120);
        trackExtra = new Track("Extra Song", "Artist", 120);

        service.loadSourceFrom(Arrays.asList(trackA, trackB, trackC), trackA);
        service.stopTimer();
    }

    /** Releases the background timer of the service under test. */
    @AfterEach
    void tearDown() {
        service.shutdownTimer();
    }

    /** Scenarios covering the append the command delegates to. */
    @Nested
    @DisplayName("Execution of the append")
    class WhenExecuting {

        /** Verifies that the command runs the supplied append action. */
        @Test
        @DisplayName("a single track lands at the last position")
        void singleTrack_landsAtLastPosition() {
            AddToQueueCommand command =
                    new AddToQueueCommand(service, () -> service.addTrackToQueue(trackNew));

            command.execute();

            List<Track> queue = queueTracks();
            assertEquals(4, queue.size(), "the queue must hold four tracks after the append");
            assertSame(trackNew, queue.get(3), "the appended track must occupy the last slot");
        }

        /** Verifies that a whole block is appended in its original order. */
        @Test
        @DisplayName("a playlist block lands at the end in its original order")
        void playlistBlock_landsAtTheEndInOrder() {
            AddToQueueCommand command = new AddToQueueCommand(service,
                    () -> service.appendSource(Arrays.asList(trackNew, trackExtra)));

            command.execute();

            List<Track> queue = queueTracks();
            assertEquals(5, queue.size(), "the queue must hold five tracks after the block append");
            assertSame(trackNew,   queue.get(3), "the first block track must sit at index 3");
            assertSame(trackExtra, queue.get(4), "the second block track must sit at index 4");
        }
    }

    /** Scenarios covering the restoration performed by the undo. */
    @Nested
    @DisplayName("Undo of the append")
    class WhenUndoing {

        /** Verifies that undoing a single-track append restores the previous queue. */
        @Test
        @DisplayName("restores the queue content and order after a single track")
        void undo_singleTrack_restoresTheQueue() {
            AddToQueueCommand command =
                    new AddToQueueCommand(service, () -> service.addTrackToQueue(trackNew));

            command.execute();
            command.undo();

            assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),
                    "the queue must be back to its three original tracks, in order");
        }

        /** Verifies that undoing a block append restores the previous queue. */
        @Test
        @DisplayName("restores the queue content and order after a playlist block")
        void undo_playlistBlock_restoresTheQueue() {
            AddToQueueCommand command = new AddToQueueCommand(service,
                    () -> service.appendSource(Arrays.asList(trackNew, trackExtra)));

            command.execute();
            command.undo();

            assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),
                    "the whole appended block must be removed by the undo");
        }

        /** Verifies that the undo restores the queue structure without stopping playback. */
        @Test
        @DisplayName("leaves the currently playing track untouched")
        void undo_leavesTheCurrentTrackUntouched() {
            AddToQueueCommand command =
                    new AddToQueueCommand(service, () -> service.addTrackToQueue(trackNew));

            command.execute();
            command.undo();

            assertSame(trackA, service.getCurrentTrack(),
                    "undoing an append must not interrupt the track being played");
        }

        /** Verifies that undoing an append made into an empty queue empties it again. */
        @Test
        @DisplayName("empties the queue again when the append started from an empty one")
        void undo_appendIntoEmptyQueue_emptiesItAgain() {
            PlaybackService emptyService = new PlaybackService();
            try {
                AddToQueueCommand command =
                        new AddToQueueCommand(emptyService, () -> emptyService.addTrackToQueue(trackNew));

                command.execute();
                emptyService.stopTimer();
                assertEquals(1, emptyService.getQueue().getTracks().size(),
                        "precondition: the append must have filled the empty queue");

                command.undo();

                assertTrue(emptyService.getQueue().getTracks().isEmpty(),
                        "the queue must be empty again after undoing the only append");
            } finally {
                emptyService.shutdownTimer();
            }
        }

        /**
         * Verifies that the queue is left in a workable state after an undo: a reorder
         * performed afterwards must still operate on a consistent structure.
         */
        @Test
        @DisplayName("leaves the queue usable for a later reorder")
        void undo_leavesTheQueueUsableForALaterReorder() {
            AddToQueueCommand command = new AddToQueueCommand(service,
                    () -> service.appendSource(Arrays.asList(trackNew, trackExtra)));

            command.execute();
            command.undo();

            assertDoesNotThrow(() -> service.moveTrackInQueue(2, 0),
                    "a reorder after the undo must not hit an inconsistent queue");

            List<Track> queue = queueTracks();
            assertEquals(3, queue.size(), "the reorder must not change the number of queued tracks");
            assertTrue(queue.containsAll(Arrays.asList(trackA, trackB, trackC)),
                    "the reorder must keep the three original tracks in the queue");
        }
    }

    /** Returns the current queue tracks as a list for assertions. */
    @SuppressWarnings("unchecked")
    private List<Track> queueTracks() {
        return (List<Track>) service.getQueue().getTracks();
    }
}
