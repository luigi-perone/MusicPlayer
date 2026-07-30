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
import it.unisa.gruppo7.musicplayer.undo.UndoManager;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    /** Resets singletons, builds the facade and starts a three-track playlist as the active source. */
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

    /** Shuts down playback, clears the undo stack, deletes test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        CommandInvoker.getUndoManager().clear();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    /** Scenarios undoing a track removal from a playlist. */
    @Nested
    @DisplayName("Undo of a removal")
    class WhenUndoingRemoval {

        /** Verifies that undoing a removal restores the track's index in the model, file and queue. */
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
            assertEquals(Arrays.asList(trackA, trackB, trackC), playlist.getTracks(),
                    "the surrounding tracks must keep their original order after the undo");
            assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),
                    "the queue must be restored to its original order");
            assertTrue(reloadPlaylist().getTracks().contains(trackB),
                    "B must be present again in the persisted playlist file");
        }

        /**
         * Verifies that undoing the removal of the head track puts it back at index zero
         * rather than appending it elsewhere.
         */
        @Test
        @DisplayName("restores the first track at index zero")
        void undoRemove_firstTrack_restoresAtIndexZero() throws Exception {
            RemoveTrackCommand command = new RemoveTrackCommand(playlistService, service, playlist, trackA);

            command.execute(); // model: remove A (was index 0)
            facade.onTrackRemovedFromPlaylist(playlist, trackA);

            assertEquals(0, playlist.indexOf(trackB), "B must slide up to index 0 while A is out");

            command.undo();

            assertEquals(0, playlist.indexOf(trackA), "A must return to the head of the playlist");
            assertEquals(Arrays.asList(trackA, trackB, trackC), playlist.getTracks(),
                    "the playlist must be back to its original order");
            assertTrue(reloadPlaylist().getTracks().contains(trackA),
                    "A must be present again in the persisted playlist file");
        }

        /**
         * Verifies that undoing the removal of the tail track puts it back at the last
         * index, the boundary case of the i
         */
        @Test
        @DisplayName("restores the last track at the tail index")
        void undoRemove_lastTrack_restoresAtLastIndex() throws Exception {
            RemoveTrackCommand command = new RemoveTrackCommand(playlistService, service, playlist, trackC);

            command.execute(); // model: remove C (was index 2, the tail)
            facade.onTrackRemovedFromPlaylist(playlist, trackC);

            assertEquals(2, playlist.getTrackCount(), "the playlist must be down to two tracks");

            command.undo();

            assertEquals(2, playlist.indexOf(trackC), "C must return to the tail index");
            assertEquals(Arrays.asList(trackA, trackB, trackC), playlist.getTracks(),
                    "the playlist must be back to its original order");
            assertTrue(reloadPlaylist().getTracks().contains(trackC),
                    "C must be present again in the persisted playlist file");
        }
    }

    /** Scenarios undoing a track addition to a playlist. */
    @Nested
    @DisplayName("Undo of an addition")
    class WhenUndoingAddition {

        /** Verifies that undoing an addition removes the added track from the model, file and queue. */
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

        /** Verifies that undoing an addition removes only the newly added tracks, not pre-existing ones. */
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

    /** Scenarios undoing through the CommandInvoker and the UndoManager stack. */
    @Nested
    @DisplayName("Undo through the CommandInvoker and the UndoManager stack")
    class WhenUndoingThroughStack {

        /** Verifies that execute pushes the command and undoLast reverts it end-to-end. */
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

        /**
         * Verifies that a real removal recorded through an invoker is still reversible
         * while its time-to-live has not elapsed.
         */
        @Test
        @DisplayName("within the TTL the recorded removal is still reversible")
        void withinTtl_removalIsStillReversible() throws Exception {
            MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
            CommandInvoker invoker =
                    new CommandInvoker(new UndoManager(Duration.ofSeconds(10), clock), e -> { });

            invoker.invoke(new RemoveTrackCommand(playlistService, service, playlist, trackB));
            facade.onTrackRemovedFromPlaylist(playlist, trackB);
            assertFalse(playlist.getTracks().contains(trackB), "precondition: B must have been removed");

            clock.advance(Duration.ofSeconds(9));

            assertTrue(invoker.getUndoHistory().isUndoAvailable(),
                    "before the TTL elapses the action must still be undoable");
            assertTrue(invoker.getUndoHistory().undoLast(), "the undo must report success");
            assertEquals(1, playlist.indexOf(trackB), "B must be restored at its original index");
        }

        /**
         * Verifies that once the time-to-live has elapsed with no user intervention the
         * removal becomes definitive: nothing is left on the stack and the track stays
         * out of both the model and the persisted file.
         */
        @Test
        @DisplayName("past the TTL the removal becomes definitive")
        void pastTtl_removalBecomesDefinitive() throws Exception {
            MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
            CommandInvoker invoker =
                    new CommandInvoker(new UndoManager(Duration.ofSeconds(10), clock), e -> { });

            invoker.invoke(new RemoveTrackCommand(playlistService, service, playlist, trackB));
            facade.onTrackRemovedFromPlaylist(playlist, trackB);
            assertTrue(invoker.getUndoHistory().isUndoAvailable(),
                    "precondition: the executed command must be on the stack");

            clock.advance(Duration.ofSeconds(11));

            assertFalse(invoker.getUndoHistory().isUndoAvailable(),
                    "once the TTL has elapsed no undo must be offered any more");
            assertFalse(invoker.getUndoHistory().undoLast(),
                    "undoLast must report failure on an expired action");
            assertFalse(playlist.getTracks().contains(trackB),
                    "the expired removal must stand in the playlist model");
            assertFalse(reloadPlaylist().getTracks().contains(trackB),
                    "the expired removal must stand in the persisted playlist file");
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
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }

    /** Clock whose current instant can be advanced manually, to drive the TTL scenarios. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
