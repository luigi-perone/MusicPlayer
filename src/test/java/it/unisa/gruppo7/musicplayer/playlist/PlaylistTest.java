package it.unisa.gruppo7.musicplayer.playlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;

import java.time.Year;
import java.util.ArrayList;

/**
 * Unit test suite for the {@link Playlist} model class.
 * Verifies initial creation states, track addition behaviors, dynamic duration aggregation,
 * and removal mutations.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
class PlaylistTest {

    private Playlist playlist;
    private Track trk1;
    private Track trk2;
    private Track trk3;

    /**
     * Initializes testing variables and sets up an empty playlist baseline before each test.
     */
    @BeforeEach
    void setUp() {
        playlist = new Playlist("My Playlist", new ArrayList<>());
        trk1 = new Track("Bohemian Rhapsody", "Queen", 354, "Rock", Year.of(1975));
        trk2 = new Track("Billie Jean", "Michael Jackson", 294, "Pop", Year.of(1982));
        trk3 = new Track("Lose Yourself", "Eminem", 326, "Hip-Hop", Year.of(2002));
    }

    /**
     * Test scenarios verifying state parameters directly following playlist initialization.
     */
    @Nested
    class WhenCreated {

        /**
         * Assures the metadata name attribute matches initial parameters.
         */
        @Test
        void hasCorrectName() {
            assertEquals("My Playlist", playlist.getName());
        }

        /**
         * Confirms that a newly instantiated playlist contains zero tracks.
         */
        @Test
        void isEmpty() {
            assertEquals(0, playlist.getTrackCount());
        }

        /**
         * Confirms that the cumulative duration metrics of an empty playlist equals zero.
         */
        @Test
        void hasZeroDuration() {
            assertEquals(0, playlist.getTotalDuration());
        }
    }

    /**
     * Test scenarios evaluating track insertion behaviors and size tracking updates.
     */
    @Nested
    class WhenAddingTracks {

        /**
         * Verifies that adding a single track increments the overall track count by one.
         */
        @Test
        void countIncreasesAfterAdd() {
            playlist.addTrack(trk1);
            assertEquals(1, playlist.getTrackCount());
        }

        /**
         * Verifies that the track count scales accurately when adding multiple tracks.
         */
        @Test
        void countIsCorrectWithMultipleTracks() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.addTrack(trk3);
            assertEquals(3, playlist.getTrackCount());
        }

        /**
         * Verifies that the total play duration updates properly after adding a track.
         */
        @Test
        void durationUpdatesAfterAdd() {
            playlist.addTrack(trk1);
            assertEquals(354, playlist.getTotalDuration());
        }

        /**
         * Verifies that cumulative duration metrics dynamically sum multiple track values.
         */
        @Test
        void durationIsCorrectWithMultipleTracks() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.addTrack(trk3);
            assertEquals(354 + 294 + 326, playlist.getTotalDuration());
        }
    }

    /**
     * Test scenarios evaluating track removal behaviors and metric updates.
     */
    @Nested
    class WhenRemovingTracks {

        /**
         * Verifies that removing a track correctly decrements the overall track count.
         */
        @Test
        void countDecreasesAfterRemove() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertEquals(1, playlist.getTrackCount());
        }

        /**
         * Verifies that removing a track correctly subtracts its length from the cumulative duration.
         */
        @Test
        void durationUpdatesAfterRemove() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertEquals(294, playlist.getTotalDuration());
        }

        /**
         * Assures that non-targeted track records are left unmodified during removal actions.
         */
        @Test
        void otherTracksAreUntouched() {
            playlist.addTrack(trk1);
            playlist.addTrack(trk2);
            playlist.removeTrack(trk1);
            assertTrue(playlist.getTracks().contains(trk2));
        }

        /**
         * Confirms that removing all sequential tracks returns metrics cleanly back to zero.
         */
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