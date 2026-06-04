package it.unisa.gruppo7.musicplayer.playlist;
import it.unisa.gruppo7.musicplayer.core.PersistenceService;
import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;

class PlaylistServiceTest {
    
    @TempDir
    Path tempDir;
    private PlaylistService service;

    @BeforeEach
    void setUp() {
        Path tempFile = tempDir.resolve("isolated-test-playlists.json");
        service = new PlaylistService(tempFile.toString());
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
        void throwsExceptionForEmptyString() {
            assertThrows(IllegalArgumentException.class, () -> {
                service.createPlaylist("");
            });
        }

        @Test
        void throwsExceptionForNullName() {
            assertThrows(IllegalArgumentException.class, () -> {
                service.createPlaylist(null);
            });
        }

        @Test
        void throwsExceptionForWhitespaceName() {
            assertThrows(IllegalArgumentException.class, () -> {
                service.createPlaylist("   ");
            });
        }

        @Test
        void playlistIsNotAdded() {
            try {
                service.createPlaylist("");
            } catch (IllegalArgumentException ignored) {
            }
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
        void throwsExceptionForDuplicateName() {
            assertThrows(IllegalArgumentException.class, () -> {
                service.createPlaylist("My Playlist");
            });
        }

        @Test
        void duplicateIsNotAdded() {
            try {
                service.createPlaylist("My Playlist");
            } catch (IllegalArgumentException ignored) {
            }
            assertEquals(1, service.getPlaylists().size());
        }
    }

    @Nested
    class DeletePlaylist {

        @BeforeEach
        void existingPlaylist() {
            service.createPlaylist("My Playlist");
            service.createPlaylist("Another Playlist");
        }

        @Nested
        class WithValidName {

            @Test
            void returnsEmptyOptional() {
                Optional<String> error = service.deletePlaylist("My Playlist");
                assertFalse(error.isPresent());
            }

            @Test
            void playlistIsRemovedFromCollection() {
                service.deletePlaylist("My Playlist");
                assertEquals(1, service.getPlaylists().size());
            }

            @Test
            void correctPlaylistIsRemoved() {
                service.deletePlaylist("My Playlist");
                assertEquals("Another Playlist", service.getPlaylists().get(0).getName());
            }

            @Test
            void deletingLastPlaylistLeavesEmptyCollection() {
                service.deletePlaylist("My Playlist");
                service.deletePlaylist("Another Playlist");
                assertEquals(0, service.getPlaylists().size());
            }
        }

        @Nested
        class TracksIntegrity {

            @Test
            void deletingPlaylistDoesNotRemoveTracksSharedWithOtherPlaylists() {
                service.createPlaylist("Playlist A");
                service.createPlaylist("Playlist B");
                Track track=new Track("Track1", "pippo", 180, "rock");
                service.getPlaylist("Playlist A").addTrack(track);
                service.getPlaylist("Playlist B").addTrack(track);

                service.deletePlaylist("Playlist A");


                Collection<Track> libraryTracks = service.getPlaylist("Playlist B").getTracks();
                assertTrue(libraryTracks.contains(track));
            }

            @Test
            void deletingPopulatedPlaylistKeepsTracksInLibrary() {
                service.createPlaylist("Playlist");
                Track track = new Track("Track1", "pippo", 180, "rock");


                Library.getInstance().addTrack(track);
                service.getPlaylist("Playlist").addTrack(track);

                service.deletePlaylist("Playlist");


                assertTrue(Library.getInstance().getTracks().contains(track));
            }
        }

        @Nested
        class WithInvalidName {

            @Test
            void returnsErrorForNullName() {
                Optional<String> error = service.deletePlaylist(null);
                assertTrue(error.isPresent());
            }

            @Test
            void returnsErrorForEmptyName() {
                Optional<String> error = service.deletePlaylist("");
                assertTrue(error.isPresent());
            }

            @Test
            void returnsErrorForWhitespaceName() {
                Optional<String> error = service.deletePlaylist("   ");
                assertTrue(error.isPresent());
            }

            @Test
            void collectionIsUnchangedForNullName() {
                service.deletePlaylist(null);
                assertEquals(2, service.getPlaylists().size());
            }

            @Test
            void collectionIsUnchangedForEmptyName() {
                service.deletePlaylist("");
                assertEquals(2, service.getPlaylists().size());
            }
        }

        @Nested
        class WithNonExistentName {

            @Test
            void returnsError() {
                Optional<String> error = service.deletePlaylist("Ghost Playlist");
                assertTrue(error.isPresent());
            }

            @Test
            void collectionIsUnchanged() {
                service.deletePlaylist("Ghost Playlist");
                assertEquals(2, service.getPlaylists().size());
            }
        }

        @Nested
        class Persistence {

            @TempDir
            Path tempDir;

            private PlaylistService persistenceService;
            private Path tempFile;

            @BeforeEach
            void setUpTempFile() {
                tempFile = tempDir.resolve("test-playlists.json");
                persistenceService = new PlaylistService(tempFile.toString());
                persistenceService.createPlaylist("To Keep");
                persistenceService.createPlaylist("To Delete");
                persistenceService.save();
            }

            @Test
            void deletedPlaylistIsNotRestoredAfterReload() {
                persistenceService.deletePlaylist("To Delete");

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals(1, reloaded.getPlaylists().size());
                assertEquals("To Keep", reloaded.getPlaylists().get(0).getName());
            }

            @Test
            void survivingPlaylistIsStillPresentAfterReload() {
                persistenceService.deletePlaylist("To Delete");

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals("To Keep", reloaded.getPlaylists().get(0).getName());
            }
        }
    }

    @Nested
    class RenamePlaylist {

        @BeforeEach
        void existingPlaylists() {
            service.createPlaylist("Old Name");
            service.createPlaylist("Another Playlist");
        }

        @Test
        void successfulRename() {
            Optional<String> error = service.renamePlaylist("Old Name", "New Name");
            assertFalse(error.isPresent());
            assertNull(service.getPlaylist("Old Name"));
            assertNotNull(service.getPlaylist("New Name"));
        }

        @Test
        void returnsErrorForNullNewName() {
            Optional<String> error = service.renamePlaylist("Old Name", null);
            assertTrue(error.isPresent());
            assertNotNull(service.getPlaylist("Old Name"));
        }

        @Test
        void returnsErrorForEmptyNewName() {
            Optional<String> error = service.renamePlaylist("Old Name", "   ");
            assertTrue(error.isPresent());
            assertNotNull(service.getPlaylist("Old Name"));
        }

        @Test
        void returnsErrorIfOldPlaylistNotFound() {
            Optional<String> error = service.renamePlaylist("Non Existent", "New Name");
            assertTrue(error.isPresent());
        }

        @Test
        void returnsEmptyOptionalIfNamesAreIdentical() {
            Optional<String> error = service.renamePlaylist("Old Name", "Old Name");
            assertFalse(error.isPresent());
        }

        @Test
        void returnsErrorIfNewNameAlreadyExists() {
            Optional<String> error = service.renamePlaylist("Old Name", "Another Playlist");
            assertTrue(error.isPresent());
            assertNotNull(service.getPlaylist("Old Name"));
        }

        @Nested
        class Persistence {

            @TempDir
            Path tempDir;

            private PlaylistService persistenceService;
            private Path tempFile;

            @BeforeEach
            void setUpTempFile() {
                tempFile = tempDir.resolve("test-rename-playlists.json");
                persistenceService = new PlaylistService(tempFile.toString());
                persistenceService.createPlaylist("Old Name");
                persistenceService.save();
            }

            @Test
            void renamedPlaylistIsRestoredAfterReload() {
                persistenceService.renamePlaylist("Old Name", "New Name");

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals(1, reloaded.getPlaylists().size());
                assertEquals("New Name", reloaded.getPlaylists().get(0).getName());
                assertNull(reloaded.getPlaylist("Old Name"));
            }
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

    @Nested
    class RemoveTrackFromPlaylist {

        private Track sharedTrack;

        @BeforeEach
        void setUp() {
            service.createPlaylist("Playlist A");
            service.createPlaylist("Playlist B");
            
            sharedTrack = new Track("Stayin' Alive", "Bee Gees", 285, "Disco");
            
            try {
                Library.getInstance().addTrack(sharedTrack);
            } catch (IllegalArgumentException e) {
            }
            
            service.getPlaylist("Playlist A").addTrack(sharedTrack);
            service.getPlaylist("Playlist B").addTrack(sharedTrack);
        }

        @Test
        void removingTrackFromOnePlaylistKeepsItInLibrary() {
            service.getPlaylist("Playlist A").removeTrack(sharedTrack);
            service.save();

            assertFalse(service.getPlaylist("Playlist A").getTracks().contains(sharedTrack), 
                    "Track should've been removed from A");

            assertTrue(Library.getInstance().getTracks().contains(sharedTrack), 
                    "Tracks must be available in the library");
        }

        @Test
        void removingTrackFromOnePlaylistKeepsItInOtherPlaylists() {
            service.getPlaylist("Playlist A").removeTrack(sharedTrack);
            service.save();

            assertTrue(service.getPlaylist("Playlist B").getTracks().contains(sharedTrack), 
                    "Track should be available in Playlist B");
        }
    }
}
