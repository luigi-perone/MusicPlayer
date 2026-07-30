package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-008: the playlist summary counters after a round trip
 * through the JSON file. The track count and the total duration are read from the
 * live track list, which only exists once the stored track ids have been resolved
 * against the library, so these figures are meaningful only end-to-end.
 */
class PlaylistSummaryIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us008-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us008-playlist.json";
    private static final String PLAYLIST_NAME      = "Summary-IT";

    /** Durations of the three seeded tracks, summing to 365 seconds. */
    private static final int DURATION_A = 120;
    private static final int DURATION_B = 200;
    private static final int DURATION_C = 45;
    private static final int TOTAL_DURATION = DURATION_A + DURATION_B + DURATION_C;

    private MusicPlayerFacade facade;
    private PlaylistService   playlistService;

    private Track trackA;
    private Track trackB;
    private Track trackC;

    /** Resets singletons, builds the facade and seeds three tracks in the library. */
    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);

        trackA = addToLibrary("US008 A", "Artist1", DURATION_A);
        trackB = addToLibrary("US008 B", "Artist2", DURATION_B);
        trackC = addToLibrary("US008 C", "Artist3", DURATION_C);
    }

    /** Shuts down playback, deletes the test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    /** Scenarios reading the counters of a populated playlist. */
    @Nested
    @DisplayName("Counters of a populated playlist")
    class WhenPopulated {

        /** Verifies the counters before any reload, as the detail view shows them live. */
        @Test
        @DisplayName("count and total duration are correct in memory")
        void counters_areCorrectInMemory() {
            Playlist playlist = seededPlaylist();

            assertEquals(3, playlist.getTrackCount(), "the playlist must report three tracks");
            assertEquals(TOTAL_DURATION, playlist.getTotalDuration(),
                    "the total duration must be the sum of the three track durations");
        }

        /** Verifies that the counters survive a round trip through the JSON file. */
        @Test
        @DisplayName("count and total duration survive a reload from disk")
        void counters_surviveAReload() {
            seededPlaylist();

            Playlist reloaded = reloadPlaylist();

            assertNotNull(reloaded, "the playlist must be found again after the reload");
            assertEquals(3, reloaded.getTrackCount(),
                    "the reloaded playlist must report three tracks, resolved from the library");
            assertEquals(TOTAL_DURATION, reloaded.getTotalDuration(),
                    "the reloaded playlist must report the same total duration");
        }

        /** Verifies that the reload preserves the original track order. */
        @Test
        @DisplayName("the track order is preserved across the reload")
        void order_isPreservedAcrossTheReload() {
            seededPlaylist();

            List<String> titles = new ArrayList<>();
            for (Track t : reloadPlaylist().getTracks()) {
                titles.add(t.getTitle());
            }

            assertEquals(Arrays.asList("US008 A", "US008 B", "US008 C"), titles,
                    "the stored order must come back unchanged");
        }
    }

    /** Scenarios reading the counters of an empty playlist. */
    @Nested
    @DisplayName("Counters of an empty playlist")
    class WhenEmpty {

        /** Verifies that a playlist created and never filled reports zeroed counters. */
        @Test
        @DisplayName("a never-filled playlist reports zeroed counters after a reload")
        void neverFilled_reportsZeroedCounters() {
            playlistService.createPlaylist(PLAYLIST_NAME);

            Playlist reloaded = reloadPlaylist();

            assertNotNull(reloaded, "an empty playlist must still be stored and found again");
            assertEquals(0, reloaded.getTrackCount(), "an empty playlist must report no tracks");
            assertEquals(0, reloaded.getTotalDuration(), "an empty playlist must report a zero duration");
        }

        /**
         * Verifies that emptying a playlist that was itself loaded from disk is stored as
         * empty, instead of falling back to the track ids read at load time and silently
         * resurrecting the removed tracks on the next reload.
         */
        @Test
        @DisplayName("emptying a reloaded playlist is stored as empty")
        void emptyingAReloadedPlaylist_isStoredAsEmpty() {
            seededPlaylist();

            PlaylistService secondSession = new PlaylistService(TEST_PLAYLIST_PATH);
            Playlist loaded = secondSession.getPlaylist(PLAYLIST_NAME);
            assertEquals(3, loaded.getTrackCount(), "precondition: the reloaded playlist holds three tracks");

            for (Track t : new ArrayList<>(loaded.getTracks())) {
                loaded.removeTrack(t);
            }
            secondSession.save();

            Playlist thirdSession = new PlaylistService(TEST_PLAYLIST_PATH).getPlaylist(PLAYLIST_NAME);

            assertEquals(0, thirdSession.getTrackCount(),
                    "the emptied playlist must come back empty, not repopulated from the stored ids");
            assertEquals(0, thirdSession.getTotalDuration(),
                    "the emptied playlist must report a zero total duration");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Creates the test playlist and fills it with the three seeded tracks. */
    private Playlist seededPlaylist() {
        Playlist playlist = playlistService.createPlaylist(PLAYLIST_NAME);
        playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));
        return playlist;
    }

    /** Reads the test playlist back from disk with a fresh service instance. */
    private Playlist reloadPlaylist() {
        return new PlaylistService(TEST_PLAYLIST_PATH).getPlaylist(PLAYLIST_NAME);
    }

    /** Resets the Library and facade singletons and points the library at the test file. */
    private void resetSingletons() throws Exception {
        Field libraryInstance = Library.class.getDeclaredField("instance");
        libraryInstance.setAccessible(true);
        libraryInstance.set(null, null);

        Library lib = Library.getInstance();
        Field libraryPath = lib.getClass().getSuperclass().getDeclaredField("path");
        libraryPath.setAccessible(true);
        libraryPath.set(lib, TEST_LIBRARY_PATH);
        lib.clearLibrary();
    }

    /** Builds the facade singleton wired to the given playlist service as observer. */
    private MusicPlayerFacade buildFacade(PlaylistService ps) throws Exception {
        MusicPlayerFacade f = new MusicPlayerFacade();

        Field psField = MusicPlayerFacade.class.getDeclaredField("playlistService");
        psField.setAccessible(true);

        PlaylistService old = (PlaylistService) psField.get(f);
        f.removeObserver(old);
        psField.set(f, ps);
        f.addObserver(ps);
        return f;
    }

    /** Adds a track to the library and returns the created {@link Track}. */
    private Track addToLibrary(String title, String author, int duration) {
        facade.addNewTrackToLibrary(title, author, duration, null, null);
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Track not found in library: " + title));
    }
}

