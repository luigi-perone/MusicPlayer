package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US-014: playlist playback.
 * Covers all acceptance criteria: start from populated playlist,
 * reject empty playlist, overwrite previous queue, and automatic
 * advance to next track at end of current.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaylistPlaybackIntegrationTest {

    private static final String TEST_LIBRARY_PATH  = "data/test-it-us014-library.json";
    private static final String TEST_PLAYLIST_PATH = "data/test-it-us014-playlist.json";

    private MusicPlayerFacade facade;
    private PlaybackService   service;
    private PlaylistService   playlistService;

    private Track trackA;
    private Track trackB;
    private Track trackC;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Resets singletons, builds the facade and adds three sample tracks to the library. */
    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();

        playlistService = new PlaylistService(TEST_PLAYLIST_PATH);
        facade          = buildFacade(playlistService);
        service         = facade.getPlaybackService();

        trackA = addToLibrary("US014 A", "Artist", 10);
        trackB = addToLibrary("US014 B", "Artist", 10);
        trackC = addToLibrary("US014 C", "Artist", 10);
    }

    /** Shuts down playback, deletes the test files and resets singletons. */
    @AfterEach
    void tearDown() throws Exception {
        facade.shutdownPlayback();
        new File(TEST_LIBRARY_PATH).delete();
        new File(TEST_PLAYLIST_PATH).delete();
        resetSingletons();
    }

    // ------------------------------------------------------------------
    // Test scenarios evaluating playback start behaviors
    // ------------------------------------------------------------------

    /**
     * Test scenarios evaluating the effects of starting playback
     * from a playlist under different initial conditions.
     */
    @Nested
    class WhenStartingPlayback {

        /**
         * IT-014-1 – Verifies that starting playback from a populated playlist
         * correctly loads the full queue and begins from the first track.
         */
        @Test
        @Order(1)
        void playFromPlaylist_populated_loadsQueueAndStartsFromFirst() throws Exception {
            Playlist playlist = playlistService.createPlaylist("Pop-IT");
            playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

            facade.playFromPlaylist(playlist);
            service.stopTimer();

            assertSame(trackA, facade.getCurrentPlayingTrack(),
                    "Playback must start from the first track in the playlist");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must be in PLAYING state");
            assertSame(playlist, facade.getActivePlaylist(),
                    "Active playlist must be set to the started playlist");

            java.util.List<Track> queue =
                    (java.util.List<Track>) service.getQueue().getTracks();
            assertEquals(3, queue.size(),
                    "Queue must contain all 3 tracks from the playlist");
            assertSame(trackA, queue.get(0), "First queue slot must be A");
            assertSame(trackB, queue.get(1), "Second queue slot must be B");
            assertSame(trackC, queue.get(2), "Third queue slot must be C");
        }

        /**
         * IT-014-2 – Verifies that attempting to start playback from an empty playlist
         * is blocked with an {@link IllegalArgumentException} and leaves the player untouched.
         */
        @Test
        @Order(2)
        void playFromPlaylist_empty_throwsWithMessage() {
            Playlist empty = playlistService.createPlaylist("Empty-IT");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> facade.playFromPlaylist(empty),
                    "Starting an empty playlist must throw IllegalArgumentException"
            );

            assertTrue(ex.getMessage().contains(empty.getName()),
                    "Error message must mention the playlist name");

            assertEquals(PlaybackState.START_UP, facade.getPlaybackState(),
                    "Player state must not change after a rejected start");
            assertNull(facade.getCurrentPlayingTrack(),
                    "Current track must remain null after a rejected start");
        }

        /**
         * IT-014-3 – Verifies that starting a new playlist while one is already active
         * correctly overwrites the previous queue and updates the active playlist reference.
         */
        @Test
        @Order(3)
        void playFromPlaylist_newPlaylist_overwritesPreviousQueue() throws Exception {
            Playlist first = playlistService.createPlaylist("First-IT");
            playlistService.addTracksToPlaylist(first, Arrays.asList(trackA, trackB));
            facade.playFromPlaylist(first);
            service.stopTimer();

            assertSame(trackA, facade.getCurrentPlayingTrack());

            Playlist second = playlistService.createPlaylist("Second-IT");
            playlistService.addTracksToPlaylist(second, Arrays.asList(trackC));
            facade.playFromPlaylist(second);
            service.stopTimer();

            assertSame(trackC, facade.getCurrentPlayingTrack(),
                    "After starting a new playlist, current track must be the first of the new one");
            assertSame(second, facade.getActivePlaylist(),
                    "Active playlist must be updated to the new playlist");

            java.util.List<Track> queue =
                    (java.util.List<Track>) service.getQueue().getTracks();
            assertEquals(1, queue.size(),
                    "Queue must contain only the tracks of the new playlist");
            assertSame(trackC, queue.get(0),
                    "The only queue entry must be C from the second playlist");
        }

        /**
         * IT-014-6 – Verifies that starting playback from a specific mid-playlist track
         * correctly begins from that track rather than the first.
         */
        @Test
        @Order(6)
        void playFromPlaylistFrom_midTrack_startsFromGivenTrack() throws Exception {
            Playlist playlist = playlistService.createPlaylist("MidStart-IT");
            playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

            facade.playFromPlaylistFrom(playlist, trackB);
            service.stopTimer();

            assertSame(trackB, facade.getCurrentPlayingTrack(),
                    "Playback must start from B when explicitly requested");
            assertSame(playlist, facade.getActivePlaylist(),
                    "Active playlist must be set even when starting from a mid track");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must be in PLAYING state");
        }
    }

    // ------------------------------------------------------------------
    // Test scenarios evaluating automatic track advancement behaviors
    // ------------------------------------------------------------------

    /**
     * Test scenarios evaluating the automatic progression of playback
     * when a track ends, with repeat mode off.
     */
    @Nested
    class WhenTrackEnds {

        /**
         * IT-014-4 – Verifies that when the current track ends, playback automatically
         * advances to the next track in the queue.
         */
        @Test
        @Order(4)
        void playFromPlaylist_trackEnds_autoAdvancesToNext() throws Exception {
            Playlist playlist = playlistService.createPlaylist("AutoAdv-IT");
            playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB, trackC));

            service.setRepeatMode(RepeatMode.OFF);
            facade.playFromPlaylist(playlist);
            service.stopTimer();

            assertSame(trackA, facade.getCurrentPlayingTrack());

            // Simulate end of trackA by manually triggering playNext
            // (mirrors what startTimer does when simulatedTime >= track.getDuration())
            service.playNext();
            service.stopTimer();

            assertSame(trackB, facade.getCurrentPlayingTrack(),
                    "After track A ends, playback must automatically advance to B");
            assertEquals(PlaybackState.PLAYING, facade.getPlaybackState(),
                    "Player must remain in PLAYING state after auto-advance");
        }

        /**
         * IT-014-5 – Verifies that when the last track in the queue ends with repeat off,
         * playback stops and the current track reference is cleared.
         */
        @Test
        @Order(5)
        void playFromPlaylist_lastTrackEnds_stopsPlayback() throws Exception {
            Playlist playlist = playlistService.createPlaylist("LastStop-IT");
            playlistService.addTracksToPlaylist(playlist, Arrays.asList(trackA, trackB));

            service.setRepeatMode(RepeatMode.OFF);
            facade.playFromPlaylist(playlist);
            service.stopTimer();

            service.playNext(); // A → B
            service.stopTimer();

            assertSame(trackB, facade.getCurrentPlayingTrack());

            service.playNext(); // B → end of queue
            // No stopTimer needed: playNext calls stop() which cancels the timer

            assertEquals(PlaybackState.STOPPED, facade.getPlaybackState(),
                    "Player must stop after the last track ends with repeat OFF");
            assertNull(facade.getCurrentPlayingTrack(),
                    "Current track must be null after queue is exhausted");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Resets the Library and facade singletons and points the library at the test file. */
    private void resetSingletons() throws Exception {
        Field libraryInstance = Library.class.getDeclaredField("instance");
        libraryInstance.setAccessible(true);
        libraryInstance.set(null, null);

        Library lib = Library.getInstance();
        Field libraryPath = lib.getClass().getSuperclass().getDeclaredField("path");
        libraryPath.setAccessible(true);
        libraryPath.set(lib, TEST_LIBRARY_PATH);
        lib.clearLibrary();

    }

    /** Builds the facade singleton wired to the given playlist service as observer. */
    private MusicPlayerFacade buildFacade(PlaylistService ps) throws Exception {
        MusicPlayerFacade f = new MusicPlayerFacade();

        Field psField = MusicPlayerFacade.class.getDeclaredField("playlistService");
        psField.setAccessible(true);

        PlaylistService old = (PlaylistService) psField.get(f);
        f.removeObserver(old);
        psField.set(f, ps);
        f.addObserver(ps);
        return f;
    }

    /** Adds a track to the library and returns the created {@link Track}. */
    private Track addToLibrary(String title, String author, int duration) {
        facade.addNewTrackToLibrary(title, author, duration, null, null);
        return facade.getTracksFromLibrary().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Track not found: " + title));
    }
}