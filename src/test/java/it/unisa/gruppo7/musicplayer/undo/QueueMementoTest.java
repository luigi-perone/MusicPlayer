package it.unisa.gruppo7.musicplayer.undo;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US-020: the playback-queue snapshot used to undo operations that
 * affect the active queue.
 */
class QueueMementoTest {

    private PlaybackService service;
    private Track trackA, trackB, trackC, trackD;

    @BeforeEach
    void setUp() {
        service = new PlaybackService();
        service.setTimer(new NoOpScheduler()); // no background timer threads in tests

        trackA = new Track("Alpha", "Artist1", 120);
        trackB = new Track("Beta",  "Artist2", 120);
        trackC = new Track("Gamma", "Artist3", 120);
        trackD = new Track("Delta", "Artist4", 120);
    }

    @AfterEach
    void tearDown() {
        service.getQueue().clear();
    }

    @Test
    @DisplayName("Restore recovers the exact content and order after mutations")
    void restore_recoversContentAndOrder() {
        service.loadSource(Arrays.asList(trackA, trackB, trackC));

        QueueMemento snapshot = service.captureQueueState();

        service.addTrackToQueue(trackD);
        service.removeTrackFromQueue(trackB);

        service.restoreQueueState(snapshot);

        assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(),"the queue content and order must match the snapshot");
    }

    @Test
    @DisplayName("Restore recovers the cursor position")
    void restore_recoversCursor() {
        service.loadSource(Arrays.asList(trackA, trackB, trackC));
        service.playNext(); // cursor -> 1 (B)
        QueueMemento snapshot = service.captureQueueState();

        service.playNext(); // cursor -> 2 (C)
        assertSame(trackC, service.getCurrentTrack());

        service.restoreQueueState(snapshot);

        assertEquals(1, service.getQueue().getCurrentIndex(),"the cursor index must be restored");
        assertSame(trackB, service.getQueue().getCurrentTrack(),"the cursor must point back at B");
    }

    @Test
    @DisplayName("Restore recovers the shuffle flag and canonical order")
    void restore_recoversShuffleState() {
        service.loadSource(Arrays.asList(trackA, trackB, trackC));
        QueueMemento snapshot = service.captureQueueState(); // shuffle OFF

        service.getQueue().setShuffle(true, trackA);
        assertTrue(service.getQueue().isShuffleActive());

        service.restoreQueueState(snapshot);

        assertFalse(service.getQueue().isShuffleActive(),"shuffle must be disabled again, as in the snapshot");
        assertEquals(Arrays.asList(trackA, trackB, trackC), queueTracks(), "the canonical order must be restored");
    }

    private List<Track> queueTracks() {
        return new ArrayList<>(service.getQueue().getTracks());
    }

    static class NoOpScheduler extends ScheduledThreadPoolExecutor {
        NoOpScheduler() { super(1); }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return new DummyScheduledFuture<>();
        }
    }

    static class DummyScheduledFuture<V> implements ScheduledFuture<V> {
        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { return true; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return null; }
        @Override public V get(long timeout, TimeUnit unit) { return null; }
    }
}

