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

    public TagGenerationStrategy(Set<TrackTag> selectedTags, TagCombinationMode mode) {
        if (selectedTags == null || selectedTags.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un tag");
        }
        this.selectedTags = new HashSet<>(selectedTags);
        this.mode = (mode == null) ? TagCombinationMode.ALL : mode;
    }

    @Override
    public List<Track> generate(Collection<Track> allTracks) {
        return allTracks.stream()
                .filter(track -> track != null && matches(track))
                .collect(Collectors.toList());
    }

    private boolean matches(Track track) {
        if (mode == TagCombinationMode.ALL) {
            return track.getTags().containsAll(selectedTags);
        }
        return selectedTags.stream().anyMatch(track::hasTag);
    }

    public Set<TrackTag> getSelectedTags() {
        return new HashSet<>(selectedTags);
    }

    public TagCombinationMode getMode() {
        return mode;
    }
}