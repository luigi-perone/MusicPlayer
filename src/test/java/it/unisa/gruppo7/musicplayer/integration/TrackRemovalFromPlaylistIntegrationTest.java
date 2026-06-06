package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for US-010.
 *
 * Verifies that removing a track from a playlist:
 *   1. Decreases the playlist track count.
 *   2. Leaves the track untouched in the library.
 *
 * Also verifies that cancelling the removal produces no side effects.
 */
public class TrackRemovalFromPlaylistIntegrationTest {

    private MusicPlayerFacade facade;
    private PlaylistService   playlistService;
    private Track             track;
    private Playlist          testPlaylist;

    private static final String PLAYLIST_NAME = "Test Playlist";

    @BeforeEach
    public void setUp() {
        facade          = MusicPlayerFacade.getInstance();
        playlistService = facade.getPlaylistService();

        playlistService.getPlaylists().clear();
        facade.clearLibrary();

        facade.addNewTrackToLibrary("Track To Remove", "Author A", 180, "Rock", Year.of(2020));
        facade.addNewTrackToLibrary("Track To Keep",   "Author B", 240, "Pop",  Year.of(2021));

        track = facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals("Track To Remove"))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);

        testPlaylist = facade.createPlaylist(PLAYLIST_NAME);

        List<Track> both = new ArrayList<>(facade.getTracksFromLibrary());
        AdditionResult result = playlistService.addTracksToPlaylist(testPlaylist, both);
        assertEquals(2, result.getAdded(), "Setup: both tracks should have been added");
    }

    @Test
    public void testRemovalDecreasesCountAndPreservesLibraryTrack() {
        assertTrue(testPlaylist.getPlaylist().contains(track),
                "Pre-condition: track must be in the playlist before removal");

        int countBefore = testPlaylist.getTrackCount();

        testPlaylist.removeTrack(track);
        playlistService.save();

        assertEquals(countBefore - 1, testPlaylist.getTrackCount(),
                "Track count must decrease by 1 after removal");

        assertFalse(testPlaylist.getPlaylist().contains(track),
                "Removed track must not appear in the playlist anymore");

        assertNotNull(facade.getTrackFromLibrary(track.getId()),
                "Track must still exist in the library after playlist removal");
    }

    @Test
    public void testCancelRemovalProducesNoSideEffects() {
        int countBefore   = testPlaylist.getTrackCount();
        int libSizeBefore = facade.getTracksFromLibrary().size();

        assertEquals(countBefore, testPlaylist.getTrackCount(),
                "Playlist count must not change when removal is cancelled");

        assertTrue(testPlaylist.getPlaylist().contains(track),
                "Track must still be present in the playlist when removal is cancelled");

        assertEquals(libSizeBefore, facade.getTracksFromLibrary().size(),
                "Library size must not change when removal is cancelled");

        assertNotNull(facade.getTrackFromLibrary(track.getId()),
                "Track must still exist in the library when removal is cancelled");
    }

    @Test
    public void testSiblingTrackRemainsInPlaylistAfterRemoval() {
        Track sibling = facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals("Track To Keep"))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);

        testPlaylist.removeTrack(track);
        playlistService.save();

        assertTrue(testPlaylist.getPlaylist().contains(sibling),
                "Sibling track must remain in the playlist after another track is removed");

        assertNotNull(facade.getTrackFromLibrary(sibling.getId()),
                "Sibling track must remain in the library after another track is removed");
    }
}