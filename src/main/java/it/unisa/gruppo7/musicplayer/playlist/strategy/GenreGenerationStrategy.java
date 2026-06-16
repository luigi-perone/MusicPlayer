package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author francescoLemmo
 */
public class GenreGenerationStrategy implements PlaylistGenerationStrategy {
    private final String targetGenre;

    public GenreGenerationStrategy(String targetGenre) {
        this.targetGenre = targetGenre;
    }


    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track.getGenre() != null &&
                                       track.getGenre().trim().equalsIgnoreCase(targetGenre.trim()))
                .collect(Collectors.toList());
    }
}
