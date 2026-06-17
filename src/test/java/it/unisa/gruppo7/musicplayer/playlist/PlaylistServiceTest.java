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

/**
 * Unit test suite for the {@link PlaylistService} class.
 * Validates playlist lifecycle actions including creation, name validation,
 * safe deletion, structural renaming, track consistency, and JSON disk persistence.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
class PlaylistServiceTest {

    @TempDir
    Path tempDir;
    private PlaylistService service;

    /**
     * Initializes a new isolated state context before each test method execution.
     * Creates a temporary file to separate testing persistence IO operations.
     */
    @BeforeEach
    void setUp() {
        Path tempFile = tempDir.resolve("isolated-test-playlists.json");
        service = new PlaylistService(tempFile.toString());
    }

    /**
     * Test scenarios verifying playlist creation when valid names are supplied.
     */
    @Nested
    class WithValidName {

        /**
         * Verifies that a valid playlist name successfully increases the collection size.
         */
        @Test
        void playlistIsAddedToCollection() {
            service.createPlaylist("My Playlist");
            assertEquals(1, service.getPlaylists().size());
        }

        /**
         * Assures that the assigned playlist name perfectly matches the original input data.
         */
        @Test
        void playlistHasGivenName() {
            service.createPlaylist("My Playlist");
            assertEquals("My Playlist", service.getPlaylists().get(0).getName());
        }
    }

    /**
     * Test scenarios verifying rejection behavior when blank, empty, or null inputs are provided.
     */
    @Nested
    class WithEmptyName {

        /**
         * Checks that an empty string input prompts an IllegalArgumentException.
         */
        @Test
        void throwsExceptionForEmptyString() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist(""));
        }

        /**
         * Checks that a null reference value prompts an IllegalArgumentException.
         */
        @Test
        void throwsExceptionForNullName() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist(null));
        }

        /**
         * Checks that white space strings prompt an IllegalArgumentException.
         */
        @Test
        void throwsExceptionForWhitespaceName() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist("   "));
        }

        /**
         * Guarantees the internal playlist collection data array remains unchanged
         * after a failed validation attempt.
         */
        @Test
        void playlistIsNotAdded() {
            try { service.createPlaylist(""); } catch (IllegalArgumentException ignored) {}
            assertEquals(0, service.getPlaylists().size());
        }
    }

    /**
     * Test scenarios evaluating duplicate constraint enforcement behaviors.
     */
    @Nested
    class WithDuplicateName {

        /**
         * Sets up a baseline collision name condition within the environment.
         */
        @BeforeEach
        void existingPlaylist() {
            service.createPlaylist("My Playlist");
        }

        /**
         * Assures an IllegalArgumentException triggers if a matching name string is submitted again.
         */
        @Test
        void throwsExceptionForDuplicateName() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createPlaylist("My Playlist"));
        }

        /**
         * Confirms that duplicate creation actions do not duplicate record structures.
         */
        @Test
        void duplicateIsNotAdded() {
            try { service.createPlaylist("My Playlist"); } catch (IllegalArgumentException ignored) {}
            assertEquals(1, service.getPlaylists().size());
        }
    }

    /**
     * Comprehensive test suite validating playlist deletion routines.
     */
    @Nested
    class DeletePlaylist {

        private Playlist myPlaylist;
        private Playlist anotherPlaylist;

        /**
         * Seeds initial dummy records to process deletion transitions.
         */
        @BeforeEach
        void existingPlaylist() {
            myPlaylist      = service.createPlaylist("My Playlist");
            anotherPlaylist = service.createPlaylist("Another Playlist");
        }

        /**
         * Core deletion validations for verified tracked records.
         */
        @Nested
        class WithValidPlaylist {

            /**
             * Assures successful execution yields an empty Optional error indicator.
             */
            @Test
            void returnsEmptyOptional() {
                Optional<String> error = service.deletePlaylist(myPlaylist);
                assertFalse(error.isPresent());
            }

            /**
             * Assures the registry array down-sizes correctly following record deletion.
             */
            @Test
            void playlistIsRemovedFromCollection() {
                service.deletePlaylist(myPlaylist);
                assertEquals(1, service.getPlaylists().size());
            }

            /**
             * Confirms only the explicitly targeted record instance gets pruned away.
             */
            @Test
            void correctPlaylistIsRemoved() {
                service.deletePlaylist(myPlaylist);
                assertEquals("Another Playlist", service.getPlaylists().get(0).getName());
            }

            /**
             * Verifies that removing all sequential entities cleans the storage mapping space entirely.
             */
            @Test
            void deletingLastPlaylistLeavesEmptyCollection() {
                service.deletePlaylist(myPlaylist);
                service.deletePlaylist(anotherPlaylist);
                assertEquals(0, service.getPlaylists().size());
            }
        }

        /**
         * Safety validations protecting structural track entities during playlist cleanup tasks.
         */
        @Nested
        class TracksIntegrity {

            /**
             * Assures that wiping playlist A leaves common shared tracks untouched inside playlist B.
             */
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

            /**
             * Verifies that deleting a populated playlist profile does not purge its constituent
             * tracks from the global system music library repository.
             */
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

        /**
         * Edge-case validations handling null pointer requests gracefully.
         */
        @Nested
        class WithNullPlaylist {

            /**
             * Assures a diagnostic error logging feedback string returns on a null request.
             */
            @Test
            void returnsErrorForNullPlaylist() {
                Optional<String> error = service.deletePlaylist(null);
                assertTrue(error.isPresent());
            }

            /**
             * Guarantees data arrays remain unmodified upon incoming null parameters.
             */
            @Test
            void collectionIsUnchangedForNullPlaylist() {
                service.deletePlaylist(null);
                assertEquals(2, service.getPlaylists().size());
            }
        }

        /**
         * Edge-case validations handling unmanaged playlist references.
         */
        @Nested
        class WithNonExistentPlaylist {

            /**
             * Assures an error message generates when processing an unmapped record reference.
             */
            @Test
            void returnsError() {
                Playlist ghost = new Playlist("Ghost Playlist", null);
                Optional<String> error = service.deletePlaylist(ghost);
                assertTrue(error.isPresent());
            }

            /**
             * Assures registry parameters stay perfectly static under unknown execution contexts.
             */
            @Test
            void collectionIsUnchanged() {
                Playlist ghost = new Playlist("Ghost Playlist", null);
                service.deletePlaylist(ghost);
                assertEquals(2, service.getPlaylists().size());
            }
        }

        /**
         * Validates IO file persistence interaction consistency for deletion transactions.
         */
        @Nested
        class Persistence {

            @TempDir
            Path tempDir;

            private PlaylistService persistenceService;
            private Path tempFile;

            /**
             * Pre-loads an explicit physical JSON disk state before verifying update loads.
             */
            @BeforeEach
            void setUpTempFile() {
                tempFile = tempDir.resolve("test-playlists.json");
                persistenceService = new PlaylistService(tempFile.toString());
                persistenceService.createPlaylist("To Keep");
                persistenceService.createPlaylist("To Delete");
                persistenceService.save();
            }

            /**
             * Verifies a deleted profile configuration is not restored after a reload.
             */
            @Test
            void deletedPlaylistIsNotRestoredAfterReload() {
                Playlist toDelete = persistenceService.getPlaylist("To Delete");
                persistenceService.deletePlaylist(toDelete);

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                assertEquals(1, reloaded.getPlaylists().size());
                assertEquals("To Keep", reloaded.getPlaylists().get(0).getName());
            }

            /**
             * Ensures non-deleted tracking profiles remain intact after loading data.
             */
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

    /**
     * Test scenarios evaluating renaming operation boundaries and conditions.
     */
    @Nested
    class RenamePlaylist {

        private Playlist oldPlaylist;
        private Playlist anotherPlaylist;

        /**
         * Generates target test fields for naming mutation operations.
         */
        @BeforeEach
        void existingPlaylists() {
            oldPlaylist     = service.createPlaylist("Old Name");
            anotherPlaylist = service.createPlaylist("Another Playlist");
        }

        /**
         * Verifies a standard valid title mutation processes correctly.
         */
        @Test
        void successfulRename() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "New Name");
            assertFalse(error.isPresent());
            assertEquals("New Name", oldPlaylist.getName());
            assertNull(service.getPlaylist("Old Name"));
            assertNotNull(service.getPlaylist("New Name"));
        }

        /**
         * Confirms title update requests containing null are blocked with error responses.
         */
        @Test
        void returnsErrorForNullNewName() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, null);
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
        }

        /**
         * Confirms title update requests containing empty whitespaces are rejected.
         */
        @Test
        void returnsErrorForEmptyNewName() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "   ");
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
        }

        /**
         * Confirms rename attempts directed at external unmanaged references are rejected.
         */
        @Test
        void returnsErrorIfPlaylistNotManaged() {
            Playlist external = new Playlist("Non Existent", null);
            Optional<String> error = service.renamePlaylist(external, "New Name");
            assertTrue(error.isPresent());
        }

        /**
         * Verifies renaming to the exact same value acts as a safe, error-free no-op.
         */
        @Test
        void returnsEmptyOptionalIfNamesAreIdentical() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "Old Name");
            assertFalse(error.isPresent());
        }

        /**
         * Confirms that a name change conflict with an existing playlist returns an error.
         */
        @Test
        void returnsErrorIfNewNameAlreadyExists() {
            Optional<String> error = service.renamePlaylist(oldPlaylist, "Another Playlist");
            assertTrue(error.isPresent());
            assertEquals("Old Name", oldPlaylist.getName());
        }

        /**
         * Validates persistence synchronization states across renaming transactions.
         */
        @Nested
        class Persistence {

            @TempDir
            Path tempDir;

            private PlaylistService persistenceService;
            private Path tempFile;

            /**
             * Saves baseline structure entries to disc.
             */
            @BeforeEach
            void setUpTempFile() {
                tempFile = tempDir.resolve("test-rename-playlists.json");
                persistenceService = new PlaylistService(tempFile.toString());
                persistenceService.createPlaylist("Old Name");
                persistenceService.save();
            }

            /**
             * Assures the new name maps correctly onto persistence records upon system reload.
             */
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

    /**
     * Verification suite handling disk IO read and write integration operations.
     */
    @Nested
    class Persistence {

        @TempDir
        Path tempDir;

        private PlaylistService persistenceService;
        private Path tempFile;

        /**
         * Assigns localized isolated disk resource endpoints.
         */
        @BeforeEach
        void setUpTempFile() {
            tempFile = tempDir.resolve("test-playlists.json");
            persistenceService = new PlaylistService(tempFile.toString());
        }

        /**
         * Assures saving a model profile generates a valid file on disk.
         */
        @Test
        void saveWritesFileToDisk() {
            persistenceService.createPlaylist("My Playlist");
            persistenceService.save();
            assertTrue(Files.exists(tempFile));
        }

        /**
         * Verifies that attempting to load a non-existent file completes safely without errors.
         */
        @Test
        void loadDoesNothingIfFileDoesNotExist() {
            Path nonExisting = tempDir.resolve("does-not-exist.json");
            PlaylistService s = new PlaylistService(nonExisting.toString());
            s.load();
            assertEquals(0, s.getPlaylists().size());
        }

        /**
         * Validates full structural data recovery during serialized storage read phases.
         */
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

    /**
     * Verifies system boundaries when deleting tracks from playlist entities.
     */
    @Nested
    class RemoveTrackFromPlaylist {

        private Track sharedTrack;
        private Playlist playlistA;
        private Playlist playlistB;

        /**
         * populates testing playlists with shared tracks.
         */
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

        /**
         * Assures removing a track from a playlist does not remove it from the global music library.
         */
        @Test
        void removingTrackFromOnePlaylistKeepsItInLibrary() {
            playlistA.removeTrack(sharedTrack);
            service.save();

            assertFalse(playlistA.getTracks().contains(sharedTrack),
                    "Track should've been removed from A");
            assertTrue(Library.getInstance().getTracks().contains(sharedTrack),
                    "Track must still be available in the library");
        }

        /**
         * Assures dropping a track out of playlist A leaves playlist B completely untouched.
         */
        @Test
        void removingTrackFromOnePlaylistKeepsItInOtherPlaylists() {
            playlistA.removeTrack(sharedTrack);
            service.save();

            assertTrue(playlistB.getTracks().contains(sharedTrack),
                    "Track should still be available in Playlist B");
        }
    }

    /**
     * Test scenarios for reordering tracks within a playlist (US-027).
     */
    @Nested
    class ReorderTrack {

        private Playlist playlist;
        private Track t1, t2, t3;

        /** Creates a playlist with three ordered tracks before each test. */
        @BeforeEach
        void setUp() {
            playlist = service.createPlaylist("Reorder Playlist");
            t1 = new Track("First",  "Author", 100, "Rock");
            t2 = new Track("Second", "Author", 100, "Rock");
            t3 = new Track("Third",  "Author", 100, "Rock");
            playlist.addTrack(t1);
            playlist.addTrack(t2);
            playlist.addTrack(t3);
        }

        /** Verifies that moving a track changes the playlist model order. */
        @Test
        void movingTrackChangesTheModelOrder() {
            Optional<String> error = service.reorderTrack(playlist, 0, 2);

            assertFalse(error.isPresent());
            assertEquals(java.util.Arrays.asList(t2, t3, t1), playlist.getPlaylist());
        }

        /** Verifies that reordering to the same index is a no-op. */
        @Test
        void sameIndexIsANoOp() {
            Optional<String> error = service.reorderTrack(playlist, 1, 1);

            assertFalse(error.isPresent());
            assertEquals(java.util.Arrays.asList(t1, t2, t3), playlist.getPlaylist());
        }

        /** Verifies that reordering a null playlist returns an error. */
        @Test
        void returnsErrorForNullPlaylist() {
            assertTrue(service.reorderTrack(null, 0, 1).isPresent());
        }

        /** Verifies that reordering an unmanaged playlist returns an error. */
        @Test
        void returnsErrorForUnmanagedPlaylist() {
            Playlist ghost = new Playlist("Ghost", null);
            assertTrue(service.reorderTrack(ghost, 0, 0).isPresent());
        }

        /** Verifies that reordering with out-of-range indices returns an error and keeps the order. */
        @Test
        void returnsErrorForOutOfRangeIndices() {
            assertTrue(service.reorderTrack(playlist, -1, 1).isPresent());
            assertTrue(service.reorderTrack(playlist, 0, 99).isPresent());
            // Order must remain unchanged after rejected moves.
            assertEquals(java.util.Arrays.asList(t1, t2, t3), playlist.getPlaylist());
        }

        /**
         * Verifies the custom order persists across a save/reload cycle (AC1/AC4).
         */
        @Nested
        class Persistence {

            @TempDir
            Path tempDir;

            /** Verifies that the reordered sequence is restored after a save/reload cycle. */
            @Test
            void reorderedSequenceIsRestoredAfterReload() {
                Path tempFile = tempDir.resolve("reorder-playlists.json");
                PlaylistService persistenceService = new PlaylistService(tempFile.toString());

                // The tracks must live in the library so load() can re-map their ids.
                Library library = Library.getInstance();
                for (Track t : new Track[]{t1, t2, t3}) {
                    try { library.addTrack(t); } catch (IllegalArgumentException ignored) {}
                }

                Playlist p = persistenceService.createPlaylist("Persisted Order");
                p.addTrack(t1);
                p.addTrack(t2);
                p.addTrack(t3);

                persistenceService.reorderTrack(p, 0, 2); // -> t2, t3, t1 (saves)

                PlaylistService reloaded = new PlaylistService(tempFile.toString());
                reloaded.load();

                Playlist reloadedPlaylist = reloaded.getPlaylist("Persisted Order");
                assertEquals(
                        java.util.Arrays.asList(t2.getId(), t3.getId(), t1.getId()),
                        reloadedPlaylist.getTrackIds(),
                        "The custom track order must survive a reload");
            }
        }
    }
}