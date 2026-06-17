package it.unisa.gruppo7.musicplayer.library;

/**
 * Opaque marker interface representing an immutable memento of the {@link Library} contents.
 * It exposes no methods, ensuring strict Information Hiding from the Caretaker (Commands).
 *
 * It is used to restore the library exactly as it was before an operation.
 * In the underlying implementation, the track set is defensively copied;
 * the uniqueness signatures are rebuilt from it on restore, so they are not stored here.
 */
public interface LibraryMemento {}
