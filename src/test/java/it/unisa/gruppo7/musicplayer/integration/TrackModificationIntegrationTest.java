package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

public class TrackModificationIntegrationTest {

    private Library library;
    private PlaylistService playlistService;
    private Track track;
    private final String TEST_PLAYLIST_PATH = "data/test-playlist.json";

    @BeforeEach
    public void setUp() {
        library = Library.getInstance();
        library.clearLibrary();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        playlistService.getPlaylists().clear();

        track = new Track("Original Title", "Original Author", 200, "Pop", Year.of(2020));
        library.addTrack(track);
    }

    @Test
    public void testValidModificationPropagatesToPlaylist() {
        playlistService.createPlaylist("Test Playlist");
        playlistService.addTracksToPlaylist("Test Playlist", java.util.Collections.singletonList(track));

        library.modifyTrackInLibrary(track, "New Title", "New Author", 250, "Rock", Year.of(2022));

        Playlist playlist = playlistService.getPlaylist("Test Playlist");
        Track trackInPlaylist = playlist.getPlaylist().get(0);

        assertEquals("New Title", trackInPlaylist.getTitle());
        assertEquals("New Author", trackInPlaylist.getAuthor());
        assertEquals(250, trackInPlaylist.getDuration());
    }

    @Test
    public void testModificationWithEmptyTitleIsBlocked() {
        assertThrows(IllegalArgumentException.class, () -> {
            library.modifyTrackInLibrary(track, "", "New Author", 200, "Pop", Year.of(2020));
        });

        assertEquals("Original Title", track.getTitle());
    }

    @Test
    public void testModificationWithFutureYearIsBlocked() {
        Year futureYear = Year.now().plusYears(1);

        assertThrows(IllegalArgumentException.class, () -> {
            library.modifyTrackInLibrary(track, "New Title", "Original Author", 200, "Pop", futureYear);
        });

        assertEquals(Year.of(2020), track.getPublicationYear());
    }
}