package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TrackDeletionIntegrationTest {

    private Library library;
    private PlaylistService playlistService;
    private Track track;
    private final String TEST_PLAYLIST_PATH = "data/test-playlist-delete.json";

    @BeforeEach
    public void setUp() {
        library = Library.getInstance();
        library.clearLibrary();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        playlistService.getPlaylists().clear();

        track = new Track("Song To Delete", "Test Author", 200, "Pop", Year.of(2021));
        library.addTrack(track);

        List<Track> list = new ArrayList<>();
        list.add(track);

        playlistService.createPlaylist("My Playlist");
        playlistService.addTracksToPlaylist("My Playlist", list);
    }

    @Test
    public void testTrackDeletionCascadesToPlaylists() {
        Playlist playlist = playlistService.getPlaylist("My Playlist");

        assertNotNull(library.getTrackById(track.getId()));
        assertTrue(playlist.getPlaylist().contains(track));

        library.removeTrack(track);

        assertNull(library.getTrackById(track.getId()), "La traccia deve essere rimossa dalla libreria");
        assertFalse(playlist.getPlaylist().contains(track), "La traccia deve essere rimossa dalla playlist");
    }
}