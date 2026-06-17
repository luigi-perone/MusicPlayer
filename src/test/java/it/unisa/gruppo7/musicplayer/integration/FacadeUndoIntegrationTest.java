package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.command.RemoveTrackCommand;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FacadeUndoActionIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us020-facade-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us020-facade-playlist.json";
    private static final String PLAYLIST_NAME      = "Undo-Facade";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;
    private Playlist          playlist;

    private Track trackA, trackB, trackC;

    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
        service         = facade.getPlaybackService();
        CommandInvoker.getUndoManager().clear();

        trackA = addToLibrary("Facade A", "Artist", 120);
        trackB = addToLibrary("Facade B", "Artist", 120);
        trackC = addToLibrary("Facade C", "Artist", 120);

        playlist = playlistService.createPlaylist(PLAYLIST_NAME);
        playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));
    }

    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        CommandInvoker.getUndoManager().clear();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    @Test
    @DisplayName("undoLastAction reverts the most recent undoable command")
    void undoLastAction_revertsLastCommand() {
        CommandInvoker.execute(
                new RemoveTrackCommand(playlistService, service, playlist, trackB),
                e -> {});

        assertFalse(playlist.getTracks().contains(trackB), "B must be removed after the command");

        assertTrue(facade.undoLastAction(), "undoLastAction must report that something was undone");
        assertEquals(1, playlist.indexOf(trackB), "B must be restored at its original index");
    }

    @Test
    @DisplayName("undoLastAction returns false when there is nothing to undo")
    void undoLastAction_emptyStack_returnsFalse() {
        CommandInvoker.getUndoManager().clear();
        assertFalse(facade.undoLastAction(), "with an empty stack undoLastAction must return false");
    }

    @Test
    @DisplayName("a second consecutive undoLastAction returns false")
    void undoLastAction_secondCall_returnsFalse() {
        CommandInvoker.execute(
                new RemoveTrackCommand(playlistService, service, playlist, trackB),
                e -> { });

        assertTrue(facade.undoLastAction(), "the first undo must succeed");
        assertFalse(facade.undoLastAction(), "the second undo must find nothing left to revert");
    }

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

    private Track addToLibrary(String title, String author, int duration) {
        facade.addNewTrackToLibrary(title, author, duration, null, null);
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }
}
