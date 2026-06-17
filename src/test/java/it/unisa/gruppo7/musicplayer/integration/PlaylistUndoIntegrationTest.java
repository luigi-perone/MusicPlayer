package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.command.AddTracksCommand;
import it.unisa.gruppo7.musicplayer.playlist.command.RemoveTrackCommand;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-020: undo of add/remove operations on a playlist.
 * Verifies that the undo restores the playlist model, the persisted file and the
 * active playback queue, and that a removed track returns to its original index.
 */
class PlaylistUndoIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us020-pl-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us020-pl-playlist.json";
    private static final String PLAYLIST_NAME      = "Undo-PL";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;
    private Playlist          playlist;

    private Track trackA, trackB, trackC, trackD;

    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
        service         = facade.getPlaybackService();
        CommandInvoker.getUndoManager().clear();

        trackA = addToLibrary("US020 A", "Artist1", 120);
        trackB = addToLibrary("US020 B", "Artist2", 120);
        trackC = addToLibrary("US020 C", "Artist3", 120);
        trackD = addToLibrary("US020 D", "Artist4", 120);

        playlist = playlistService.createPlaylist(PLAYLIST_NAME);
        playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

        facade.playFromPlaylistFrom(playlist, trackA); // playlist becomes the active queue source
        service.stopTimer();
    }

    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        CommandInvoker.getUndoManager().clear();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    @Nested
    @DisplayName("Undo of a removal")
    class WhenUndoingRemoval {

        @Test
        @DisplayName("restores the track at its original index, in the model, file and queue")
        void undoRemove_restoresIndexFileAndQueue() throws Exception {
            RemoveTrackCommand command = new RemoveTrackCommand(playlistService, service, playlist, trackB);

            command.execute(); // model: remove B (was index 1)
            facade.onTrackRemovedFromPlaylist(playlist, trackB); // controller-side queue sync

            assertFalse(playlist.getTracks().contains(trackB), "B must be gone from the playlist");
            assertFalse(queueTracks().contains(trackB), "B must be gone from the active queue");

            command.undo();

            assertEquals(1, playlist.indexOf(trackB), "B must return to its original index (1)");
            assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),
                    "the queue must be restored to its original order");
            assertTrue(reloadPlaylist().getTracks().contains(trackB),
                    "B must be present again in the persisted playlist file");
        }
    }

    @Nested
    @DisplayName("Undo of an addition")
    class WhenUndoingAddition {

        @Test
        @DisplayName("removes the added track from the model, file and queue")
        void undoAdd_removesAddedTrackEverywhere() throws Exception {
            AddTracksCommand command = new AddTracksCommand(playlistService, service, playlist, Arrays.asList(trackD));

            command.execute(); // model: add D
            facade.onTrackAddedToPlaylist(playlist, trackD); // controller-side queue sync

            assertTrue(playlist.getTracks().contains(trackD), "D must be in the playlist after add");
            assertTrue(queueTracks().contains(trackD), "D must be in the queue after add");

            command.undo();

            assertFalse(playlist.getTracks().contains(trackD), "D must be removed from the playlist on undo");
            assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),"the queue must be restored to its original order");
            assertFalse(reloadPlaylist().getTracks().contains(trackD),"D must be absent from the persisted playlist file");
        }

        @Test
        @DisplayName("removes only the newly added tracks, not pre-existing duplicates")
        void undoAdd_removesOnlyAddedSubset() throws Exception {
            // A is already in the playlist; only D is actually added.
            AddTracksCommand command = new AddTracksCommand(playlistService, service, playlist, Arrays.asList(trackA, trackD));

            command.execute();
            assertTrue(playlist.getTracks().contains(trackD), "D must have been added");

            command.undo();

            assertTrue(playlist.getTracks().contains(trackA),"the pre-existing track A must remain after undo");
            assertFalse(playlist.getTracks().contains(trackD),"the added track D must be removed by undo");
            assertEquals(3, playlist.getTrackCount(), "the playlist must be back to its 3 original tracks");
        }
    }

    @Nested
    @DisplayName("Undo through the CommandInvoker and the UndoManager stack")
    class WhenUndoingThroughStack {

        @Test
        @DisplayName("execute pushes the command and undoLast reverts it end-to-end")
        void invokerPushes_andUndoLastReverts() throws Exception {
            RemoveTrackCommand command = new RemoveTrackCommand(playlistService, service, playlist, trackB);

            CommandInvoker.execute(command, e -> { });
            facade.onTrackRemovedFromPlaylist(playlist, trackB);

            assertFalse(playlist.getTracks().contains(trackB), "B must be removed after the invoked command");
            assertTrue(CommandInvoker.getUndoManager().isUndoAvailable(), "the executed undoable command must be on the stack");

            assertTrue(CommandInvoker.getUndoManager().undoLast(), "undoLast must revert the last action");
            assertEquals(1, playlist.indexOf(trackB), "B must be restored at its original index");
        }
    }
    
    private List<Track> queueTracks() {
        return new ArrayList<>(service.getQueue().getTracks());
    }

    private Playlist reloadPlaylist() {
        return new PlaylistService(TEST_PLAYLIST_PATH).getPlaylist(PLAYLIST_NAME);
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
