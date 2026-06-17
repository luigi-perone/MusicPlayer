package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.*;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for US-027 (AC3) exercising the same path the UI controller uses
 * through {@link MusicPlayerFacade}: reorder a track in a playlist that is the active
 * playback source and verify the playing track continues and the queue reorders.
 */
@DisplayName("US-027 — Riordino via facade durante la riproduzione")
public class PlaylistReorderFacadeIntegrationTest {

    private MusicPlayerFacade facade;
    private PlaylistService playlistService;
    private Playlist playlist;
    private Track t1, t2, t3, t4;

    /** Clears the library and playlists and seeds a four-track playlist before each test. */
    @BeforeEach
    void setUp() {
        facade = MusicPlayerFacade.getInstance();
        playlistService = facade.getPlaylistService();

        playlistService.getPlaylists().clear();
        facade.clearLibrary();

        facade.addNewTrackToLibrary("One",   "A", 100, "Rock", Year.of(2000));
        facade.addNewTrackToLibrary("Two",   "A", 100, "Rock", Year.of(2000));
        facade.addNewTrackToLibrary("Three", "A", 100, "Rock", Year.of(2000));
        facade.addNewTrackToLibrary("Four",  "A", 100, "Rock", Year.of(2000));

        List<Track> lib = new ArrayList<>(facade.getTracksFromLibrary());
        t1 = byTitle(lib, "One");
        t2 = byTitle(lib, "Two");
        t3 = byTitle(lib, "Three");
        t4 = byTitle(lib, "Four");

        playlist = facade.createPlaylist("US27");
        playlistService.addTracksToPlaylist(playlist, java.util.Arrays.asList(t1, t2, t3, t4));
    }

    /** Pauses playback and clears the queue, keeping the shared singleton alive for other tests. */
    @AfterEach
    void tearDown() {
        // Pause ticking but keep the shared singleton's timer executor alive for other tests.
        facade.pauseTrack();
        facade.getPlaybackService().getQueue().clear();
    }

    /** Finds a track by title in the given list, throwing if it is absent. */
    private static Track byTitle(List<Track> tracks, String title) {
        return tracks.stream().filter(t -> t.getTitle().equals(title)).findFirst().orElseThrow(NoSuchElementException::new);
    }

    /** Mirrors the controller's reorderTrack(from, to) sequence. */
    private void reorder(int from, int to) {
        playlistService.reorderTrack(playlist, from, to);
        facade.onTrackReorderedInPlaylist(playlist, from, to);
    }

    /** Verifies that moving the playing track keeps it playing and realigns the queue. */
    @Test
    @DisplayName("Spostando la traccia in riproduzione, continua e la coda si riallinea")
    void movingPlayingTrack_continuesAndQueueReorders() {
        // Start playback from T2 (index 1) within the playlist.
        facade.playFromPlaylistFrom(playlist, t2);
        assertSame(t2, facade.getCurrentPlayingTrack(), "Pre-condizione: T2 in riproduzione");

        // Move the playing track T2 from index 1 to index 3 (end).
        reorder(1, 3);

        assertSame(t2, facade.getCurrentPlayingTrack(),
                "La traccia in riproduzione deve continuare ad essere T2");

        List<Track> queue = new ArrayList<>(facade.getPlaybackService().getQueue().getTracks());
        assertEquals(java.util.Arrays.asList(t1, t3, t4, t2), queue,
                "La coda deve riflettere il nuovo ordine della playlist");

        assertSame(t2, facade.getPlaybackService().getQueue().getCurrentTrack(),
                "Il cursore della coda deve seguire T2");
    }

    /** Verifies that moving a future track realigns the queue while the playing track continues. */
    @Test
    @DisplayName("Spostando una traccia futura, la coda si riallinea e T1 continua")
    void movingFutureTrack_queueReorders() {
        facade.playFromPlaylistFrom(playlist, t1);
        assertSame(t1, facade.getCurrentPlayingTrack());

        reorder(3, 1); // T4 -> subito dopo T1

        assertSame(t1, facade.getCurrentPlayingTrack());
        List<Track> queue = new ArrayList<>(facade.getPlaybackService().getQueue().getTracks());
        assertEquals(java.util.Arrays.asList(t1, t4, t2, t3), queue);
    }
}
