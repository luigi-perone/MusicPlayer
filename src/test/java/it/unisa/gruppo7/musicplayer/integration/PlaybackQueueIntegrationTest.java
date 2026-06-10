package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-018: end-to-end validation of playlist modification
 * during active playback, covering the full chain from MusicPlayerFacade
 * through PlaylistService and PlaybackService down to PlaybackList.
 */
class PlaybackQueueIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-playlist.json";

    private MusicPlayerFacade facade;
    private PlaylistService   playlistService;
    private Playlist          testPlaylist;

    private Track trackA;
    private Track trackB;
    private Track trackC;
    private Track trackNew;

    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();

        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade = buildFacade(playlistService);

        trackA   = addToLibrary("Integration A", "Artist", 120);
        trackB   = addToLibrary("Integration B", "Artist", 120);
        trackC   = addToLibrary("Integration C", "Artist", 120);
        trackNew = addToLibrary("Integration New", "Artist", 120);

        testPlaylist = playlistService.createPlaylist("TestPlaylist-IT");
        playlistService.addTracksToPlaylist(testPlaylist, Arrays.asList(trackA, trackB, trackC));

        facade.playFromPlaylistFrom(testPlaylist, trackA);
        facade.getPlaybackService().stopTimer();
    }

    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenAddingTracks {

        /**
         * Tests that adding a new track to a playlist in normal playback mode
         * appends the track to the end of the playback queue and properly persists the change.
         */
        @Test
        @Order(1)
        void normalMode_appendedAtEndOfQueue() {
            playlistService.addTracksToPlaylist(testPlaylist, Arrays.asList(trackNew));
            facade.onTrackAddedToPlaylist(testPlaylist, trackNew);

            List<Track> queue = (List<Track>) facade.getPlaybackService().getQueue().getTracks();
            assertEquals(4, queue.size(),
                    "Queue must contain 4 tracks after addition");
            assertSame(trackNew, queue.get(3),
                    "New track must be the last element in the queue");

            assertTrue(testPlaylist.getTracks().contains(trackNew),
                    "New track must be present in the playlist model");

            PlaylistService freshService = new PlaylistService(TEST_PLAYLIST_PATH);
            Playlist reloaded = freshService.getPlaylist("TestPlaylist-IT");
            assertNotNull(reloaded, "Playlist must survive a reload from disk");
            assertTrue(reloaded.getTracks().contains(trackNew),
                    "New track must be persisted in the playlist file");
        }

        /**
         * Tests that adding a new track while shuffle mode is active inserts the track
         * into the up-next queue while also appending it to the underlying canonical queue.
         */
        @Test
        @Order(2)
        void shuffleActive_insertedAtRandomPosition() {
            facade.shuffleQueue(true, trackA);

            playlistService.addTracksToPlaylist(testPlaylist, Arrays.asList(trackNew));
            facade.onTrackAddedToPlaylist(testPlaylist, trackNew);

            List<Track> upNext = facade.getUpNextQueueFrom();
            assertTrue(upNext.contains(trackNew),
                    "New track must appear in the up-next queue when shuffle is on");

            List<Track> canonical = (List<Track>) facade.getPlaybackService().getQueue().getTracks();
            assertTrue(canonical.contains(trackNew),
                    "New track must also be in the canonical list for when shuffle is toggled off");
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenRemovingTracks {

        /**
         * Tests the removal of a future track from the active playlist.
         * Verifies that playback continues uninterrupted, the track is removed
         * from the up-next queue, and the changes are persisted.
         */
        @Test
        @Order(1)
        void futureTrack_queueRecalculatedPlaybackContinues() {
            assertSame(trackA, facade.getCurrentPlayingTrack());

            testPlaylist.removeTrack(trackB);
            playlistService.save();
            facade.onTrackRemovedFromPlaylist(testPlaylist, trackB);

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "Current track must still be A after removing a future track");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Playback must continue uninterrupted");

            List<Track> upNext = facade.getUpNextQueueFrom();
            assertFalse(upNext.contains(trackB),
                    "Removed track B must no longer be in the up-next queue");
            assertTrue(upNext.contains(trackC),
                    "Track C must still be reachable after B is removed");
            assertEquals(1, upNext.size(),
                    "Only one track must remain in the up-next queue");

            assertFalse(testPlaylist.getTracks().contains(trackB),
                    "Track B must be absent from the in-memory playlist model");

            PlaylistService freshService = new PlaylistService(TEST_PLAYLIST_PATH);
            Playlist reloaded = freshService.getPlaylist("TestPlaylist-IT");
            assertFalse(reloaded.getTracks().contains(trackB),
                    "Track B must be absent from the persisted playlist file");
        }

        /**
         * Tests the removal of the currently playing track from the active playlist.
         * Verifies that the player automatically skips to the next available track
         * and persists the removal.
         */
        @Test
        @Order(2)
        void currentTrack_autoSkipsToNext() {
            assertSame(trackA, facade.getCurrentPlayingTrack());

            testPlaylist.removeTrack(trackA);
            playlistService.save();
            facade.onTrackRemovedFromPlaylist(testPlaylist, trackA);

            assertSame(trackB, facade.getCurrentPlayingTrack(),
                    "Player must auto-skip to B after A is removed while playing");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must still be in PLAYING state after the auto-skip");

            List<Track> upNext = facade.getUpNextQueueFrom();
            assertFalse(upNext.contains(trackA),
                    "Removed track A must not appear anywhere in the queue");

            assertFalse(testPlaylist.getTracks().contains(trackA),
                    "Track A must be absent from the in-memory playlist model");

            PlaylistService freshService = new PlaylistService(TEST_PLAYLIST_PATH);
            Playlist reloaded = freshService.getPlaylist("TestPlaylist-IT");
            assertFalse(reloaded.getTracks().contains(trackA),
                    "Track A must be absent from the persisted playlist file");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
                .orElseThrow(() -> new RuntimeException("Track not found in library: " + title));
    }
}