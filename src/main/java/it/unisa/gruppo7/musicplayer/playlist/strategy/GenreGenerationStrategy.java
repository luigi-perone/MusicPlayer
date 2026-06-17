package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generation strategy that selects the tracks belonging to a given genre.
 *
 * @author francescoLemmo
 */
public class GenreGenerationStrategy implements PlaylistGenerationStrategy {
    private final String targetGenre;

    /**
     * Creates a strategy that matches tracks of the given genre.
     *
     * @param targetGenre the genre to match (case-insensitive, trimmed)
     */
    public GenreGenerationStrategy(String targetGenre) {
        this.targetGenre = targetGenre;
    }


    /**
     * Returns the tracks whose genre matches the target genre.
     *
     * @param allTracks the tracks to filter
     * @return the tracks belonging to the target genre
     */
    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track.getGenre() != null &&
                                       track.getGenre().trim().equalsIgnoreCase(targetGenre.trim()))
                .collect(Collectors.toList());
    }
}
