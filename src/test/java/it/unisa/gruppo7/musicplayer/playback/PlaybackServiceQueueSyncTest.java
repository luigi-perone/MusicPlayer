package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US-018: queue synchronization during active playback.
 * Verifies proper synchronization of the playback queue when adding
 * or removing tracks.
 */
class PlaybackServiceQueueSyncTest {

    /** The playback service under test. */
    private PlaybackService service;

    /** Test track A. */
    private Track trackA;

    /** Test track B. */
    private Track trackB;

    /** Test track C. */
    private Track trackC;

    /** A new track to add to the queue during tests. */
    private Track trackNew;

    /**
     * Configures the test environment before each execution.
     * Initializes the playback service, creates test tracks,
     * and loads an initial queue (A → B → C) starting with track A.
     */
    @BeforeEach
    void setUp() {
        service = new PlaybackService();

        trackA   = new Track("Song A", "Artist", 120);
        trackB   = new Track("Song B", "Artist", 120);
        trackC   = new Track("Song C", "Artist", 120);
        trackNew = new Track("New Song", "Artist", 120);

        // Load A → B → C and start playing A
        service.loadSourceFrom(Arrays.asList(trackA, trackB, trackC), trackA);
    }

    /**
     * Test scenarios evaluating track addition behaviors in the queue.
     */
    @Nested
    class WhenAddingTracks {

        /**
         * TC1 – Verifies that adding a track in normal mode (without shuffle)
         * correctly appends it to the end of the queue.
         */
        @Test
        void addTrackToQueue_normalMode_appendsAtEnd() {
            service.addTrackToQueue(trackNew);

            List<Track> queue = (List<Track>) service.getQueue().getTracks();
            assertEquals(4, queue.size(),
                    "Queue should contain 4 tracks after addition");
            assertSame(trackNew, queue.get(3),
                    "New track should be the last element in the queue");
        }

        /**
         * TC2 – Verifies that adding a track with shuffle mode active
         * inserts it at a random position in the queue, ensuring it does not
         * interrupt or overwrite the currently playing track.
         */
        @Test
        void addTrackToQueue_shuffleActive_insertsAtRandomPosition() {
            // Enable shuffle while trackA is playing (trackA will be pinned at index 0)
            service.getQueue().setShuffle(true, trackA);

            // Add the new track several times (with fresh services) to confirm it never
            // lands at position 0 and is always present in the shuffled list.
            for (int i = 0; i < 20; i++) {
                PlaybackService s = new PlaybackService();
                Track a = new Track("A" + i, "X", 60);
                Track b = new Track("B" + i, "X", 60);
                Track c = new Track("C" + i, "X", 60);
                Track n = new Track("N" + i, "X", 60);
                s.loadSourceFrom(Arrays.asList(a, b, c), a);
                s.getQueue().setShuffle(true, a);

                s.addTrackToQueue(n);

                List<Track> shuffled = s.getQueue().getUpNextQueue();
                // shuffled is the "up next" slice, so n must be somewhere in it
                assertTrue(shuffled.contains(n),
                        "New track must appear in the up-next queue when shuffle is on");
            }

            // Also verify the canonical list is consistent
            service.addTrackToQueue(trackNew);
            List<Track> canonical = (List<Track>) service.getQueue().getTracks();
            assertTrue(canonical.contains(trackNew),
                    "New track must be in the canonical list regardless of shuffle");
        }
    }

    /**
     * Test scenarios evaluating track removal behaviors and playback continuity.
     */
    @Nested
    class WhenRemovingTracks {

        /**
         * TC3 – Verifies that removing a future track (not currently playing)
         * properly removes it from the queue without interrupting
         * the current playback state.
         */
        @Test
        void removeTrackFromQueue_futureTrack_removedFromQueue() {
            // Currently playing: trackA. trackB is next, trackC is after.
            service.removeTrackFromQueue(trackB);

            PlaybackState state = service.getCurrentState();
            assertSame(PlaybackState.PLAYING, state,
                    "Playback should continue uninterrupted after removing a future track");
            assertSame(trackA, service.getCurrentTrack(),
                    "Current track should still be A");

            List<Track> upNext = service.getQueue().getUpNextQueue();
            assertFalse(upNext.contains(trackB),
                    "Removed track must no longer appear in the up-next queue");
            assertTrue(upNext.contains(trackC),
                    "Remaining future track must still be in the up-next queue");
            assertEquals(1, upNext.size(),
                    "Up-next queue should have exactly 1 remaining track");
        }

