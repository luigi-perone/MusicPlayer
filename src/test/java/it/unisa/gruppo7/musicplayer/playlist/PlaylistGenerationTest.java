package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.strategy.GenerationCriterion;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;
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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for automatic playlist generation through {@link MusicPlayerFacade},
 * covering generation by genre and by year, including the failure case when no
 * track matches the rule. Real production data files are backed up and restored
 * around each test so the suite never corrupts the user's library.
 */
public class PlaylistGenerationTest {

    private MusicPlayerFacade facade;

    private static final Path REAL_TRACK_FILE = Paths.get("data/track-library.json");
    private static final Path REAL_PLAYLIST_FILE = Paths.get("data/playlist.json");

    private static final Path BACKUP_TRACK_FILE = Paths.get("data/track-library.json.bak");
    private static final Path BACKUP_PLAYLIST_FILE = Paths.get("data/playlist.json.bak");

    /** Backs up the production data files and resets the facade to a clean state. */
    @BeforeEach
    void setUp() throws IOException {
        if (Files.exists(REAL_TRACK_FILE)) {
            Files.copy(REAL_TRACK_FILE, BACKUP_TRACK_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(REAL_PLAYLIST_FILE)) {
            Files.copy(REAL_PLAYLIST_FILE, BACKUP_PLAYLIST_FILE, StandardCopyOption.REPLACE_EXISTING);
        }

        facade = new MusicPlayerFacade();
        facade.clearLibrary();

        List<Playlist> existingPlaylists = new ArrayList<>(facade.getPlaylists());
        for (Playlist p : existingPlaylists) {
            facade.deletePlaylist(p);
        }
    }

    /** Flushes background IO, clears in-memory data and restores the production files. */
    @AfterEach
    void tearDown() throws IOException {
        try {
            Field executorField = MusicPlayerFacade.class.getDeclaredField("ioExecutor");
            executorField.setAccessible(true);
            ExecutorService executor = (ExecutorService) executorField.get(facade);
            if (executor != null && !executor.isShutdown()) {
                executor.submit(() -> {}).get();
            }
        } catch (Exception e) {
            System.err.println("Could not flush background IO Executor: " + e.getMessage());
        }

        facade.clearLibrary();

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

    /** Tests covering automatic playlist generation by genre. */
    @Nested
    class GenrePlaylistGeneration {

        /** Verifies that generating by genre filters the matching tracks into a new playlist. */
        @Test
        void testCreateByGenre_FiltersCorrectlyAndGeneratesPlaylist() {
            // Arrange
            facade.addNewTrackToLibrary("Rock Track 1", "Author A", 120, "Rock", Year.of(2015));
            facade.addNewTrackToLibrary("Rock Track 2", "Author B", 180, "Rock", Year.of(2018));
            facade.addNewTrackToLibrary("Pop Track 1", "Author C", 150, "Pop", Year.of(2020));

            PlaylistGenerationStrategy genreStrategy = tracks -> tracks.stream()
                    .filter(t -> "Rock".equalsIgnoreCase(t.getGenre()))
                    .collect(Collectors.toList());

            // Create a valid rule instance (adjust parameters if your constructor is different)
            AutomaticPlaylistRule rockRule = new AutomaticPlaylistRule();
            rockRule.criterion = GenerationCriterion.GENRE;
            rockRule.target = "Rock";

            // Act
            Playlist generatedPlaylist = facade.createAutoPlaylist("Best of Rock", genreStrategy, rockRule);

            // Assert
            assertNotNull(generatedPlaylist, "The generated playlist must not be null");
            assertEquals("Best of Rock", generatedPlaylist.getName(), "The playlist name must match the input");

            List<Track> playlistTracks = (List<Track>) generatedPlaylist.getTracks();
            assertEquals(2, playlistTracks.size(), "The playlist must contain exactly the 2 Rock tracks");

            for (Track track : playlistTracks) {
                assertEquals("Rock", track.getGenre(), "Every track in the playlist must have the 'Rock' genre");
            }
        }

        /** Verifies that generating by genre with no matching tracks throws and creates no playlist. */
        @Test
        void testCreateByGenre_ThrowsExceptionWhenNoTracksFound() {
            // Arrange
            facade.addNewTrackToLibrary("Pop Track 1", "Author C", 150, "Pop", Year.of(2020));
            int initialPlaylistCount = facade.getPlaylists().size();

            PlaylistGenerationStrategy genreStrategy = tracks -> tracks.stream()
                    .filter(t -> "Classical".equalsIgnoreCase(t.getGenre()))
                    .collect(Collectors.toList());

            // Create a valid rule instance
            AutomaticPlaylistRule classicalRule = new AutomaticPlaylistRule();
            classicalRule.criterion = GenerationCriterion.GENRE;
            classicalRule.target = "Classical";

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                facade.createAutoPlaylist("Classical Hits", genreStrategy, classicalRule);
            });

            assertEquals("Nessuna traccia trovata", exception.getMessage(), "It must throw the specific error message");
            assertEquals(initialPlaylistCount, facade.getPlaylists().size(), "No playlist should be created if the operation fails");
        }
    }

