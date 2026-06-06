package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.track.Track;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist(""));
        }

        @Test
        void throwsExceptionForNullName() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist(null));
        }

        @Test
        void throwsExceptionForWhitespaceName() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist("   "));
        }

        @Test
        void playlistIsNotAdded() {
            try { service.createPlaylist(""); } catch (IllegalArgumentException ignored) {}
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
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist("My Playlist"));
        }

        @Test
        void duplicateIsNotAdded() {
            try { service.createPlaylist("My Playlist"); } catch (IllegalArgumentException ignored) {}
            assertEquals(1, service.getPlaylists().size());
        }
    }

    @Nested
    class DeletePlaylist {

        private Playlist myPlaylist;
        private Playlist anotherPlaylist;

        @BeforeEach
        void existingPlaylist() {
            myPlaylist      = service.createPlaylist("My Playlist");
            anotherPlaylist = service.createPlaylist("Another Playlist");
        }

        @Nested
        class WithValidPlaylist {

            @Test
            void returnsEmptyOptional() {
                Optional<String> error = service.deletePlaylist(myPlaylist);
                assertFalse(error.isPresent());
            }

            @Test
            void playlistIsRemovedFromCollection() {
                service.deletePlaylist(myPlaylist);
                assertEquals(1, service.getPlaylists().size());
            }

            @Test
            void correctPlaylistIsRemoved() {
                service.deletePlaylist(myPlaylist);
                assertEquals("Another Playlist", service.getPlaylists().get(0).getName());
            }

            @Test
            void deletingLastPlaylistLeavesEmptyCollection() {
                service.deletePlaylist(myPlaylist);
                service.deletePlaylist(anotherPlaylist);
                assertEquals(0, service.getPlaylists().size());
            }
        }

        @Nested
        class TracksIntegrity {

            @Test
            void deletingPlaylistDoesNotRemoveTracksSharedWithOtherPlaylists() {
                Playlist playlistA = service.createPlaylist("Playlist A");
                Playlist playlistB = service.createPlaylist("Playlist B");
                Track track = new Track("Track1", "pippo", 180, "rock");
                playlistA.addTrack(track);
                playlistB.addTrack(track);

                service.deletePlaylist(playlistA);

                Collection<Track> tracksInB = playlistB.getTracks();
                assertTrue(tracksInB.contains(track));
            }

            @Test
            void deletingPopulatedPlaylistKeepsTracksInLibrary() {
                Playlist playlist = service.createPlaylist("Playlist");
                Track track = new Track("Track1", "pippo", 180, "rock");
                Library.getInstance().addTrack(track);
                playlist.addTrack(track);

                service.deletePlaylist(playlist);

                assertTrue(Library.getInstance().getTracks().contains(track));
            }
        }

        @Nested
        class WithNullPlaylist {

            @Test
            void returnsErrorForNullPlaylist() {
                Optional<String> error = service.deletePlaylist(null);
                assertTrue(error.isPresent());
            }

            @Test
            void collectionIsUnchangedForNullPlaylist() {
                service.deletePlaylist(null);
                assertEquals(2, service.getPlaylists().size());
            }
        }

        @Nested
        class WithNonExistentPlaylist {

            @Test
            void returnsError() {
                Playlist ghost = new Playlist("Ghost Playlist", null);
                Optional<String> error = service.deletePlaylist(ghost);
                assertTrue(error.isPresent());
            }

            @Test
            void collectionIsUnchanged() {
                Playlist ghost = new Playlist("Ghost Playlist", null);
                service.deletePlaylist(ghost);
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
                Playlist toDelete = persistenceService.getPlaylist("To Delete");
                persistenceService.deletePlaylist(toDelete);

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals(1, reloaded.getPlaylists().size());
                assertEquals("To Keep", reloaded.getPlaylists().get(0).getName());
            }

            @Test
            void survivingPlaylistIsStillPresentAfterReload() {
                Playlist toDelete = persistenceService.getPlaylist("To Delete");
                persistenceService.deletePlaylist(toDelete);

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals("To Keep", reloaded.getPlaylists().get(0).getName());
            }
        }
    }

    @Nested
    class RenamePlaylist {

        private Playlist oldPlaylist;
        private Playlist anotherPlaylist;

        @BeforeEach
        void existingPlaylists() {
            oldPlaylist     = service.createPlaylist("Old Name");
            anotherPlaylist = service.createPlaylist("Another Playlist");
        }

        @Test
        void successfulRename() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "New Name");
            assertFalse(error.isPresent());
            assertEquals("New Name", oldPlaylist.getName());
            assertNull(service.getPlaylist("Old Name"));
            assertNotNull(service.getPlaylist("New Name"));
        }

        @Test
        void returnsErrorForNullNewName() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, null);
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
        }

        @Test
        void returnsErrorForEmptyNewName() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "   ");
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
        }

        @Test
        void returnsErrorIfPlaylistNotManaged() {
            Playlist external = new Playlist("Non Existent", null);
            Optional<String> error = service.renamePlaylist(external, "New Name");
            assertTrue(error.isPresent());
        }

        @Test
        void returnsEmptyOptionalIfNamesAreIdentical() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "Old Name");
            assertFalse(error.isPresent());
        }

        @Test
        void returnsErrorIfNewNameAlreadyExists() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "Another Playlist");
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
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
                Playlist toRename = persistenceService.getPlaylist("Old Name");
                persistenceService.renamePlaylist(toRename, "New Name");

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

        @Test
        void loadDoesNothingIfFileDoesNotExist() {
            Path nonExisting = tempDir.resolve("does-not-exist.json");
            PlaylistService s = new PlaylistService(nonExisting.toString());
            s.load();
            assertEquals(0, s.getPlaylists().size());
        }

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
        private Playlist playlistA;
        private Playlist playlistB;

        @BeforeEach
        void setUp() {
            playlistA = service.createPlaylist("Playlist A");
            playlistB = service.createPlaylist("Playlist B");

            sharedTrack = new Track("Stayin' Alive", "Bee Gees", 285, "Disco");

            try {
                Library.getInstance().addTrack(sharedTrack);
            } catch (IllegalArgumentException ignored) {}

            playlistA.addTrack(sharedTrack);
            playlistB.addTrack(sharedTrack);
        }

        @Test
        void removingTrackFromOnePlaylistKeepsItInLibrary() {
            playlistA.removeTrack(sharedTrack);
            service.save();

            assertFalse(playlistA.getTracks().contains(sharedTrack),
                    "Track should've been removed from A");
            assertTrue(Library.getInstance().getTracks().contains(sharedTrack),
                    "Track must still be available in the library");
        }

        @Test
        void removingTrackFromOnePlaylistKeepsItInOtherPlaylists() {
            playlistA.removeTrack(sharedTrack);
            service.save();

            assertTrue(playlistB.getTracks().contains(sharedTrack),
                    "Track should still be available in Playlist B");
        }
    }
}