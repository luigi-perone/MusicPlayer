package it.unisa.gruppo7.musicplayer.playback;

/**
 * Narrow interface of the playback-queue memento.
 * Deliberately empty: this is the only view the caretaker (the undoable commands) has of
 * the snapshot, so a memento can be held and handed back to {@link PlaybackList} without
 * ever exposing its contents. The concrete state lives in a private nested class of
 * {@link PlaybackList} — the originator — which alone can create and read it.
 */
public interface QueueMemento {}