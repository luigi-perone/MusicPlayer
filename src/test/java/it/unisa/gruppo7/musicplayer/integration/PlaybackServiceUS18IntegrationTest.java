package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.time.Year;
import java.util.*;
import java.util.concurrent.*;
import it.unisa.gruppo7.musicplayer.playback.*;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-018: modifying the playback queue during active playback.
 * Uses hand-written stubs (instead of a mocking framework) for tracks, the timer and
 * an observer, and covers standard additions, additions under shuffle, removal of a
 * future track, removal of the playing track (auto-skip) and observer notifications.
 */
@DisplayName("US-018 — Modifica della playlist durante la riproduzione")
public class PlaybackServiceUS18IntegrationTest {

    private static final int TRACK_DURATION = 200;

    // =========================================================================
    // Manual stubs replacing Mockito
    // =========================================================================

    /**
     * Manual stub of Track.
     * Note: if Track is an interface in your project, change "extends" to "implements".
     */
    static class StubTrack extends Track {
        private final String title;

        public StubTrack(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() { return title; }

        @Override
        public String getAuthor() { return "Artista"; }

        @Override
        public int getDuration() { return TRACK_DURATION; }

        @Override
        public Year getPublicationYear() { return Year.of(2020); }
    }

    /**
     * Dummy future used to avoid exceptions when the timer is stopped.
     */
    static class DummyScheduledFuture<V> implements ScheduledFuture<V> {
        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { return true; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return null; }
        @Override public V get(long timeout, TimeUnit unit) { return null; }
    }

    /**
     * Dummy scheduler that never runs the background task.
     */
    static class NoOpScheduler extends ScheduledThreadPoolExecutor {
        public NoOpScheduler() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return new DummyScheduledFuture<>();
        }
    }

    // =========================================================================
    // Capturing observer
    // =========================================================================

    /** Observer that records the track, state and queue-change notifications it receives. */
    static class CapturingObserver implements PlaybackObserver {

        final List<Track> trackChanges = new ArrayList<>();
        final List<PlaybackState> stateChanges = new ArrayList<>();
        int queueChangedCount = 0;

        @Override
        public void onTimeTick(int simulatedSeconds) {}

        @Override
        public void onTrackChanged(Track track) {
            trackChanges.add(track);
        }

        @Override
        public void onStateChanged(PlaybackState newState) {
            stateChanges.add(newState);
        }

        @Override
        public void onQueueChanged() {
            queueChangedCount++;
        }
    }

    // =========================================================================
    // Common fixture
    // =========================================================================

    private PlaybackService service;
    private CapturingObserver observer;
    private Track trackA, trackB, trackC, trackD;

    /** Builds a playback service with a no-op timer, a capturing observer and stub tracks. */
    @BeforeEach
    void setUp() {
        service = new PlaybackService();

        // Inject the timer stub
        service.setTimer(new NoOpScheduler());

        observer = new CapturingObserver();
        service.addObserver(observer);

        // Create the stub tracks
        trackA = new StubTrack("Alpha");
        trackB = new StubTrack("Beta");
        trackC = new StubTrack("Gamma");
        trackD = new StubTrack("Delta");
    }

    /** Clears the queue after each test. */
    @AfterEach
    void tearDown() {
        service.getQueue().clear();
    }

    // =========================================================================
    // Scenario 1 — Standard addition (shuffle off)
    // =========================================================================

    /** Scenario 1: adding tracks with shuffle disabled. */
    @Nested
    @DisplayName("Scenario 1 — Aggiunta standard (shuffle off)")
    class AggiuntaStandard {

