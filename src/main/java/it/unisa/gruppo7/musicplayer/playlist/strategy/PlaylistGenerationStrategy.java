package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;
import java.util.List;

/**
 * @author francescoLemmo
 */
public interface PlaylistGenerationStrategy {

    /**
     * Filters a collection of tracks with a specific criterion
     */
    List<Track> generate(Collection<Track> allTracks);
}
