package it.unisa.gruppo7.musicplayer.undo;

import java.util.ArrayList;
import java.util.List;

import it.unisa.gruppo7.musicplayer.playback.PlaybackList;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Immutable Memento of a {@link PlaybackList}'s state, used to
 * restore the playback queue exactly as it was before an operation, including the
 * canonical track order, the shuffled order and the cursor position.
 *
 * The lists are defensively copied so later mutations of the live queue do not
 * affect the captured state. This captures the queue structure only, not
 * the audio currently playing (the live track and timer in {@link PlaybackService}
 * are intentionally left untouched on restore).
 */
public final class QueueMemento {
    private final List<Track> canonicalTracks;
    private final List<Track> shuffledTracks;
    private final int         currentIndex;
    private final boolean     shuffleActive;

    /**
     * Creates a snapshot.
     *
     * @param canonicalTracks the canonical (non-shuffled) track order.
     * @param shuffledTracks  the shuffled track order.
     * @param currentIndex    the cursor position.
     * @param shuffleActive   whether shuffle was active.
     */
    public QueueMemento(List<Track> canonicalTracks, List<Track> shuffledTracks,
                 int currentIndex, boolean shuffleActive) {
        this.canonicalTracks = new ArrayList<>(canonicalTracks);
        this.shuffledTracks  = new ArrayList<>(shuffledTracks);
        this.currentIndex    = currentIndex;
        this.shuffleActive   = shuffleActive;
    }

    /**
     * Returns a copy of the captured canonical (non-shuffled) track order.
     *
     * @return a defensive copy of the canonical tracks
     */
    public List<Track> getCanonicalTracks() { return new ArrayList<>(canonicalTracks); }

    /**
     * Returns a copy of the captured shuffled track order.
     *
     * @return a defensive copy of the shuffled tracks
     */
    public List<Track> getShuffledTracks() { return new ArrayList<>(shuffledTracks); }

    /**
     * Returns the captured cursor position.
     *
     * @return the current index at capture time
     */
    public int getCurrentIndex() { return currentIndex; }

    /**
     * Returns whether shuffle was active at capture time.
     *
     * @return {@code true} if shuffle was active
     */
    public boolean isShuffleActive() { return shuffleActive; }
}
