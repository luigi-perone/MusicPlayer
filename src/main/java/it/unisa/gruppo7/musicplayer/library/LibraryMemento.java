package it.unisa.gruppo7.musicplayer.library;

/**
 * Narrow interface of the library memento: deliberately empty.
 * The concrete state lives in a private nested class of {@link Library} — the originator —
 * which alone can create and read it; callers only hold this opaque view.
 */
public interface LibraryMemento {}
