package it.unisa.gruppo7.musicplayer.library;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Immutable memento of the {@link Library} contents, used to restore
 * the library exactly as it was before an operation. The track set is defensively copied;
 * the uniqueness signatures are rebuilt from it on restore, so they are not stored here.
 */
public final class LibraryMemento {
    private final Set<Track> tracks;

    /**
     * Creates a snapshot. Package-private: instances are produced by
     * {@link Library#snapshot()}.
     *
     * @param tracks the tracks currently in the library.
     */
    LibraryMemento(Collection<Track> tracks) {
        this.tracks = new HashSet<>(tracks);
    }

    Set<Track> getTracks(){
        return new HashSet<>(tracks);
    }
}
