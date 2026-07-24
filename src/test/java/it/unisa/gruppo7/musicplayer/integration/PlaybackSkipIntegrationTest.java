package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-013: next/previous skip navigation.
 * Uses a playlist as the playback source to guarantee track order.
 */
class PlaybackSkipIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-skip-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-skip-playlist.json";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;
    private Playlist          playlist;

    private Track trackA;
    private Track trackB;
    private Track trackC;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Resets singletons and starts playback of a three-track playlist from track A. */
    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
        service         = facade.getPlaybackService();

        trackA = addToLibrary("Skip A", "Artist", 120);
        trackB = addToLibrary("Skip B", "Artist", 120);
        trackC = addToLibrary("Skip C", "Artist", 120);

        playlist = playlistService.createPlaylist("SkipTest");
        playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

        facade.playFromPlaylistFrom(playlist, trackA);
        stopTimerAndResetExecutor();
    }

    /** Shuts down playback, deletes the test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    // ------------------------------------------------------------------
    // Nested: Skip Next Tests
    // ------------------------------------------------------------------
    /** Integration scenarios for skipping to the next track. */
    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenSkippingNext {

        /** Verifies that skipping next from the middle of the queue advances one track. */
        @Test
        @Order(1)
        void fromMiddleOfQueue_advancesToNextTrack() {
            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackB, facade.getCurrentPlayingTrack(),
                    "After skipping next from A, current track must be B");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state after skip");
        }

        /** Verifies that skipping next twice reaches the last track. */
        @Test
        @Order(2)
        void twice_reachesTailTrack() {
            service.playNext();
            stopTimerAndResetExecutor();
            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackC, facade.getCurrentPlayingTrack(),
                    "After two skips from A, current track must be C");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state after reaching tail");
        }

        /** Verifies that skipping next past the last track with repeat OFF stops playback. */
        @Test
        @Order(3)
        void fromLastTrack_repeatOff_stopsPlayback() {
            service.setRepeatMode(RepeatMode.OFF);

            service.playNext();
            stopTimerAndResetExecutor();
            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackC, facade.getCurrentPlayingTrack());

            service.playNext();

            assertEquals(PlaybackState.STOPPED, facade.getPlaybackState(),
                    "Player must stop when skipping past the last track with repeat OFF");
            assertNull(facade.getCurrentPlayingTrack(),
                    "Current track must be null after playback stops");
        }

        /** Verifies that skipping next past the last track with repeat-playlist wraps to the first. */
        @Test
        @Order(4)
        void fromLastTrack_repeatPlaylist_wrapsToFirst() {
            service.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

            service.playNext();
            stopTimerAndResetExecutor();
            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackC, facade.getCurrentPlayingTrack());

            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "With REPEAT_PLAYLIST, skipping past the last track must wrap to A");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state after wrap");
        }

        /** Verifies that skipping next with repeat-one replays the current track. */
        @Test
        @Order(5)
        void repeatOne_replaysCurrentTrack() {
            service.setRepeatMode(RepeatMode.REPEAT_ONE);

            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "With REPEAT_ONE, skip next must replay the current track");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state");
        }
    }

    // ------------------------------------------------------------------
    // Nested: Skip Previous Tests
    // ------------------------------------------------------------------
    /** Integration scenarios for skipping to the previous track. */
    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenSkippingPrevious {

        /** Verifies that skipping previous from the middle of the queue goes back one track. */
        @Test
        @Order(1)
        void fromMiddleOfQueue_goesBackOneTrack() {
            service.playNext();
            stopTimerAndResetExecutor();

            assertSame(trackB, facade.getCurrentPlayingTrack());

            service.playPrevious();
            stopTimerAndResetExecutor();

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "After skipping previous from B, current track must be A");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state after skip previous");
        }

        /** Verifies that skipping previous from the first track restarts it instead of wrapping. */
        @Test
        @Order(2)
        void fromFirstTrack_restartsCurrentTrack() {
            assertSame(trackA, facade.getCurrentPlayingTrack());

            service.playPrevious();
            stopTimerAndResetExecutor();

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "Skipping previous from the first track must restart it, not wrap");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state");
        }

        /** Verifies that skipping previous while stopped plays the last track in the queue. */
        @Test
        @Order(3)
        void whenStopped_playsLastTrack() {
            service.setRepeatMode(RepeatMode.OFF);

            service.playNext();
            stopTimerAndResetExecutor();
            service.playNext();
            stopTimerAndResetExecutor();
            service.playNext();

            assertEquals(PlaybackState.STOPPED, facade.getPlaybackState());
            assertNull(facade.getCurrentPlayingTrack());

            service.playPrevious();
            stopTimerAndResetExecutor();

            assertSame(trackC, facade.getCurrentPlayingTrack(),
                    "Skipping previous when stopped must play the last track in the queue");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must be in PLAYING state after skip previous from stopped");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Stops the playback timer and replaces its executor if it was shut down. */
    private void stopTimerAndResetExecutor() {
        service.stopTimer();
        try {
            Field timerField = PlaybackService.class.getDeclaredField("timer");
            timerField.setAccessible(true);
            java.util.concurrent.ScheduledExecutorService ex =
                    (java.util.concurrent.ScheduledExecutorService) timerField.get(service);
            if (ex.isShutdown()) {
                timerField.set(service, Executors.newScheduledThreadPool(1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not reset executor", e);
        }
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
}