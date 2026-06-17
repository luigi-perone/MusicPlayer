package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for US-027: synchronization of the playback queue when a track
 * is reordered in the active playlist.
 *
 * <p>Verifies the playback-side acceptance criteria: after reordering a track the
 * queue realigns and, above all, the currently playing track is not interrupted,
 * whether a future track or the playing track is moved. The timer is replaced by a
 * no-op scheduler to make the test deterministic (same approach as the US-018 test).</p>
 */
@DisplayName("US-027 — Riordino traccia e sincronizzazione della coda")
public class PlaybackServiceReorderSyncTest {

    /** Inert {@link ScheduledFuture} returned by {@link NoOpScheduler}. */
    static class DummyScheduledFuture<V> implements ScheduledFuture<V> {
        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { return true; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return null; }
        @Override public V get(long timeout, TimeUnit unit) { return null; }
    }

    /** Scheduler that never runs the background tick, keeping playback state stable. */
    static class NoOpScheduler extends ScheduledThreadPoolExecutor {
        public NoOpScheduler() { super(1); }
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return new DummyScheduledFuture<>();
        }
    }

    /** Observer that records the track changes and queue-change notifications it receives. */
    static class CapturingObserver implements PlaybackObserver {
        final List<Track> trackChanges = new ArrayList<>();
        int queueChangedCount = 0;
        @Override public void onTimeTick(int simulatedSeconds) {}
        @Override public void onTrackChanged(Track track) { trackChanges.add(track); }
        @Override public void onStateChanged(PlaybackState newState) {}
        @Override public void onQueueChanged() { queueChangedCount++; }
    }

    private PlaybackService service;
    private CapturingObserver observer;
    private Track a, b, c, d;

    /** Builds a playback service with a no-op timer, a capturing observer and four tracks. */
    @BeforeEach
    void setUp() {
        service = new PlaybackService();
        service.setTimer(new NoOpScheduler());
        observer = new CapturingObserver();
        service.addObserver(observer);

        a = new Track("A", "Artist", 100, "Rock", Year.of(2000));
        b = new Track("B", "Artist", 100, "Rock", Year.of(2000));
        c = new Track("C", "Artist", 100, "Rock", Year.of(2000));
        d = new Track("D", "Artist", 100, "Rock", Year.of(2000));
    }

    /** Clears the queue after each test. */
    @AfterEach
    void tearDown() {
        service.getQueue().clear();
    }

    /** Scenarios reordering a future (not currently playing) track. */
    @Nested
    @DisplayName("Riordino di una traccia futura (non in riproduzione)")
    class FutureTrack {

        /** Verifies that the queue realigns and the playing track is not interrupted. */
        @Test
        @DisplayName("La coda si riallinea e il brano in corso non viene interrotto")
        void queueReordered_currentTrackUntouched() {
            service.loadSource(Arrays.asList(a, b, c, d)); // current = A
            int trackChangesBefore = observer.trackChanges.size();

            service.moveTrackInQueue(1, 3); // B -> end

            assertEquals(Arrays.asList(a, c, d, b),
                    new ArrayList<>(service.getQueue().getTracks()),
                    "La coda deve riflettere il nuovo ordine");
            assertSame(a, service.getCurrentTrack(), "Il brano in corso resta A");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState());
            assertEquals(trackChangesBefore, observer.trackChanges.size(),
                    "Spostare una traccia futura non deve cambiare la traccia in corso");
            assertEquals(1, observer.queueChangedCount,
                    "onQueueChanged deve essere notificato una volta");
        }

        /** Verifies that the up-next queue reflects the new order. */
        @Test
        @DisplayName("La coda 'up next' riflette il nuovo ordine")
        void upNextReflectsNewOrder() {
            service.loadSource(Arrays.asList(a, b, c, d)); // current = A

            service.moveTrackInQueue(3, 1); // D subito dopo A

            assertEquals(Arrays.asList(d, b, c), service.getQueue().getUpNextQueue());
        }
    }

    /** Scenarios reordering the currently playing track (AC3). */
    @Nested
    @DisplayName("Riordino della traccia attualmente in riproduzione (AC3)")
    class CurrentTrack {

        /** Verifies that the playing track continues and the cursor follows it to the new position. */
        @Test
        @DisplayName("Il brano continua e il cursore lo segue nella nuova posizione")
        void playingTrackContinues_cursorFollows() {
            service.loadSource(Arrays.asList(a, b, c)); // current = A
            service.playFromQueue(b);                   // current = B (cursor index 1)
            assertSame(b, service.getCurrentTrack());

            service.moveTrackInQueue(1, 2); // sposta B (in play) in fondo

            assertEquals(Arrays.asList(a, c, b),
                    new ArrayList<>(service.getQueue().getTracks()));
            assertSame(b, service.getCurrentTrack(),
                    "Il brano in riproduzione deve restare B (nessuna interruzione)");
            assertSame(b, service.getQueue().getCurrentTrack(),
                    "Il cursore della coda deve seguire B nella nuova posizione");
            assertEquals(PlaybackState.PLAYING, service.getCurrentState());
        }

        /** Verifies that navigation after the reorder follows the new order. */
        @Test
        @DisplayName("Dopo lo spostamento, la navigazione successiva rispetta il nuovo ordine")
        void nextAfterReorder_followsNewOrder() {
            service.loadSource(Arrays.asList(a, b, c)); // current = A
            service.playFromQueue(a);

            service.moveTrackInQueue(0, 1); // A (in play) tra B e C -> [B, A, C]

            service.playNext();
            assertSame(c, service.getCurrentTrack(),
                    "Dopo A (ora in posizione 1) il brano successivo deve essere C");
        }
    }
}
