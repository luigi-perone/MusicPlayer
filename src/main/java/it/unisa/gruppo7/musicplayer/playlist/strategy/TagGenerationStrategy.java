package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Filters library tracks according to a set of predefined tags. */
public class TagGenerationStrategy implements PlaylistGenerationStrategy {
    private final Set<TrackTag> selectedTags;
    private final TagCombinationMode mode;

    /**
     * Creates a tag-based strategy.
     *
     * @param selectedTags the tags a track must match; must not be {@code null} or empty
     * @param mode         how the tags are combined; defaults to {@link TagCombinationMode#ALL} when {@code null}
     * @throws IllegalArgumentException if {@code selectedTags} is {@code null} or empty
     */
    public TagGenerationStrategy(Set<TrackTag> selectedTags, TagCombinationMode mode) {
        if (selectedTags == null || selectedTags.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un tag");
        }
        this.selectedTags = new HashSet<>(selectedTags);
        this.mode = (mode == null) ? TagCombinationMode.ALL : mode;
    }

    /**
     * Returns the tracks matching the selected tags according to the combination mode.
     *
     * @param allTracks the tracks to filter
     * @return the tracks matching the selected tags
     */
    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track != null && matches(track))
                .collect(Collectors.toList());
    }

    /**
     * Tests whether a track matches the selected tags.
     *
     * @param track the track to test
     * @return {@code true} if the track matches according to the current combination mode
     */
    private boolean matches(Track track) {
        if (mode == TagCombinationMode.ALL) {
            return track.getTags().containsAll(selectedTags);
        }
        return selectedTags.stream().anyMatch(track::hasTag);
    }

    /**
     * Returns a copy of the tags used by this strategy.
     *
     * @return a defensive copy of the selected tags
     */
    public Set<TrackTag> getSelectedTags() {
        return new HashSet<>(selectedTags);
    }

    /**
     * Returns the mode used to combine the selected tags.
     *
     * @return the tag combination mode
     */
    public TagCombinationMode getMode() {
        return mode;
    }
}