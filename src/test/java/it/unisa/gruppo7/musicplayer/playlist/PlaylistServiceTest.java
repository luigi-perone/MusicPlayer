package it.unisa.gruppo7.musicplayer.playlist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class PlaylistServiceTest {

    private PlaylistService service;

    @BeforeEach
    void setUp() {
        service = new PlaylistService();
    }

    @Nested
    class WithValidName {

        /**
        @Test
        void returnsEmptyOptional() {
            Optional<String> error = service.createPlaylist("My Playlist");
            assertTrue(error.isEmpty());
        }*/

        @Test
        void playlistIsAddedToCollection() {
            service.createPlaylist("My Playlist");
            assertEquals(1, service.getPlaylists().size());
        }

        @Test
        void playlistHasGivenName() {
            service.createPlaylist("My Playlist");
            assertEquals("My Playlist", service.getPlaylists().get(0).getName());
        }
    }

    @Nested
    class WithEmptyName {

        @Test
        void returnsError() {
            Optional<String> error = service.createPlaylist("");
            assertTrue(error.isPresent());
        }

        @Test
        void returnsErrorForNullName() {
            Optional<String> error = service.createPlaylist(null);
            assertTrue(error.isPresent());
        }

        @Test
        void returnsErrorForWhitespaceName() {
            Optional<String> error = service.createPlaylist("   ");
            assertTrue(error.isPresent());
        }

        @Test
        void playlistIsNotAdded() {
            service.createPlaylist("");
            assertEquals(0, service.getPlaylists().size());
        }
    }

    @Nested
    class WithDuplicateName {

        @BeforeEach
        void existingPlaylist() {
            service.createPlaylist("My Playlist");
        }

        @Test
        void returnsError() {
            Optional<String> error = service.createPlaylist("My Playlist");
            assertTrue(error.isPresent());
        }

        @Test
        void duplicateIsNotAdded() {
            service.createPlaylist("My Playlist");
            assertEquals(1, service.getPlaylists().size());
        }
    }

    @Nested
    class Persistence {
        // Creates a temporary directory which will be deleted after the test is done.
        @TempDir
        Path tempDir;

        private PlaylistService persistenceService;
        private Path tempFile;

        @BeforeEach
        void setUpTempFile() {
            tempFile = tempDir.resolve("test-playlists.json");
            persistenceService = new PlaylistService(tempFile.toString());
        }

        @Test
        void saveWritesFileToDisk() {
            persistenceService.createPlaylist("My Playlist");
            persistenceService.save();
            assertTrue(Files.exists(tempFile));
        }

        /**
        @Test
        void saveWritesPlaylistName() throws IOException {
            persistenceService.createPlaylist("My Playlist");
            persistenceService.save();
            String content = Files.readString(tempFile);
            assertTrue(content.contains("My Playlist"));
        }

        @Test
        void saveWritesMultiplePlaylists() throws IOException {
            persistenceService.createPlaylist("Playlist A");
            persistenceService.createPlaylist("Playlist B");
            persistenceService.save();
            String content = Files.readString(tempFile);
            assertTrue(content.contains("Playlist A"));
            assertTrue(content.contains("Playlist B"));
        }*/

        @Test
        void loadDoesNothingIfFileDoesNotExist() {
            Path nonExisting = tempDir.resolve("does-not-exist.json");
            PlaylistService s = new PlaylistService(nonExisting.toString());
            s.load();
            assertEquals(0, s.getPlaylists().size());
        }

        /**
        @Test
        void saveActuallyWritesContent() throws IOException {
            persistenceService.createPlaylist("Test");
            persistenceService.save();
            String content = Files.readString(tempFile);
            System.out.println("FILE CONTENT: " + content);
            assertTrue(content.contains("Test"));
        }*/

        @Test
        void savePlaylistIsRestoredAfterReload() {
            persistenceService.createPlaylist("Test");
            persistenceService.save();

            PlaylistService reloaded = new PlaylistService(tempFile.toString());
            reloaded.load();

            assertEquals(1, reloaded.getPlaylists().size());
            assertEquals("Test", reloaded.getPlaylists().get(0).getName());
        }
    }
}