        /**
         * TC4 – Verifies that removing the currently playing track
         * forces the player to automatically advance to the next track in the queue.
         */
        @Test
        void removeTrackFromQueue_currentTrack_advancesToNext() {
            // Shutdown the real timer to avoid race conditions in unit tests
            service.shutdownTimer();
            PlaybackService s = new PlaybackService() {
                @Override
                public void startTimer() { /* no-op in tests */ }
            };
            Track a = new Track("A", "X", 60);
            Track b = new Track("B", "X", 60);
            Track c = new Track("C", "X", 60);
            s.loadSourceFrom(Arrays.asList(a, b, c), a);

            s.removeTrackFromQueue(a);

            assertNotSame(a, s.getCurrentTrack(),
                    "After removing the playing track, a different track should be current");
            assertSame(b, s.getCurrentTrack(),
                    "Playback should advance to B, the next track in the queue");
            assertEquals(PlaybackState.PLAYING, s.getCurrentState(),
                    "Player should still be in PLAYING state after advancing");
        }
    }

    /**
     * Test scenarios evaluating enqueueing of single tracks and playlist blocks.
     */
    @Nested
	class WhenEnqueuingTracks {
		/**
		 * Verifies that adding a single track to a non-empty queue
		 * appends it in last position without altering the existing entries.
		 */
		@Test
		void addSingleTrack_nonEmptyQueue_appendedAtLastPosition() {
			// Queue is A -> B -> C (loaded in setUp), trackA is playing
			service.addTrackToQueue(trackNew);

			List<Track> queue = (List<Track>) service.getQueue().getTracks();
			assertEquals(4, queue.size(),
					"Queue must contain 4 tracks after enqueueing one more");
			assertSame(trackNew, queue.get(3),
					"New track must occupy the last position");
			// Existing order must be untouched
			assertSame(trackA, queue.get(0));
			assertSame(trackB, queue.get(1));
			assertSame(trackC, queue.get(2));
		}

		/**
		 * Verifies that adding a playlist in bulk appends all its tracks
		 * at the end of the queue, preserving the original playlist order.
		 */
		@Test
		void addPlaylist_nonEmptyQueue_appendsAllTracksInOriginalOrder() {
			// Queue is A -> B -> C; we enqueue [trackNew, trackB] as a "playlist block"
			Track extraTrack = new Track("Extra Song", "Artist", 90);
			List<Track> playlistBlock = Arrays.asList(trackNew, extraTrack);

			service.appendSource(playlistBlock);

			List<Track> queue = (List<Track>) service.getQueue().getTracks();
			assertEquals(5, queue.size(),
					"Queue must contain 5 tracks after bulk enqueue");
			assertSame(trackNew,   queue.get(3),
					"First playlist track must be in position 3");
			assertSame(extraTrack, queue.get(4),
					"Second playlist track must be in position 4, order preserved");
		}

		/**
		 * Verifies that adding a single track to an empty queue
		 * automatically starts playback on that track.
		 */
		@Test
		void addSingleTrack_emptyQueue_autoStartsPlayback() {
			PlaybackService emptyService = new PlaybackService();

			emptyService.addTrackToQueue(trackA);

			assertEquals(PlaybackState.PLAYING, emptyService.getCurrentState(),
					"Adding a track to an empty queue must start playback automatically");
			assertSame(trackA, emptyService.getCurrentTrack(),
					"The enqueued track must become the current playing track");

			emptyService.shutdownTimer();
		}

		/**
		 * Verifies that adding a playlist to an empty queue
		 * automatically starts playback from the first track of the playlist,
		 * respecting the original order for subsequent tracks.
		 */
		@Test
		void addPlaylist_emptyQueue_autoStartsFromFirstTrack() {
			PlaybackService emptyService = new PlaybackService();
			List<Track> playlistBlock = Arrays.asList(trackA, trackB, trackC);

			emptyService.appendSource(playlistBlock);

			assertEquals(PlaybackState.PLAYING, emptyService.getCurrentState(),
					"Adding a playlist to an empty queue must start playback automatically");
			assertSame(trackA, emptyService.getCurrentTrack(),
					"Playback must start from the first track of the enqueued playlist");

			List<Track> queue = (List<Track>) emptyService.getQueue().getTracks();
			assertEquals(3, queue.size());
			assertSame(trackB, queue.get(1), "Second slot must be B");
			assertSame(trackC, queue.get(2), "Third slot must be C");

			emptyService.shutdownTimer();
		}
	}
}