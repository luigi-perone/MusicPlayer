package it.unisa.gruppo7.musicplayer.track;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit test suite for the {@link Track} class.
 * Validates track creation constraints, attribute validation rules, and exception handling boundaries.
 *
 * @author Francesco Lemmo
 */
public class TrackTest {

    /**
     * Verifies that a track is correctly initialized when provided with valid parameters.
     */
    @Test
    public void testCorrectTrackCreation() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));

        assertEquals("Faithfully", track.getTitle());
        assertEquals("Journey", track.getAuthor());
        assertEquals(266, track.getDuration());
        assertEquals("Rock", track.getGenre());
        assertEquals(Year.of(1983), track.getPublicationYear());
    }

    /**
     * Verifies that attempting to create a track with an empty or null title throws an exception.
     */
    @Test
    public void testNoTitleThrowsException() {
        assertThrows(Exception.class, () -> {
            Track track = new Track("", "Journey", 266, "Rock", Year.of(1983));
        });

        assertThrows(Exception.class, () -> {
            Track track = new Track(null, "Journey", 266, "Rock", Year.of(1983));
        });
    }

    /**
     * Verifies that providing an invalid or future publication year throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testWrongYearThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(2983));
        });
    }

    /**
     * Verifies that specifying a negative duration value throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testNegativeDurationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Track track = new Track("Faithfully", "Journey", -12, "Rock", Year.of(1983));
        });
    }
    
    /** Verifies that a newly created track has no tags. */
    @Test
    public void newTrackHasNoTags() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));

        assertTrue(track.getTags().isEmpty());
    }

    /** Verifies that adding a tag makes the track report it as present. */
    @Test
    public void addTagAddsTagToTrack() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));

        track.addTag(TrackTag.FAVOURITE);

        assertTrue(track.hasTag(TrackTag.FAVOURITE));
    }

    /** Verifies that removing a tag makes the track no longer report it. */
    @Test
    public void removeTagRemovesTagFromTrack() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));
        track.addTag(TrackTag.EXPLICIT);

        track.removeTag(TrackTag.EXPLICIT);

        assertFalse(track.hasTag(TrackTag.EXPLICIT));
    }

    /** Verifies that hasTag returns true only for tags actually present on the track. */
    @Test
    public void hasTagReturnsTrueOnlyWhenTagIsPresent() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));
        track.addTag(TrackTag.NEW_RELEASE);

        assertTrue(track.hasTag(TrackTag.NEW_RELEASE));
        assertFalse(track.hasTag(TrackTag.FAVOURITE));
    }

    /** Verifies that adding the same tag twice does not create duplicates. */
    @Test
    public void addingSameTagTwiceDoesNotCreateDuplicates() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));

        track.addTag(TrackTag.FAVOURITE);
        track.addTag(TrackTag.FAVOURITE);

        assertEquals(1, track.getTags().size());
    }

    /** Verifies that setting tags to null clears them without throwing. */
    @Test
    public void setTagsWithNullClearsTagsWithoutException() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));
        track.addTag(TrackTag.FAVOURITE);

        assertDoesNotThrow(() -> track.setTags(null));
        assertTrue(track.getTags().isEmpty());
    }

    /** Verifies that setTags replaces the current set of tags entirely. */
    @Test
    public void setTagsReplacesCurrentTags() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));
        Set<TrackTag> tags = new HashSet<>();
        tags.add(TrackTag.EXPLICIT);
        tags.add(TrackTag.NEW_RELEASE);

        track.setTags(tags);

        assertTrue(track.hasTag(TrackTag.EXPLICIT));
        assertTrue(track.hasTag(TrackTag.NEW_RELEASE));
        assertFalse(track.hasTag(TrackTag.FAVOURITE));
    }

}