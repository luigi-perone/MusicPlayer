package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;
import java.util.List;

/**
 * Strategy used to generate the contents of an automatic playlist.
 * <p>
 * Implementations filter a collection of tracks according to a specific
 * criterion (e.g. genre, publication year or tags).
 *
 * @author francescoLemmo
 */
public interface PlaylistGenerationStrategy {

    /**
     * Filters a collection of tracks according to this strategy's criterion.
     *
     * @param allTracks the tracks to filter
     * @return the tracks matching the criterion
     */
    List<Track> generate(Collection<Track> allTracks);
}
