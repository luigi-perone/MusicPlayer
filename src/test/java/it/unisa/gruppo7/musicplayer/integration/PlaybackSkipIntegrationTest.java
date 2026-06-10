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
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-013: next/previous skip navigation.
 * Uses a playlist as the playback source to guarantee track order,
 * since the library internally uses a HashSet with no ordering guarantees.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

        // Usa una playlist come sorgente: l'ordine di inserimento è garantito
        // a differenza della libreria che usa HashSet internamente
        playlist = playlistService.createPlaylist("SkipTest");
        playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

        facade.playFromPlaylistFrom(playlist, trackA);
        stopTimerAndResetExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    // ------------------------------------------------------------------
    // IT-013-1: skip next da posizione intermedia avanza alla traccia successiva
    // ------------------------------------------------------------------
    @Test
    @Order(1)
    void skipNext_fromMiddleOfQueue_advancesToNextTrack() {
        // A è in riproduzione; skip a B
        service.playNext();
        stopTimerAndResetExecutor();

        assertSame(trackB, facade.getCurrentPlayingTrack(),
                "After skipping next from A, current track must be B");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state after skip");
    }

    // ------------------------------------------------------------------
    // IT-013-2: due skip next consecutivi raggiungono la coda
    // ------------------------------------------------------------------
    @Test
    @Order(2)
    void skipNext_twice_reachesTailTrack() {
        service.playNext(); // A → B
        stopTimerAndResetExecutor();
        service.playNext(); // B → C
        stopTimerAndResetExecutor();

        assertSame(trackC, facade.getCurrentPlayingTrack(),
                "After two skips from A, current track must be C");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state after reaching tail");
    }

    // ------------------------------------------------------------------
    // IT-013-3: skip next dall'ultima traccia con repeat OFF ferma la riproduzione
    // ------------------------------------------------------------------
    @Test
    @Order(3)
    void skipNext_fromLastTrack_repeatOff_stopsPlayback() {
        service.setRepeatMode(RepeatMode.OFF);

        service.playNext(); // A → B
        stopTimerAndResetExecutor();
        service.playNext(); // B → C
        stopTimerAndResetExecutor();

        assertSame(trackC, facade.getCurrentPlayingTrack());

        service.playNext(); // C → fine coda → stop
        // stop() cancella il timer internamente, nessun stopTimer necessario

        assertEquals(PlaybackState.STOPPED, facade.getPlaybackState(),
                "Player must stop when skipping past the last track with repeat OFF");
        assertNull(facade.getCurrentPlayingTrack(),
                "Current track must be null after playback stops");
    }

    // ------------------------------------------------------------------
    // IT-013-4: skip previous da posizione intermedia torna indietro di uno
    // ------------------------------------------------------------------
    @Test
    @Order(4)
    void skipPrevious_fromMiddleOfQueue_goesBackOneTrack() {
        service.playNext(); // A → B
        stopTimerAndResetExecutor();

        assertSame(trackB, facade.getCurrentPlayingTrack());

        service.playPrevious(); // B → A
        stopTimerAndResetExecutor();

        assertSame(trackA, facade.getCurrentPlayingTrack(),
                "After skipping previous from B, current track must be A");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state after skip previous");
    }

    // ------------------------------------------------------------------
    // IT-013-5: skip previous dalla prima traccia la riavvia (nessun wrap)
    // ------------------------------------------------------------------
    @Test
    @Order(5)
    void skipPrevious_fromFirstTrack_restartsCurrentTrack() {
        assertSame(trackA, facade.getCurrentPlayingTrack());

        service.playPrevious(); // A è la prima → riavvia A
        stopTimerAndResetExecutor();

        assertSame(trackA, facade.getCurrentPlayingTrack(),
                "Skipping previous from the first track must restart it, not wrap");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state");
    }

    // ------------------------------------------------------------------
    // IT-013-6: skip previous quando il player è fermo riproduce l'ultima traccia
    // ------------------------------------------------------------------
    @Test
    @Order(6)
    void skipPrevious_whenStopped_playsLastTrack() {
        service.setRepeatMode(RepeatMode.OFF);

        service.playNext(); // A → B
        stopTimerAndResetExecutor();
        service.playNext(); // B → C
        stopTimerAndResetExecutor();
        service.playNext(); // C → stop

        assertEquals(PlaybackState.STOPPED, facade.getPlaybackState());
        assertNull(facade.getCurrentPlayingTrack());

        service.playPrevious(); // da stopped → ultima traccia della coda
        stopTimerAndResetExecutor();

        assertSame(trackC, facade.getCurrentPlayingTrack(),
                "Skipping previous when stopped must play the last track in the queue");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must be in PLAYING state after skip previous from stopped");
    }

    // ------------------------------------------------------------------
    // IT-013-7: skip next con REPEAT_PLAYLIST torna al primo elemento
    // ------------------------------------------------------------------
    @Test
    @Order(7)
    void skipNext_fromLastTrack_repeatPlaylist_wrapsToFirst() {
        service.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

        service.playNext(); // A → B
        stopTimerAndResetExecutor();
        service.playNext(); // B → C
        stopTimerAndResetExecutor();

        assertSame(trackC, facade.getCurrentPlayingTrack());

        service.playNext(); // C → A (wrap)
        stopTimerAndResetExecutor();

        assertSame(trackA, facade.getCurrentPlayingTrack(),
                "With REPEAT_PLAYLIST, skipping past the last track must wrap to A");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state after wrap");
    }

    // ------------------------------------------------------------------
    // IT-013-8: skip next con REPEAT_ONE riproduce di nuovo la traccia corrente
    // ------------------------------------------------------------------
    @Test
    @Order(8)
    void skipNext_repeatOne_replaysCurrentTrack() {
        service.setRepeatMode(RepeatMode.REPEAT_ONE);

        service.playNext(); // deve riprodurre ancora A, non avanzare a B
        stopTimerAndResetExecutor();

        assertSame(trackA, facade.getCurrentPlayingTrack(),
                "With REPEAT_ONE, skip next must replay the current track");
        assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                "Player must remain in PLAYING state");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Ferma il timer corrente e reinizializza l'executor in modo che
     * il prossimo play() possa schedulare nuovi task senza eccezioni.
     * Usare shutdownNow() senza reinizializzare causa RejectedExecutionException
     * alla chiamata successiva di startTimer().
     */
    private void stopTimerAndResetExecutor() {
        service.stopTimer();
        // Reinizializza l'executor solo se è stato spento da un test precedente
        // stopTimer() usa cancel() sul task, non shutdown sull'executor,
        // quindi normalmente non serve reinizializzare — ma lo facciamo
        // per sicurezza in caso di stati inconsistenti tra test
        try {
            Field timerField = PlaybackService.class.getDeclaredField("timer");
            timerField.setAccessible(true);
            java.util.concurrent.ScheduledExecutorService ex =
                    (java.util.concurrent.ScheduledExecutorService) timerField.get(service);
            if (ex.isShutdown()) {
                timerField.set(service,
                        Executors.newScheduledThreadPool(1));
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

    private Track addToLibrary(String title, String author, int duration) {
        facade.addNewTrackToLibrary(title, author, duration, null, null);
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }
}