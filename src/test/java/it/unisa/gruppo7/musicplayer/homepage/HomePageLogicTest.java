package it.unisa.gruppo7.musicplayer.homepage;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the home-page "most played" logic exposed through {@link MusicPlayerFacade}.
 * Covers listening-history checks, ranking/filtering of top tracks and playlists, and
 * the quick-play action. Real production data files are backed up and restored around
 * each test so the suite never corrupts the user's library.
 */
public class HomePageLogicTest {

    private MusicPlayerFacade facade;

    // Real production files paths
    private static final Path REAL_TRACK_FILE = Paths.get("data/track-library.json");
    private static final Path REAL_PLAYLIST_FILE = Paths.get("data/playlist.json");

    // Backup files paths used during test execution
    private static final Path BACKUP_TRACK_FILE = Paths.get("data/track-library.json.bak");
    private static final Path BACKUP_PLAYLIST_FILE = Paths.get("data/playlist.json.bak");

    /** Backs up the production data files and resets the facade to a clean state. */
    @BeforeEach
    void setUp() throws IOException {
        // 1. Physically back up your real production data if files exist
        if (Files.exists(REAL_TRACK_FILE)) {
            Files.copy(REAL_TRACK_FILE, BACKUP_TRACK_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(REAL_PLAYLIST_FILE)) {
            Files.copy(REAL_PLAYLIST_FILE, BACKUP_PLAYLIST_FILE, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Initialize facade and clear the in-memory data for a clean test environment
        facade = MusicPlayerFacade.getInstance();
        facade.clearLibrary();

        List<Playlist> existingPlaylists = new ArrayList<>(facade.getPlaylists());
        for (Playlist p : existingPlaylists) {
            facade.deletePlaylist(p);
        }
        facade.getPlaybackService().pause();
    }

    /** Flushes background IO, clears in-memory data and restores the production files. */
    @AfterEach
    void tearDown() throws IOException {
        // 1. Safety Trick: Flush the background IO Executor using Reflection
        // to prevent async tasks from writing to disk after the restore process
        try {
            Field executorField = MusicPlayerFacade.class.getDeclaredField("ioExecutor");
            executorField.setAccessible(true);
            ExecutorService executor = (ExecutorService) executorField.get(facade);
            if (executor != null && !executor.isShutdown()) {
                // Submitting an empty task and waiting for it guarantees
                // all previous async saving tasks are fully completed
                executor.submit(() -> {}).get();
            }
        } catch (Exception e) {
            System.err.println("Could not flush background IO Executor: " + e.getMessage());
        }

        // 2. Clear in-memory structures
        facade.clearLibrary();

        // 3. Restore original production files from backups
        if (Files.exists(BACKUP_TRACK_FILE)) {
            Files.move(BACKUP_TRACK_FILE, REAL_TRACK_FILE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(REAL_TRACK_FILE);
        }

        if (Files.exists(BACKUP_PLAYLIST_FILE)) {
            Files.move(BACKUP_PLAYLIST_FILE, REAL_PLAYLIST_FILE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(REAL_PLAYLIST_FILE);
        }
    }


    /** Tests covering the listening-history checks. */
    @Nested
    class HistoryCheck {

        /** Verifies that with no listening history both top lists are empty. */
        @Test
        void testGetMostPlayed_WithNoListeningHistory_ReturnsEmptyLists() {
            // Arrange
            facade.addNewTrackToLibrary("New Track 1", "Author", 120, "Pop", null);
            facade.createPlaylist("Unplayed Playlist");

            // Act
            List<Track> topTracks = facade.getMostPlayedTracks(5);
            List<Playlist> topPlaylists = facade.getMostPlayedPlaylists(5);

            // Assert
            assertTrue(topTracks.isEmpty(), "The track list must be empty if there is no listening history");
            assertTrue(topPlaylists.isEmpty(), "The playlist list must be empty if there is no listening history");
        }
    }

    /** Tests covering the ranking and filtering of the most played items. */
    @Nested
    class RankingAndFilters {

        /** Verifies that top tracks are ordered by descending play count and zero-play tracks excluded. */
        @Test
        void testGetMostPlayedTracks_OrdersDescendingAndFiltersZero() {
            // Arrange
            facade.addNewTrackToLibrary("Ignored Track", "Author", 100, "Pop", null);
            facade.addNewTrackToLibrary("Medium Track", "Author", 100, "Pop", null);
            facade.addNewTrackToLibrary("Top Track", "Author", 100, "Pop", null);

            List<Track> allTracks = new ArrayList<>(facade.getTracksFromLibrary());
            Track ignoredTrack = allTracks.get(0);
            Track mediumTrack = allTracks.get(1);
            Track topTrack = allTracks.get(2);

            // Simulate plays
            mediumTrack.incrementPlayCount();
            mediumTrack.incrementPlayCount();
            for(int i = 0; i < 5; i++) topTrack.incrementPlayCount();

            // Act
            List<Track> topTracks = facade.getMostPlayedTracks(10);

            // Assert
            assertEquals(2, topTracks.size(), "It must exclude tracks with 0 plays");
            assertEquals(topTrack, topTracks.get(0), "The track with 5 plays must be first");
            assertEquals(mediumTrack, topTracks.get(1), "The track with 2 plays must be second");
        }

        /** Verifies that the most played playlists respect the requested limit. */
        @Test
        void testGetMostPlayedPlaylists_RespectsLimit() {
            // Arrange
            Playlist p1 = facade.createPlaylist("Rock Classics");
            Playlist p2 = facade.createPlaylist("Jazz Vibes");
            Playlist p3 = facade.createPlaylist("Pop Hits");

            p1.incrementPlayCount();
            for(int i = 0; i < 10; i++) p2.incrementPlayCount();
            for(int i = 0; i < 5; i++) p3.incrementPlayCount();

            // Act
            List<Playlist> topPlaylists = facade.getMostPlayedPlaylists(2);

            // Assert
            assertEquals(2, topPlaylists.size(), "It must limit the result to 2 items");
            assertEquals(p2, topPlaylists.get(0), "Jazz Vibes must be first");
            assertEquals(p3, topPlaylists.get(1), "Pop Hits must be second");
        }
    }

    /** Tests covering the quick-play action triggered from the UI. */
    @Nested
    class QuickStartActions {

        /** Verifies that quick play starts playback of the selected track immediately. */
        @Test
        void testQuickPlay_StartsInstantly() {
            // Arrange
            facade.addNewTrackToLibrary("Track 1", "Author", 120, "Pop", null);
            Track trackToPlay = facade.getTracksFromLibrary().iterator().next();

            // Act
            facade.playTrack(trackToPlay);

            // Assert
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(), "The state must be PLAYING");
            assertEquals(trackToPlay, facade.getCurrentPlayingTrack(), "The loaded track must match the selected one");
        }
    }
}
