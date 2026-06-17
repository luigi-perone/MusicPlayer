package it.unisa.gruppo7.musicplayer.playlist.strategy;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the filtering behavior of TagGenerationStrategy.
 * Verifies tag combination modes, invalid selections, and matching
 * behavior for tracks with or without tags.
 *
 * @author Matteo Postiglione
 */

class TagGenerationStrategyTest {
    private Track favourite;
    private Track favouriteExplicit;
    private Track withoutTags;

    /** Creates sample tracks with and without tags before each test. */
    @BeforeEach
    void setUp() {
        favourite = track("Favourite");
        favourite.addTag(TrackTag.FAVOURITE);

        favouriteExplicit = track("Favourite explicit");
        favouriteExplicit.addTag(TrackTag.FAVOURITE);
        favouriteExplicit.addTag(TrackTag.EXPLICIT);

        withoutTags = track("Without tags");
    }

    /** Verifies that ALL returns only tracks containing every selected tag. */
    @Test
    void allReturnsOnlyTracksContainingEverySelectedTag() {
        TagGenerationStrategy strategy = new TagGenerationStrategy(
                EnumSet.of(TrackTag.FAVOURITE, TrackTag.EXPLICIT),
                TagCombinationMode.ALL);

        assertEquals(
                Collections.singletonList(favouriteExplicit),
                strategy.generate(allTracks()));
    }

    /** Verifies that ALL accepts tracks containing additional non-required tags. */
    @Test
    void allAllowsTracksContainingAdditionalTags() {
        TagGenerationStrategy strategy = new TagGenerationStrategy(
                EnumSet.of(TrackTag.FAVOURITE),
                TagCombinationMode.ALL);

        assertEquals(
                Arrays.asList(favourite, favouriteExplicit),
                strategy.generate(allTracks()));
    }

    /** Verifies that ANY returns tracks containing at least one selected tag. */
    @Test
    void anyReturnsTracksContainingAtLeastOneSelectedTag() {
        TagGenerationStrategy strategy = new TagGenerationStrategy(
                EnumSet.of(TrackTag.EXPLICIT, TrackTag.NEW_RELEASE),
                TagCombinationMode.ANY);

        assertEquals(
                Collections.singletonList(favouriteExplicit),
                strategy.generate(allTracks()));
    }

    /** Verifies that tracks without tags do not match the selected criteria. */
    @Test
    void tracksWithoutTagsDoNotMatch() {
        TagGenerationStrategy strategy = new TagGenerationStrategy(
                EnumSet.of(TrackTag.FAVOURITE),
                TagCombinationMode.ANY);

        assertEquals(
                Collections.emptyList(),
                strategy.generate(Collections.singletonList(withoutTags)));
    }

    /** Verifies that an empty tag selection is rejected with the expected message. */
    @Test
    void emptySelectionIsRejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new TagGenerationStrategy(
                        Collections.emptySet(), TagCombinationMode.ALL));

        assertEquals("Seleziona almeno un tag", exception.getMessage());
    }

    /** Verifies that a null tag selection is rejected. */
    @Test
    void nullSelectionIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TagGenerationStrategy(null, TagCombinationMode.ALL));
    }

    /** Returns all sample tracks used by the tests. */
    private List<Track> allTracks() {
        return Arrays.asList(favourite, favouriteExplicit, withoutTags);
    }

    /** Creates a Pop track with the given title and a default artist and duration. */
    private Track track(String title) {
        return new Track(title, "Artist", 180, "Pop");
    }

    /** Verifies that a selection matching no track produces an empty result. */
    @Test
    void noMatchingTracksReturnsEmptyResult() {
        TagGenerationStrategy strategy = new TagGenerationStrategy(
                EnumSet.of(TrackTag.NEW_RELEASE),
                TagCombinationMode.ALL);

        assertEquals(Collections.emptyList(), strategy.generate(allTracks()));
    }
}
