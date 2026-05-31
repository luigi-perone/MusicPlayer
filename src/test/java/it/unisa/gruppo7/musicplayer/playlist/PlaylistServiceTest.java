package test.java.it.unisa.gruppo7.musicplayer.playlist;
import main.java.it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

        @Test
        void returnsEmptyOptional() {
            Optional<String> error = service.createPlaylist("My Playlist");
            assertTrue(error.isEmpty());
        }

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
}