    /** Tests covering automatic playlist generation by publication year. */
    @Nested
    class YearPlaylistGeneration {

        /** Verifies that generating by year filters matching tracks and sorts them alphabetically. */
        @Test
        void testCreateByYear_FiltersCorrectlyAndSortsAlphabetically() {
            // Arrange
            Year targetYear = Year.of(2010);

            // Inserted out of alphabetical order to test the sorting capability
            facade.addNewTrackToLibrary("Zebra Track", "Author A", 120, "Rock", targetYear);
            facade.addNewTrackToLibrary("Alpha Track", "Author B", 180, "Pop", targetYear);
            facade.addNewTrackToLibrary("Beta Track", "Author C", 150, "Jazz", targetYear);

            // Track from a different year to ensure it gets filtered out
            facade.addNewTrackToLibrary("Noise Track", "Author D", 200, "Rock", Year.of(2015));

            PlaylistGenerationStrategy yearStrategy = tracks -> tracks.stream()
                    .filter(t -> targetYear.equals(t.getPublicationYear()))
                    .collect(Collectors.toList());

            // Create a valid rule instance
            AutomaticPlaylistRule yearRule = new AutomaticPlaylistRule();
            yearRule.criterion = GenerationCriterion.YEAR;
            yearRule.target = "2010";

            // Act
            Playlist generatedPlaylist = facade.createAutoPlaylist("2010 Hits", yearStrategy, yearRule);

            // Assert
            assertNotNull(generatedPlaylist, "The generated playlist must not be null");
            assertEquals("2010 Hits", generatedPlaylist.getName(), "The playlist name must match the input");

            List<Track> playlistTracks = (List<Track>) generatedPlaylist.getTracks();
            assertEquals(3, playlistTracks.size(), "The playlist must contain exactly the 3 tracks from 2010");

            // Verify Alphabetical Sorting
            assertEquals("Alpha Track", playlistTracks.get(0).getTitle(), "First track must be sorted alphabetically (A)");
            assertEquals("Beta Track", playlistTracks.get(1).getTitle(), "Second track must be sorted alphabetically (B)");
            assertEquals("Zebra Track", playlistTracks.get(2).getTitle(), "Third track must be sorted alphabetically (Z)");
        }

        /** Verifies that generating by year with no matching tracks throws and creates no playlist. */
        @Test
        void testCreateByYear_ThrowsExceptionWhenNoTracksFound() {
            // Arrange
            facade.addNewTrackToLibrary("Modern Track", "Author", 150, "Pop", Year.of(2023));
            int initialPlaylistCount = facade.getPlaylists().size();

            PlaylistGenerationStrategy yearStrategy = tracks -> tracks.stream()
                    .filter(t -> Year.of(1980).equals(t.getPublicationYear()))
                    .collect(Collectors.toList());

            // Create a valid rule instance
            AutomaticPlaylistRule yearRule = new AutomaticPlaylistRule();
            yearRule.criterion = GenerationCriterion.YEAR;
            yearRule.target = "1980";

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                facade.createAutoPlaylist("1980 Hits", yearStrategy, yearRule);
            });

            assertEquals("Nessuna traccia trovata", exception.getMessage(), "It must throw the specific error message");
            assertEquals(initialPlaylistCount, facade.getPlaylists().size(), "No playlist should be created if the operation fails");
        }
    }
}