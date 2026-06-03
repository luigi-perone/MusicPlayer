package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryTest {

    private Library library;

    @BeforeEach
    public void setUp() {
        library = Library.getInstance();
    }

    @Test
    public void testAddValidTrack() {
        String uniqueTitle = "Test Song " + UUID.randomUUID();
        Track validTrack = new Track(uniqueTitle, "Test Author", 210, "Pop", Year.of(2023));

        assertTrue(library.addTrack(validTrack));

        assertNotNull(library.getTrackById(validTrack.getId()));
    }

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