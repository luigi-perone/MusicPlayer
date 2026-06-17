package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.AddTrackToLibraryCommand;
import it.unisa.gruppo7.musicplayer.track.RemoveTrackFromLibraryCommand;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-020: undo of add/remove operations on the library.
 * The removal cascade (library + playlists + playback queue) is reverted via mementos.
 */
class LibraryUndoIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us020-lib-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us020-lib-playlist.json";
    private static final String PLAYLIST_NAME = "Undo-LIB";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;

    /** Resets singletons and builds the facade with an empty library and playlist service. */
    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade = buildFacade(playlistService);
        service = facade.getPlaybackService();
    }

    /** Shuts down playback, deletes the test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    /** Scenarios undoing a library addition. */
    @Nested
    @DisplayName("Undo of a library addition")
    class WhenUndoingAddition {

        /** Verifies that undoing an add removes the freshly added track from the library. */
        @Test
        @DisplayName("removes the freshly added track from the library")
        void undoAdd_removesTrackFromLibrary() throws Exception {
            int sizeBefore = facade.getTracksFromLibrary().size();

            AddTrackToLibraryCommand command = new AddTrackToLibraryCommand(facade, "Fresh Track", "Artist", 90, null, null);

            command.execute();
            assertNotNull(findInLibrary("Fresh Track"), "the track must be in the library after add");

            command.undo();

            assertNull(findInLibrary("Fresh Track"), "the track must be gone from the library after undo");
            assertEquals(sizeBefore, facade.getTracksFromLibrary().size(), "the library size must be back to its original value");
        }
    }

    /** Scenarios undoing a cascading library removal. */
    @Nested
    @DisplayName("Undo of a library removal (cascade)")
    class WhenUndoingRemoval {

        /** Verifies that undoing a removal restores the track in the library, playlist and queue. */
        @Test
        @DisplayName("restores the track in the library, in the affected playlist (at its index) and in the queue")
        void undoRemove_restoresCascade() throws Exception {
            Track trackA = addToLibrary("LIB A", "Artist", 120);
            Track trackX = addToLibrary("LIB X", "Artist", 120);
            Track trackB = addToLibrary("LIB B", "Artist", 120);

            Playlist playlist = playlistService.createPlaylist(PLAYLIST_NAME);
            playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackX, trackB)); // X at index 1

            facade.playFromPlaylistFrom(playlist, trackA);
            service.stopTimer();

            // Preconditions
            assertNotNull(facade.getTrackFromLibrary(trackX.getId()), "X must start in the library");
            assertEquals(1, playlist.indexOf(trackX), "X must start at index 1 in the playlist");
            assertTrue(queueTracks().contains(trackX), "X must start in the active queue");

            RemoveTrackFromLibraryCommand command = new RemoveTrackFromLibraryCommand(facade, trackX);

            command.execute(); // cascading removal

            assertNull(facade.getTrackFromLibrary(trackX.getId()), "X must be gone from the library");
            assertFalse(playlist.getTracks().contains(trackX),     "X must be gone from the playlist");
            assertFalse(queueTracks().contains(trackX),            "X must be gone from the queue");

            command.undo(); // restore the cascade

            assertNotNull(facade.getTrackFromLibrary(trackX.getId()), "X must be restored in the library");
            assertEquals(1, playlist.indexOf(trackX), "X must return to its original index in the playlist");
            assertEquals(Arrays.asList(trackA, trackX, trackB), queueTracks(),"the queue must be restored to its original order");
            assertTrue(reloadPlaylist().getTracks().contains(trackX),"X must be present again in the persisted playlist file");
        }
    }

    /** Returns the current queue tracks as a new list for assertions. */
    private List<Track> queueTracks() {
        return new ArrayList<>(service.getQueue().getTracks());
    }

    /** Reloads the test playlist from disk to verify persistence. */
    private Playlist reloadPlaylist() {
        return new PlaylistService(TEST_PLAYLIST_PATH).getPlaylist(PLAYLIST_NAME);
    }

    /** Finds a track by title in the library, or returns null if absent. */
    private Track findInLibrary(String title) {
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElse(null);
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

        Field facadeInstance = MusicPlayerFacade.class.getDeclaredField("instance");
        facadeInstance.setAccessible(true);
        facadeInstance.set(null, null);
    }

    /** Builds the facade singleton wired to the given playlist service as observer. */
    private MusicPlayerFacade buildFacade(PlaylistService ps) throws Exception {
        MusicPlayerFacade f = MusicPlayerFacade.getInstance();

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
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }
}
