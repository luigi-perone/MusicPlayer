package it.unisa.gruppo7.musicplayer.playlist;


/**
 * Narrow interface of the playlist memento: deliberately empty.
 * The concrete state lives in a private nested class of {@link Playlist} — the originator —
 * which alone can create and read its captured ordered tracks.
 * 
 */
public interface PlaylistMemento {}
