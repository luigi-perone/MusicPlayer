package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author francescoLemmo
 */
public class YearGenerationStrategy implements PlaylistGenerationStrategy {
    private final Year targetYear;

    public YearGenerationStrategy(int targetYear) {
        this.targetYear = Year.of(targetYear);
    }

    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track.getPublicationYear() != null &&
                        track.getPublicationYear().equals(targetYear))
                .collect(Collectors.toList());
    }
}
