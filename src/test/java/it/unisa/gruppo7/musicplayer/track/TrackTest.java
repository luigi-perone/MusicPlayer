package it.unisa.gruppo7.musicplayer.track;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author francescoLemmo
 */

public class TrackTest {

    @Test
    public void testCorrectTrackCreation() {
        Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(1983));

        assertEquals("Faithfully", track.getTitle());
        assertEquals("Journey", track.getAuthor());
        assertEquals(266, track.getDuration());
        assertEquals("Rock", track.getGenre());
        assertEquals(Year.of(1983), track.getPublicationYear());
    }

    @Test
    public void testNoTitleThrowsException() {
        assertThrows(Exception.class, () -> {
            Track track = new Track("", "Journey", 266, "Rock", Year.of(1983));
        });

        assertThrows(Exception.class, () -> {
            Track track = new Track(null, "Journey", 266, "Rock", Year.of(1983));
        });
    }

    @Test
    public void testWrongYearThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Track track = new Track("Faithfully", "Journey", 266, "Rock", Year.of(2983));
        });
    }

    @Test
    public void testNegativeDurationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            Track track = new Track("Faithfully", "Journey", -12, "Rock", Year.of(1983));
        });
    }
}
