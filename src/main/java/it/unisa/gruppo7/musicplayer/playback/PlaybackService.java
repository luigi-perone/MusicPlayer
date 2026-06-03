package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author francescoLemmo
 */
public class PlaybackService {

    private Track currentTrack;
    private PlaybackState currentState;
    private AtomicInteger simulatedTimeSeconds;

    // Service for background time
    private ScheduledExecutorService timer;

    // Handle used to stop the specified timer
    private ScheduledFuture<?> timerHandle;

    // observer list
    private final List<PlaybackObserver> observers;

    public PlaybackService() {
        this.currentState = PlaybackState.STOPPED;
        this.simulatedTimeSeconds = new AtomicInteger(0);
        this.timer = Executors.newScheduledThreadPool(1);
        this.observers = new ArrayList<>();
    }

    // -- observer methods --

    public void addObserver(PlaybackObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    //
    private void notifyTimeTick(int seconds) {
        for (PlaybackObserver obs : observers) obs.onTimeTick(seconds);
    }

    private void notifyTrackChanged(Track track) {
        for (PlaybackObserver obs : observers) obs.onTrackChanged(track);
    }

    private void notifyStateChanged(PlaybackState newState) {
        for (PlaybackObserver obs : observers) obs.onStateChanged(newState);
    }


    // -- Playback methods --

    public void play(Track track) {
        if (track == null) {
            return;
        }
        this.stop();

        this.currentTrack = track;
        this.currentState = PlaybackState.PLAYING;
        notifyStateChanged(this.currentState);

        notifyTrackChanged(track);
        this.startTimer();
    }

    public void pause() {
        if (this.currentState == PlaybackState.PLAYING) {
            this.currentState = PlaybackState.PAUSED;
            notifyStateChanged(this.currentState);
            this.stopTimer();
        }
    }

    public void resume() {
        if (this.currentState == PlaybackState.PAUSED) {
            this.currentState = PlaybackState.PLAYING;
            notifyStateChanged(this.currentState);
            this.startTimer();
        }
    }

    public void stop() {
        this.currentState = PlaybackState.STOPPED;
        notifyStateChanged(this.currentState);
        this.simulatedTimeSeconds.set(0);
        notifyTimeTick(0);
        this.stopTimer();
        this.currentTrack = null;
    }

    // -- timer methods --

    public void startTimer() {

        if (this.getCurrentState() == PlaybackState.STOPPED) {
            this.simulatedTimeSeconds.set(0);
            notifyTimeTick(0);
        }

        this.timerHandle = this.timer.scheduleAtFixedRate(() -> {

            int currentTime = this.simulatedTimeSeconds.incrementAndGet();
            notifyTimeTick(currentTime);

            if (currentTime >= currentTrack.getDuration()) {
                this.stop();
            }

        }, 1, 1, TimeUnit.SECONDS);

    }

    public void stopTimer() {
        if (timerHandle != null && !timerHandle.isCancelled()) {
            // Stops the timer
            timerHandle.cancel(false);
        }
    }

    public void shutdownTimer() {
       this.stop();

        if (timer != null && !timer.isShutdown()) {
            timer.shutdownNow();
        }
    }

    // --- getter & setter ---

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(Track currentTrack) {
        this.currentTrack = currentTrack;
    }

    public PlaybackState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PlaybackState currentState) {
        this.currentState = currentState;
    }

    public AtomicInteger getSimulatedTimeSeconds() {
        return simulatedTimeSeconds;
    }

    public void setSimulatedTimeSeconds(AtomicInteger simulatedTimeSeconds) {
        this.simulatedTimeSeconds = simulatedTimeSeconds;
    }

    public ScheduledExecutorService getTimer() {
        return timer;
    }

    public void setTimer(ScheduledExecutorService timer) {
        this.timer = timer;
    }

    public ScheduledFuture<?> getTimerHandle() {
        return timerHandle;
    }

    public void setTimerHandle(ScheduledFuture<?> timerHandle) {
        this.timerHandle = timerHandle;
    }
}
