package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests guarding the invariant that, right after a source is (re)loaded,
 * the queue cursor points at the very track that the engine starts playing.
 *
 * <p>This must hold regardless of shuffle: a divergence between the cursor and the
 * playing track corrupts next/previous navigation and the "up next" panel.</p>
 */
class PlaybackServiceLoadShuffleTest {

    private PlaybackService service;

    @AfterEach
    void tearDown() {
        if (service != null) service.shutdownTimer();
    }

    /** Builds a list of distinct tracks A0..A(n-1). */
    private List<Track> makeTracks(int n) {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tracks.add(new Track("Song " + i, "Artist", 120));
        }
        return tracks;
    }

    /**
     * When shuffle is already active and a brand new source is loaded via
     * {@link PlaybackService#loadSource(List)}, the cursor must stay aligned with
     * the track the engine actually plays. The loop defeats the randomness of the
     * shuffle: under the bug the cursor lands on a random shuffled track and the
     * invariant breaks almost every iteration.
     */
    @Test
    void loadSource_withShuffleActive_cursorMatchesPlayingTrack() {
        for (int iteration = 0; iteration < 40; iteration++) {
            service = new PlaybackService() {
                @Override
                public void startTimer() { /* no-op to avoid timer races */ }
            };

            // Enable shuffle first (on an empty queue), then load a fresh source.
            service.getQueue().setShuffle(true, null);
            service.loadSource(makeTracks(10));

            Track playing = service.getCurrentTrack();
            Track cursorTrack = service.getQueue().getCurrentTrack();

            assertSame(playing, cursorTrack,
                    "Queue cursor must point at the track being played after loadSource");

            // The currently playing track must never be listed in 'up next'.
            assertFalse(service.getQueue().getUpNextQueue().contains(playing),
                    "The playing track must not appear in the up-next queue");
        }
    }
}
