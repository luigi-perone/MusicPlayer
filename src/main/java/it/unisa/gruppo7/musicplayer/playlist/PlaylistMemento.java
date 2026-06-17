package it.unisa.gruppo7.musicplayer.playlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Immutable memento of a {@link Playlist}'s ordered track list, used to restore
 * the playlist exactly as it was (content and order) before an operation.
 */
public final class PlaylistMemento {

    private final List<Track> tracks;

    /**
     * Creates a snapshot. Package-private: instances are produced by
     * {@link Playlist#snapshot()}.
     *
     * @param tracks the ordered tracks currently in the playlist.
     */
    PlaylistMemento(Collection<Track> tracks) {
        this.tracks = new ArrayList<>(tracks);
    }

    List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }
}
