package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for the {@link Library} repository class.
 * Validates track entry additions, duplication signature rules, and constraint enforcement boundaries.
 *
 */
public class LibraryTest {

    private Library library;

    /**
     * Sets up the baseline test environment by retrieving the Singleton Library instance.
     */
    @BeforeEach
    public void setUp() {
        library = Library.getInstance();
    }

    /**
     * Verifies that a valid track can be successfully appended to the library mapping
     * and accurately retrieved by its unique identifier.
     */
    @Test
    public void testAddValidTrack() {
        String uniqueTitle = "Test Song " + UUID.randomUUID();
        Track validTrack = new Track(uniqueTitle, "Test Author", 210, "Pop", Year.of(2023));

        assertTrue(library.addTrack(validTrack));

        assertNotNull(library.getTrackById(validTrack.getId()));
    }

    /**
     * Verifies that trying to insert a track with an identical case-insensitive title and author signature
     * triggers an {@link IllegalArgumentException}.
     */
    @Test
    public void testAddDuplicateTrackThrowsException() {
        String uniqueTitle = "Duplicate Song " + UUID.randomUUID();
        Track track1 = new Track(uniqueTitle, "Duplicate Author", 200);

        library.addTrack(track1);

        Track duplicateTrack = new Track(uniqueTitle, "Duplicate Author", 180);

        assertThrows(IllegalArgumentException.class, () -> {
            library.addTrack(duplicateTrack);
        });
    }

    /**
     * Assures that track instances violating core domain validation rules (such as blank titles
     * or negative durations) cannot be registered into the library tracking records.
     */
    @Test
    public void testInvalidTrackCannotBeAdded() {
        assertThrows(IllegalArgumentException.class, () -> {
            Track invalidTrack = new Track("", "Author", 200);
            library.addTrack(invalidTrack);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Track invalidTrack = new Track("Title", "Author", -5);
            library.addTrack(invalidTrack);
        });
    }
}