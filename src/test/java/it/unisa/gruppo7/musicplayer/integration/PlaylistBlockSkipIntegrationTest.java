package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
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
 * Integration tests for US-029: skip to next/previous playlist block within the queue.
 *
 * <p>Queue layout: two playlists appended one after the other.</p>
 * <pre>
 *   block 0 (Playlist 1): p1a p1b
 *   block 1 (Playlist 2): p2a p2b
 * </pre>
 */
class PlaylistBlockSkipIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-blockskip-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-blockskip-playlist.json";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;

    private Track p1a, p1b, p2a, p2b;

    private int blockedCount;
    private int queueChangedCount;
    private int trackChangedCount;
    private Track lastNotifiedTrack;
    private final PlaybackObserver blockedSpy = new PlaybackObserver() {
        @Override public void onTimeTick(int simulatedSeconds) {}
        @Override public void onTrackChanged(Track currentTrack) {
            trackChangedCount++;
            lastNotifiedTrack = currentTrack;
        }
        @Override public void onStateChanged(PlaybackState newState) {}
        @Override public void onPlaylistSkipBlocked() {
            blockedCount++;
        }
        @Override public void onQueueChanged() {
            queueChangedCount++;
        }
    };

    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
        service         = facade.getPlaybackService();
        blockedCount    = 0;
        service.addObserver(blockedSpy);

        p1a = addToLibrary("P1A", 120);
        p1b = addToLibrary("P1B", 120);
        p2a = addToLibrary("P2A", 120);
        p2b = addToLibrary("P2B", 120);

        Playlist pl1 = playlistService.createPlaylist("Playlist1");
        playlistService.addTracksToPlaylist(pl1, Arrays.asList(p1a, p1b));
        Playlist pl2 = playlistService.createPlaylist("Playlist2");
        playlistService.addTracksToPlaylist(pl2, Arrays.asList(p2a, p2b));

        facade.appendPlaylistToQueue(pl1); // block 0, auto-starts on p1a
        facade.appendPlaylistToQueue(pl2); // block 1
        stopTimerAndResetExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    @Test
    void skipNext_jumpsToFirstTrackOfNextPlaylist() {
        assertSame(p1a, facade.getCurrentPlayingTrack(), "playback starts on the first playlist");

        facade.skipToNextPlaylist();
        stopTimerAndResetExecutor();

        assertSame(p2a, facade.getCurrentPlayingTrack(),
                "skip next must jump to the first track of the second playlist");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState());
    }

    @Test
    void skipNext_pastLastPlaylist_isIgnoredAndNotifiesObserver() {
        facade.skipToNextPlaylist();          // -> p2a (last block)
        stopTimerAndResetExecutor();
        assertSame(p2a, facade.getCurrentPlayingTrack());

        facade.skipToNextPlaylist();          // blocked

        assertSame(p2a, facade.getCurrentPlayingTrack(),
                "playback must stay on the current playlist when past the last one");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "the current playback must remain active");
        assertEquals(1, blockedCount, "observer must be notified once that the skip was blocked");
        assertFalse(facade.hasNextPlaylist());
    }

    @Test
    void skipPrevious_jumpsToFirstTrackOfPreviousPlaylist() {
        facade.skipToNextPlaylist();          // -> p2a
        stopTimerAndResetExecutor();

        facade.skipToPreviousPlaylist();      // -> p1a
        stopTimerAndResetExecutor();

        assertSame(p1a, facade.getCurrentPlayingTrack(),
                "skip previous must jump to the first track of the previous playlist");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState());
    }

    @Test
    void skipPrevious_onFirstPlaylist_isIgnoredAndNotifiesObserver() {
        assertSame(p1a, facade.getCurrentPlayingTrack());
        assertFalse(facade.hasPreviousPlaylist());

        facade.skipToPreviousPlaylist();      // blocked

        assertSame(p1a, facade.getCurrentPlayingTrack());
        assertEquals(1, blockedCount, "observer must be notified the skip was blocked");
    }

    @Test
    void skipNext_firesDisplayUpdateNotificationWithNewPlaylistData() {
        trackChangedCount = 0;
        lastNotifiedTrack = null;

        facade.skipToNextPlaylist();   // -> p2a (first track of playlist 2)
        stopTimerAndResetExecutor();

        assertEquals(1, trackChangedCount,
                "skip to next playlist must fire exactly one display-update notification");
        assertSame(p2a, lastNotifiedTrack,
                "the display must be refreshed with the first track of the new playlist");
    }

    @Test
    void skipPrevious_firesDisplayUpdateNotificationWithNewPlaylistData() {
        facade.skipToNextPlaylist();   // -> p2a
        stopTimerAndResetExecutor();

        trackChangedCount = 0;
        lastNotifiedTrack = null;

        facade.skipToPreviousPlaylist(); // -> p1a (first track of playlist 1)
        stopTimerAndResetExecutor();

        assertEquals(1, trackChangedCount,
                "skip to previous playlist must fire exactly one display-update notification");
        assertSame(p1a, lastNotifiedTrack,
                "the display must be refreshed with the first track of the previous playlist");
    }

    @Test
    void blockedSkip_doesNotFireDisplayUpdateNotification() {
        // Already on the first playlist: skip previous is blocked.
        trackChangedCount = 0;

        facade.skipToPreviousPlaylist(); // blocked

        assertEquals(0, trackChangedCount,
                "a blocked skip must not refresh the display");
        assertEquals(1, blockedCount, "a blocked skip must notify observers");
    }

    @Test
    void appendingPlaylistToNonEmptyQueue_notifiesObserversAndEnablesNextPlaylist() {
        // Playing the first track of playlist 1, with only playlist 2 after it.
        assertSame(p1a, facade.getCurrentPlayingTrack());

        // Make playlist 2 the current (last) block: no further playlist to skip to.
        facade.skipToNextPlaylist();
        stopTimerAndResetExecutor();
        assertSame(p2a, facade.getCurrentPlayingTrack());
        assertFalse(facade.hasNextPlaylist(), "on the last playlist there is no next block yet");

        // Append a third playlist while playing -> the current track does not change,
        // but observers must be notified so the UI re-enables the skip-next button.
        Track p3a = addToLibrary("P3A", 120);
        Playlist pl3 = playlistService.createPlaylist("Playlist3");
        playlistService.addTracksToPlaylist(pl3, Arrays.asList(p3a));

        queueChangedCount = 0;
        facade.appendPlaylistToQueue(pl3);

        assertEquals(1, queueChangedCount,
                "appending to a non-empty queue must notify observers (onQueueChanged)");
        assertSame(p2a, facade.getCurrentPlayingTrack(), "the current track must not change");
        assertTrue(facade.hasNextPlaylist(),
                "a following playlist block now exists, so skip-next must be available");
    }

    @Test
    void skip_whileShuffleActive_isBlocked() {
        facade.shuffleQueue(true, facade.getCurrentPlayingTrack());

        facade.skipToNextPlaylist();

        assertEquals(1, blockedCount, "playlist skip is not supported while shuffle is active");
    }

    // ------------------------------------------------------------------
    // Helpers (mirrors PlaybackSkipIntegrationTest)
    // ------------------------------------------------------------------

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

    private Track addToLibrary(String title, int duration) {
        facade.addNewTrackToLibrary(title, "Artist", duration, null, null);
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }
}