        /** Verifies that an added track is appended to the end of the canonical queue. */
        @Test
        @DisplayName("La traccia viene inserita in fondo alla coda canonica")
        void inserisceInFondo() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));

            service.addTrackToQueue(trackD);

            List<Track> canonical = new ArrayList<>(service.getQueue().getTracks());
            assertEquals(4, canonical.size(),
                    "La coda deve contenere 4 tracce dopo l'aggiunta");
            assertSame(trackD, canonical.get(3),
                    "D deve essere posizionata in ultima posizione");

            assertSame(trackA, service.getCurrentTrack(),
                    "La traccia in riproduzione non deve cambiare");

            assertEquals(1, observer.queueChangedCount,
                    "onQueueChanged deve essere notificato esattamente una volta");
        }

        /** Verifies that the insertion order is preserved for multiple additions. */
        @Test
        @DisplayName("L'ordine di inserimento è preservato per aggiunte multiple")
        void ordineInserimentoPreservato() {
            service.loadSource(Arrays.asList(trackA));

            service.addTrackToQueue(trackB);
            service.addTrackToQueue(trackC);
            service.addTrackToQueue(trackD);

            List<Track> canonical = new ArrayList<>(service.getQueue().getTracks());
            assertEquals(4, canonical.size());
            assertSame(trackB, canonical.get(1), "B deve essere in posizione 1");
            assertSame(trackC, canonical.get(2), "C deve essere in posizione 2");
            assertSame(trackD, canonical.get(3), "D deve essere in posizione 3");
        }

        /** Verifies that adding to an empty queue starts playback automatically. */
        @Test
        @DisplayName("Con coda vuota, l'aggiunta avvia automaticamente la riproduzione")
        void codaVuota_avviaRiproduzioneAutomatica() {
            service.addTrackToQueue(trackA);

            assertSame(trackA, service.getCurrentTrack(),
                    "La traccia aggiunta deve diventare quella in riproduzione");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState(),
                    "Lo stato deve essere PLAYING");
            assertTrue(observer.trackChanges.contains(trackA),
                    "onTrackChanged deve essere stato notificato con trackA");
        }
    }

    // =========================================================================
    // Scenario 2 — Addition with shuffle active
    // =========================================================================

    /** Scenario 2: adding tracks while shuffle is active. */
    @Nested
    @DisplayName("Scenario 2 — Aggiunta con shuffle attivo")
    class AggiuntaConShuffle {

        /** Verifies that the track is inserted after position 0 in the shuffled list. */
        @Test
        @DisplayName("La traccia viene inserita dopo la posizione 0 nella shuffled list")
        void inserisceDopoLaTraciaInCorso() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            service.getQueue().setShuffle(true, trackA);
            assertSame(trackA, service.getQueue().getActiveList().get(0),
                    "Pre-condizione: A deve essere in testa alla shuffled list");

            service.addTrackToQueue(trackD);

            List<Track> shuffled = service.getQueue().getActiveList();
            assertTrue(shuffled.contains(trackD),
                    "D deve essere presente nella shuffled list");

            int indexOfD = shuffled.indexOf(trackD);
            assertNotEquals(0, indexOfD,
                    "D non deve trovarsi in posizione 0, riservata alla traccia in corso");
        }

        /** Verifies that both the canonical queue and the shuffled list grow correctly. */
        @Test
        @DisplayName("Sia la coda canonica sia la shuffled list crescono correttamente")
        void entrambeLeListeCrescono() {
            service.loadSource(Arrays.asList(trackA, trackB));
            service.getQueue().setShuffle(true, trackA);

            service.addTrackToQueue(trackC);
            service.addTrackToQueue(trackD);

            assertEquals(4, service.getQueue().getTracks().size(),
                    "La coda canonica deve contenere 4 tracce");
            assertEquals(4, service.getQueue().getActiveList().size(),
                    "La shuffled list deve contenere 4 tracce");
        }

        /** Verifies that the playing track always stays at the head of the shuffled list. */
        @Test
        @DisplayName("La traccia in corso rimane sempre in testa alla shuffled list")
        void traciaInCorsoRimanteInTesta() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            service.getQueue().setShuffle(true, trackA);

            service.addTrackToQueue(trackD);

            assertSame(trackA, service.getQueue().getActiveList().get(0),
                    "A deve rimanere in posizione 0 della shuffled list");
        }
    }

    // =========================================================================
    // Scenario 3 — Removing a future track
    // =========================================================================

    /** Scenario 3: removing a future (not currently playing) track. */
    @Nested
    @DisplayName("Scenario 3 — Rimozione traccia futura (non in riproduzione)")
    class RimozioneTracciaFutura {

        /** Verifies that the track is removed and the remaining order is correct. */
        @Test
        @DisplayName("La traccia viene rimossa e l'ordine residuo è corretto")
        void rimozioneAggiorna_ordineResiduo() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC, trackD));

            service.removeTrackFromQueue(trackB);

            List<Track> remaining = new ArrayList<>(service.getQueue().getTracks());
            assertEquals(3, remaining.size(),
                    "La coda deve contenere 3 tracce dopo la rimozione");
            assertFalse(remaining.contains(trackB),
                    "B non deve più essere presente nella coda");
            assertSame(trackC, remaining.get(1),
                    "C deve ora occupare la posizione 1");
            assertSame(trackD, remaining.get(2),
                    "D deve ora occupare la posizione 2");
        }

        /** Verifies that the currently playing track is not interrupted. */
        @Test
        @DisplayName("La traccia in riproduzione non viene interrotta")
        void riproduzioneCorrenteNonInterrotta() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));

            service.removeTrackFromQueue(trackC);

            assertSame(trackA, service.getCurrentTrack(),
                    "La traccia in riproduzione deve rimanere A");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState(),
                    "Lo stato deve rimanere PLAYING");
        }

        /** Verifies that subsequent navigation correctly skips the removed track. */
        @Test
        @DisplayName("La navigazione successiva salta correttamente la traccia rimossa")
        void navigazioneSuccessiva_saltoCorretto() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            service.removeTrackFromQueue(trackB);

            service.playNext();

            assertSame(trackC, service.getCurrentTrack(),
                    "Dopo la rimozione di B, playNext deve portare a C");
        }

        /** Verifies that onQueueChanged is notified but onTrackChanged is not. */
        @Test
        @DisplayName("Notifica onQueueChanged ma non onTrackChanged")
        void notificaCorretta_soloQueueChanged() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            int trackChangesBefore = observer.trackChanges.size();

            service.removeTrackFromQueue(trackC);

            assertEquals(1, observer.queueChangedCount,
                    "onQueueChanged deve essere notificato esattamente una volta");
            assertEquals(trackChangesBefore, observer.trackChanges.size(),
                    "onTrackChanged non deve essere notificato per una traccia futura");
        }
    }

    // =========================================================================
    // Scenario 4 — Removing the playing track with auto-skip
    // =========================================================================

    /** Scenario 4: removing the currently playing track (auto-skip). */
    @Nested
    @DisplayName("Scenario 4 — Rimozione traccia in riproduzione (auto-skip)")
    class RimozioneTracciaInCorso {

        /** Verifies that removing the playing track auto-skips to the next one. */
        @Test
        @DisplayName("Auto-skip al brano successivo quando la traccia in corso viene rimossa")
        void autoSkip_AlBranoSuccessivo() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            assertSame(trackA, service.getCurrentTrack());

            service.removeTrackFromQueue(trackA);

            assertSame(trackB, service.getCurrentTrack(),
                    "Dopo la rimozione di A deve partire B");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState(),
                    "La riproduzione deve continuare con stato PLAYING");

            assertTrue(observer.trackChanges.contains(trackB),
                    "onTrackChanged deve essere stato notificato con B");
        }

        /** Verifies that playback stops when there is no following track. */
        @Test
        @DisplayName("Stop della riproduzione quando non ci sono brani successivi")
        void stop_QuandoCodaRimanteVuota() {
            service.loadSource(Arrays.asList(trackA));

            service.removeTrackFromQueue(trackA);

            assertEquals(PlaybackState.STOPPED, service.getCurrentState(),
                    "Senza brani successivi, la riproduzione deve fermarsi");
            assertTrue(observer.trackChanges.contains(null),
                    "onTrackChanged(null) deve essere notificato quando la coda si svuota");
        }

        /** Verifies that the queue is empty after removing the only track. */
        @Test
        @DisplayName("La coda è vuota dopo la rimozione dell'unica traccia")
        void codaVuota_DopoRimozioneUnicaTraccia() {
            service.loadSource(Arrays.asList(trackA));

            service.removeTrackFromQueue(trackA);

            assertTrue(service.getQueue().getTracks().isEmpty(),
                    "La coda canonica deve essere vuota");
            assertTrue(service.getQueue().getUpNextQueue().isEmpty(),
                    "La upNext queue deve essere vuota");
        }

        /** Verifies that with REPEAT_ONE the auto-skip advances to the next available track. */
        @Test
        @DisplayName("Con REPEAT_ONE, l'auto-skip avanza al brano successivo disponibile")
        void repeatOne_autoSkipAlSuccessivo_SeTracciaRimossa() {
            service.loadSource(Arrays.asList(trackA, trackB));
            service.setRepeatMode(RepeatMode.REPEAT_ONE);

            service.removeTrackFromQueue(trackA);

            assertSame(trackB, service.getCurrentTrack(),
                    "Con REPEAT_ONE e A rimossa, deve partire B");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState());
        }

        /** Verifies that with REPEAT_PLAYLIST and an empty queue playback stops. */
        @Test
        @DisplayName("Con REPEAT_PLAYLIST e coda vuota, la riproduzione si ferma")
        void repeatPlaylist_stop_SeRimozioneUnicaTraccia() {
            service.loadSource(Arrays.asList(trackA));
            service.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

            service.removeTrackFromQueue(trackA);

            assertEquals(PlaybackState.STOPPED, service.getCurrentState(),
                    "Con coda vuota dopo la rimozione, la riproduzione deve fermarsi");
        }
    }

    // =========================================================================
    // Event propagation to observers
    // =========================================================================

    /** Verifies the events propagated to observers across queue operations. */
    @Nested
    @DisplayName("Propagazione eventi agli observer")
    class PropagazioneEventi {

        /** Verifies that a standard addition notifies only onQueueChanged, not onTrackChanged. */
        @Test
        @DisplayName("Aggiunta standard: solo onQueueChanged, non onTrackChanged")
        void aggiunta_soloQueueChanged() {
            service.loadSource(Arrays.asList(trackA, trackB));
            int trackChangesBefore = observer.trackChanges.size();

            service.addTrackToQueue(trackC);

            assertEquals(1, observer.queueChangedCount,
                    "onQueueChanged deve essere notificato esattamente una volta");
            assertEquals(trackChangesBefore, observer.trackChanges.size(),
                    "onTrackChanged non deve essere notificato per un'aggiunta standard");
        }

        /** Verifies that adding to an empty queue notifies only onTrackChanged (playback start). */
        @Test
        @DisplayName("Aggiunta a coda vuota: solo onTrackChanged (avvio riproduzione)")
        void aggiunta_codaVuota_soloTrackChanged() {
            service.addTrackToQueue(trackA);

            assertEquals(0, observer.queueChangedCount,
                    "onQueueChanged non deve essere notificato quando si avvia la riproduzione");
            assertTrue(observer.trackChanges.contains(trackA),
                    "onTrackChanged deve essere notificato con la prima traccia");
        }

        /** Verifies that removing a future track fires a single onQueueChanged. */
        @Test
        @DisplayName("Rimozione futura: un solo onQueueChanged")
        void rimozione_futura_unSoloQueueChanged() {
            service.loadSource(Arrays.asList(trackA, trackB, trackC));
            int before = observer.queueChangedCount;

            service.removeTrackFromQueue(trackB);

            assertEquals(before + 1, observer.queueChangedCount,
                    "onQueueChanged deve essere notificato esattamente una volta");
        }

        /** Verifies that removing the playing track fires onTrackChanged, not onQueueChanged. */
        @Test
        @DisplayName("Rimozione in corso: onTrackChanged con la nuova traccia, non onQueueChanged")
        void rimozioneInCorso_onTrackChanged_nonQueueChanged() {
            service.loadSource(Arrays.asList(trackA, trackB));
            int queueChangedBefore = observer.queueChangedCount;

            service.removeTrackFromQueue(trackA);

            assertTrue(observer.trackChanges.contains(trackB),
                    "onTrackChanged deve contenere B come nuova traccia");

            assertEquals(queueChangedBefore, observer.queueChangedCount,
                    "onQueueChanged non deve essere notificato quando scatta l'auto-skip");
        }

        /** Verifies that stopping on an empty queue notifies onStateChanged with STOPPED. */
        @Test
        @DisplayName("Stop per coda vuota: onStateChanged con STOPPED")
        void stop_CodaVuota_notificaStatoStopped() {
            service.loadSource(Arrays.asList(trackA));

            service.removeTrackFromQueue(trackA);

            assertTrue(observer.stateChanges.contains(PlaybackState.STOPPED),
                    "onStateChanged deve essere notificato con STOPPED");
        }
    }
}
