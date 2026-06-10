package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test suite validating the cascading effects of track deletion.
 * Verifies that when a track is deleted from the global library, it is automatically
 * removed from all playlists containing it.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class TrackDeletionIntegrationTest {

    private MusicPlayerFacade facade;
    private Track track;

    /**
     * Sets up the integration testing environment before each test case.
     * Resets the library and playlists, adds a sample track to the library,
     * and maps it into a newly created test playlist.
     */
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

        Playlist playlist = facade.createPlaylist("My Playlist");
        List<Track> list = new ArrayList<>();
        list.add(track);
        facade.getPlaylistService().addTracksToPlaylist(playlist, list);
    }

    /**
     * Test scenarios evaluating the cascading effects of a confirmed track deletion
     * on both the global library and all associated playlists.
     */
    @Nested
    class WhenDeletingTrack {

        /**
         * Verifies that removing a track from the central library successfully cascades
         * and purges it from any associated user playlists.
         */
        @Test
        public void trackDeletionCascadesToPlaylists() {
            Playlist playlist = facade.getPlaylist("My Playlist");

            assertNotNull(facade.getTrackFromLibrary(track.getId()),
                    "Pre-condition: track must be present in the library before deletion");
            assertTrue(playlist.getPlaylist().contains(track),
                    "Pre-condition: track must be present in the playlist before deletion");

            facade.removeTrackFromLibrary(track);

            assertNull(facade.getTrackFromLibrary(track.getId()),
                    "Track must be removed from the library after deletion");
            assertFalse(playlist.getPlaylist().contains(track),
                    "Track must be removed from the playlist after deletion");
        }
    }
}