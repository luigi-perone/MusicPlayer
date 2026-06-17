package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlaybackService}, verifying that starting a new track
 * replaces the current one and resets the playback timer.
 */
class PlaybackServiceTest {

    private PlaybackService playbackService;

    /** Creates a fresh playback service before each test. */
    @BeforeEach
    void setUp() {
        playbackService = new PlaybackService();
    }

    /** Shuts down the playback timer after each test. */
    @AfterEach
    void tearDown() {
        // Shutdown timer process
        playbackService.shutdownTimer();
    }

    /** Verifies that playing a second track replaces the first and resets the elapsed time. */
    @Test
    void testPlaySecondTrackReplacesFirstAndResetsTime() {

        Track track1 = new Track("Bohemian Rhapsody", "Queen", 354);
        Track track2 = new Track("Stairway to Heaven", "Led Zeppelin", 482);

        playbackService.play(track1);

        // Check if track1 is in play
        assertEquals(track1, playbackService.getCurrentTrack());
        assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());


        playbackService.getSimulatedTimeSeconds().set(45);
        assertEquals(45, playbackService.getSimulatedTimeSeconds().get());

        // play Track2 while track1 is playing
        playbackService.play(track2);


        assertEquals(track2, playbackService.getCurrentTrack(),
                "The currentTrack must be updated to track2");

        assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState(),
                "The service must be in PLAYING state");

        assertEquals(0, playbackService.getSimulatedTimeSeconds().get(),
                "The timer set to 0 when played the new track");
    }
}