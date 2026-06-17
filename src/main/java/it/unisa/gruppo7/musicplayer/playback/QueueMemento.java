package it.unisa.gruppo7.musicplayer.playback;

/**
 * Opaque marker interface representing an immutable Memento of a {@link PlaybackList}'s state.
 * It exposes no methods, ensuring strict Information Hiding from the Caretaker (Commands).
 *
 * It is used to restore the playback queue exactly as it was before an operation, including the
 * canonical track order, the shuffled order and the cursor position.
 *
 * The underlying implementation defensively copies the lists so later mutations of the live queue 
 * do not affect the captured state. This captures the queue structure only, not
 * the audio currently playing (the live track and timer in {@link PlaybackService}
 * are intentionally left untouched on restore).
 */
public interface QueueMemento {}
