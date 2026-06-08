package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test suite validating track modification propagation and validation boundaries.
 * Verifies that valid track metadata changes correctly reflect inside playlists,
 * and invalid modifications are strictly rejected.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class TrackModificationIntegrationTest {

    private Library library;
    private PlaylistService playlistService;
    private Track track;
    private final String TEST_PLAYLIST_PATH = "data/test-playlist.json";

    /**
     * Resets the data collections and inserts a baseline track into the library
     * before each test execution.
     */
    @BeforeEach
    public void setUp() {
        library = Library.getInstance();
        library.clearLibrary();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        playlistService.getPlaylists().clear();

        track = new Track("Original Title", "Original Author", 200, "Pop", Year.of(2020));
        library.addTrack(track);
    }

    /**
     * Verifies that a valid metadata modification updates the track fields
     * and that the changes are visible through references inside playlists.
     */
    @Test
    public void testValidModificationPropagatesToPlaylist() {
        Playlist playlist = playlistService.createPlaylist("Test Playlist");
        playlistService.addTracksToPlaylist(playlist, Collections.singletonList(track));

        library.modifyTrackInLibrary(track, "New Title", "New Author", 250, "Rock", Year.of(2022));

        Track trackInPlaylist = playlist.getPlaylist().get(0);

        assertEquals("New Title", trackInPlaylist.getTitle());
        assertEquals("New Author", trackInPlaylist.getAuthor());
        assertEquals(250, trackInPlaylist.getDuration());
    }

    /**
     * Assures that attempting to modify a track with an empty title is blocked
     * by an {@link IllegalArgumentException} and the original metadata is preserved.
     */
    @Test
    public void testModificationWithEmptyTitleIsBlocked() {
        assertThrows(IllegalArgumentException.class, () -> {
            library.modifyTrackInLibrary(track, "", "New Author", 200, "Pop", Year.of(2020));
        });

        assertEquals("Original Title", track.getTitle());
    }

    /**
     * Assures that modifying a track with a publication year set in the future is blocked
     * and leaves the original track state untouched.
     */
    @Test
    public void testModificationWithFutureYearIsBlocked() {
        Year futureYear = Year.now().plusYears(1);

        assertThrows(IllegalArgumentException.class, () -> {
            library.modifyTrackInLibrary(track, "New Title", "Original Author", 200, "Pop", futureYear);
        });

        assertEquals(Year.of(2020), track.getPublicationYear());
    }
}