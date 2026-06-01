package it.unisa.gruppo7.musicplayer.playlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import it.unisa.gruppo7.musicplayer.track.Track;
import java.time.Year;
import java.util.ArrayList;

/**
 * This is a Test Class for the Playlist model class.
 * @author Maxim Makhovskyy
 */
class PlaylistTest {

    private Playlist playlist;
    private Track trk1;
    private Track trk2;
    private Track trk3;

    @BeforeEach
    void setUp() {
        playlist = new Playlist("My Playlist", new ArrayList<>());
        trk1 = new Track("Bohemian Rhapsody", "Queen", 354, "Rock", Year.of(1975));
        trk2 = new Track("Billie Jean", "Michael Jackson", 294, "Pop", Year.of(1982));
        trk3 = new Track("Lose Yourself", "Eminem", 326, "Hip-Hop", Year.of(2002));
    }

    @Nested
    class WhenCreated {

        @Test
        void hasCorrectName() {
            assertEquals("My Playlist", playlist.getName());
        }

        @Test
        void isEmpty() {
            assertEquals(0, playlist.getTrackCount());
        }

        @Test
        void hasZeroDuration() {
            assertEquals(0, playlist.getTotalDuration());
        }
    }

    @Nested
    class WhenAddingTracks {

        @Test
        void countIncreasesAfterAdd() {
            playlist.addTrack(trk1);
            assertEquals(1, playlist.getTrackCount());
        }

        @Test
        void countIsCorrectWithMultipleTracks() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.addTrack(trk3);
            assertEquals(3, playlist.getTrackCount());
        }

        @Test
        void durationUpdatesAfterAdd() {
            playlist.addTrack(trk1);
            assertEquals(354, playlist.getTotalDuration());
        }

        @Test
        void durationIsCorrectWithMultipleTracks() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.addTrack(trk3);
            assertEquals(354 + 294 + 326, playlist.getTotalDuration());
        }
    }

    @Nested
    class WhenRemovingTracks {
        @Test
        void countDecreasesAfterRemove() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertEquals(1, playlist.getTrackCount());
        }

        @Test
        void durationUpdatesAfterRemove() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertEquals(294, playlist.getTotalDuration());
        }

        @Test
        void otherTracksAreUntouched() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertTrue(playlist.getPlaylist().contains(trk2));
        }

        @Test
        void playlistIsEmptyAfterRemovingAll() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            playlist.removeTrack(trk2);
            assertEquals(0, playlist.getTrackCount());
            assertEquals(0, playlist.getTotalDuration());
        }
    }
}
