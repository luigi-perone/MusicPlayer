package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-005: playlist creation through the MusicPlayerFacade.
 * Each scenario checks both the in-memory state and what actually reaches the JSON
 * file, by reloading the playlists from disk with a second service instance.
 */
class PlaylistCreationIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us005-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us005-playlist.json";

    private MusicPlayerFacade facade;
    private PlaylistService   playlistService;

    /** Resets singletons and builds a facade wired to an empty test playlist file. */
    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
    }

    /** Shuts down playback, deletes the test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    /** Scenarios creating a playlist with a valid name. */
    @Nested
    @DisplayName("Creation with a valid name")
    class WithValidName {

        /** Verifies that a valid name produces a playlist visible through the facade. */
        @Test
        @DisplayName("the playlist becomes visible through the facade")
        void validName_playlistIsVisibleThroughTheFacade() {
            Playlist created = facade.createPlaylist("Nuova Playlist");

            assertNotNull(created, "the facade must return the created playlist");
            assertEquals("Nuova Playlist", created.getName(), "the name must match the requested one");
            assertEquals(1, facade.getPlaylists().size(), "the facade must expose exactly one playlist");
        }

        /** Verifies that the creation reaches the JSON file without an explicit save. */
        @Test
        @DisplayName("the playlist survives a reload from disk")
        void validName_playlistIsPersisted() {
            facade.createPlaylist("Nuova Playlist");

            List<Playlist> reloaded = reloadFromDisk();

            assertEquals(1, reloaded.size(), "the stored file must contain exactly one playlist");
            assertEquals("Nuova Playlist", reloaded.get(0).getName(),
                    "the stored playlist must carry the requested name");
        }

        /** Verifies that a newly created playlist starts out with no tracks. */
        @Test
        @DisplayName("the playlist starts empty")
        void validName_playlistStartsEmpty() {
            Playlist created = facade.createPlaylist("Nuova Playlist");

            assertEquals(0, created.getTrackCount(), "a freshly created playlist must hold no tracks");
            assertEquals(0, reloadFromDisk().get(0).getTrackCount(),
                    "the stored playlist must be empty as well");
        }
    }

    /** Scenarios rejecting blank, empty or null names. */
    @Nested
    @DisplayName("Creation with an empty name")
    class WithEmptyName {

        /** Verifies that an empty name is rejected by the facade. */
        @Test
        @DisplayName("an empty name is rejected")
        void emptyName_isRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> facade.createPlaylist(""),
                    "an empty name must be refused");
        }

        /** Verifies that a whitespace-only name is rejected by the facade. */
        @Test
        @DisplayName("a whitespace-only name is rejected")
        void whitespaceName_isRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> facade.createPlaylist("   "),
                    "a blank name must be refused");
        }

        /** Verifies that a null name is rejected by the facade. */
        @Test
        @DisplayName("a null name is rejected")
        void nullName_isRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> facade.createPlaylist(null),
                    "a null name must be refused");
        }

        /** Verifies that a rejected name leaves neither the collection nor the file touched. */
        @Test
        @DisplayName("a rejected name leaves the collection and the file untouched")
        void emptyName_leavesStateAndFileUntouched() {
            facade.createPlaylist("Valida");

            try { facade.createPlaylist(""); } catch (IllegalArgumentException ignored) {}

            assertEquals(1, facade.getPlaylists().size(),
                    "the refused creation must not grow the in-memory collection");
            assertEquals(1, reloadFromDisk().size(),
                    "the refused creation must not reach the stored file");
        }
    }

    /** Scenarios rejecting a name already in use. */
    @Nested
    @DisplayName("Creation with a duplicate name")
    class WithDuplicateName {

        /** Seeds a playlist whose name the following tests collide with. */
        @BeforeEach
        void existingPlaylist() {
            facade.createPlaylist("Duplicata");
        }

        /** Verifies that a colliding name is rejected by the facade. */
        @Test
        @DisplayName("a duplicate name is rejected")
        void duplicateName_isRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> facade.createPlaylist("Duplicata"),
                    "a name already in use must be refused");
        }

        /** Verifies that the rejection leaves a single record both in memory and on disk. */
        @Test
        @DisplayName("a rejected duplicate leaves a single record in memory and on disk")
        void duplicateName_leavesASingleRecord() {
            try { facade.createPlaylist("Duplicata"); } catch (IllegalArgumentException ignored) {}

            assertEquals(1, facade.getPlaylists().size(),
                    "the collection must still hold a single playlist");

            List<Playlist> reloaded = reloadFromDisk();
            assertEquals(1, reloaded.size(), "the stored file must still hold a single playlist");
            assertEquals("Duplicata", reloaded.get(0).getName(),
                    "the surviving record must be the original one");
        }

        /** Verifies that a different name is still accepted alongside the existing one. */
        @Test
        @DisplayName("a different name is still accepted alongside the existing one")
        void differentName_isStillAccepted() {
            facade.createPlaylist("Un'altra");

            assertEquals(2, facade.getPlaylists().size(), "both playlists must coexist in memory");
            assertEquals(2, reloadFromDisk().size(), "both playlists must reach the stored file");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Reads the playlists back from the test file with a fresh service instance. */
    private List<Playlist> reloadFromDisk() {
        return new PlaylistService(TEST_PLAYLIST_PATH).getPlaylists();
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
}
