package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackServiceTest {

    private PlaybackService playbackService;

    @BeforeEach
    void setUp() {
        playbackService = new PlaybackService();
    }

    @AfterEach
    void tearDown() {
        // Shutdown timer process
        playbackService.shutdownTimer();
    }

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