package it.unisa.gruppo7.musicplayer.playlist;

/**
 * Opaque marker interface representing an immutable memento of a {@link Playlist}'s ordered track list.
 * It exposes no methods, ensuring strict Information Hiding from the Caretaker (Commands).
 *
 * It is used to restore the playlist exactly as it was (content and order) before an operation.
 * The underlying implementation defensively copies the track list to ensure the captured state 
 * remains unaffected by subsequent mutations.
 */
public interface PlaylistMemento {}
