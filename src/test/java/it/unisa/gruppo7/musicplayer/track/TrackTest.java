package it.unisa.gruppo7.musicplayer.track;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}