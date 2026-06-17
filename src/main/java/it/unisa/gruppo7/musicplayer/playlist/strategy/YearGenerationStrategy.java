package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generation strategy that selects the tracks published in a given year.
 *
 * @author francescoLemmo
 */
public class YearGenerationStrategy implements PlaylistGenerationStrategy {
    private final Year targetYear;

    /**
     * Creates a strategy that matches tracks published in the given year.
     *
     * @param targetYear the publication year to match
     */
    public YearGenerationStrategy(int targetYear) {
        this.targetYear = Year.of(targetYear);
    }

    /**
     * Returns the tracks whose publication year matches the target year.
     *
     * @param allTracks the tracks to filter
     * @return the tracks published in the target year
     */
    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track.getPublicationYear() != null &&
                        track.getPublicationYear().equals(targetYear))
                .collect(Collectors.toList());
    }
}
