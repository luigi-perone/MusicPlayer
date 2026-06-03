package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class TrackDeletionIntegrationTest {

    private MusicPlayerFacade facade;
    private Track track;

    @BeforeEach
    public void setUp() {
        facade = MusicPlayerFacade.getInstance();

        facade.getPlaylistService().getPlaylists().clear();
        facade.getTracksFromLibrary().clear();

        facade.addNewTrackToLibrary("Song To Delete", "Test Author", 200, "Pop", Year.of(2021));

        track = facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals("Song To Delete"))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);

        facade.createPlaylist("My Playlist");
        List<Track> list = new ArrayList<>();
        list.add(track);
        facade.getPlaylistService().addTracksToPlaylist("My Playlist", list);
    }

    @Test
    public void testTrackDeletionCascadesToPlaylists() {
        Playlist playlist = facade.getPlaylist("My Playlist");

        assertNotNull(facade.getTrackFromLibrary(track.getId()));
        assertTrue(playlist.getPlaylist().contains(track));

        facade.removeTrackFromLibrary(track);

        assertNull(facade.getTrackFromLibrary(track.getId()), "La traccia deve essere rimossa dalla libreria");
        assertFalse(playlist.getPlaylist().contains(track), "La traccia deve essere rimossa dalla playlist");
    }
}