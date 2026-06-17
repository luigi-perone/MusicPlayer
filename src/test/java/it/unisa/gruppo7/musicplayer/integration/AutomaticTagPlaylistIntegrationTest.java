package it.unisa.gruppo7.musicplayer.integration;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.strategy.GenerationCriterion;
import it.unisa.gruppo7.musicplayer.playlist.strategy.TagCombinationMode;
import it.unisa.gruppo7.musicplayer.playlist.strategy.TagGenerationStrategy;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the complete automatic tag playlist workflow.
 * Verifies playlist creation, empty-result handling, automatic updates
 * after tag changes, and isolation of manual playlists.
 *
 * @author Matteo Postiglione
 */

class AutomaticTagPlaylistIntegrationTest {
    @TempDir
    Path tempDir;

    private Library library;
    private InMemoryPlaylistService service;
    private MusicPlayerFacade facade;

    /** Resets singletons and installs an in-memory playlist service for each test. */
    @BeforeEach
    void setUp() throws Exception {
        resetFacadeSingleton();
        library = Library.getInstance();
        library.clearLibrary();
        setLibraryPath(tempDir.resolve("track-library.json"));
        service = new InMemoryPlaylistService(
                tempDir.resolve("playlists.json").toString());
        facade = installPlaylistService(service);
    }

    /** Clears the library, shuts down playback and resets the facade singleton. */
    @AfterEach
    void tearDown() throws Exception {
        library.clearLibrary();
        if (facade != null) {
            facade.shutdownPlayback();
        }
        resetFacadeSingleton();
    }
    /** 
     * Verifies that valid generation creates a playlist containing only matching tracks. */
    @Test
    void validGenerationCreatesPlaylistWithMatchingTracks() {
        Track matching = track("Matching", TrackTag.FAVOURITE);
        Track notMatching = track("Not matching");
        library.addTrack(matching);
        library.addTrack(notMatching);

        Playlist playlist = facade.createAutoPlaylist(
                "Preferite",
                strategy(TrackTag.FAVOURITE),
                tagRule(TrackTag.FAVOURITE));

        assertEquals(Collections.singletonList(matching), playlist.getPlaylist());
        assertTrue(service.hasRule("Preferite"));
    }
    /** 
     * Verifies that playlist creation is blocked when no tracks match the selected tags. */
    @Test
    void generationWithoutMatchesIsBlockedWithExpectedMessage() {
        library.addTrack(track("Without tags"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> facade.createAutoPlaylist(
                        "Preferite",
                        strategy(TrackTag.FAVOURITE),
                        tagRule(TrackTag.FAVOURITE)));

        assertEquals("Nessuna traccia trovata", exception.getMessage());
        assertFalse(service.hasRule("Preferite"));
    }
    /** 
     * Verifies that refresh removes a track after it loses a required tag. */
    @Test
    void refreshRemovesTrackThatLostRequiredTag() {
        Track track = track("Favourite", TrackTag.FAVOURITE);
        Playlist playlist = automaticPlaylist("Preferite", track);

        track.removeTag(TrackTag.FAVOURITE);
        service.refreshAutomaticPlaylists(Collections.singletonList(track));

        assertTrue(playlist.getTracks().isEmpty());
    }
    /** 
     * Verifies that refresh adds a track after it acquires a required tag. */
    @Test
    void refreshAddsTrackThatAcquiredRequiredTag() {
        Track track = track("Future favourite");
               Playlist playlist = service.createPlaylist("Preferite");
        service.registerAutomaticPlaylist(
                playlist, tagRule(TrackTag.FAVOURITE));

        track.addTag(TrackTag.FAVOURITE);
        service.refreshAutomaticPlaylists(Collections.singletonList(track));

        assertEquals(Collections.singletonList(track), playlist.getPlaylist());
    }
    /** 
     * Verifies that automatic refresh does not modify manual playlists. */
    @Test
    void refreshDoesNotModifyManualPlaylist() {
        Track track = track("Manual track", TrackTag.FAVOURITE);
        Playlist playlist = service.createPlaylist("Manuale");
        service.addTracksToPlaylist(playlist, Collections.singletonList(track));

        track.removeTag(TrackTag.FAVOURITE);
        service.refreshAutomaticPlaylists(Collections.singletonList(track));

        assertEquals(Collections.singletonList(track), playlist.getPlaylist());
    }

    /** Creates an automatic playlist seeded with the given track and a FAVOURITE tag rule. */
    private Playlist automaticPlaylist(String name, Track track) {
        Playlist playlist = service.createPlaylist(name);
        service.addTracksToPlaylist(playlist, Collections.singletonList(track));
        service.registerAutomaticPlaylist(
                playlist, tagRule(TrackTag.FAVOURITE));
        return playlist;
    }

    /** Builds an ALL-mode tag strategy matching the given tag. */
    private TagGenerationStrategy strategy(TrackTag tag) {
        return new TagGenerationStrategy(
                EnumSet.of(tag), TagCombinationMode.ALL);
    }

    /** Builds an ALL-mode tag rule matching the given tag. */
    private AutomaticPlaylistRule tagRule(TrackTag tag) {
        AutomaticPlaylistRule rule = new AutomaticPlaylistRule();
        rule.criterion = GenerationCriterion.TAG;
        rule.tags = EnumSet.of(tag);
        rule.combinationMode = TagCombinationMode.ALL;
        return rule;
    }

    /** Builds a Pop track with the given title and tags. */
    private Track track(String title, TrackTag... tags) {
        Track track = new Track(title, "Artist", 180, "Pop");
        for (TrackTag tag : tags) {
            track.addTag(tag);
        }
        return track;
    }

    /** Replaces the facade's playlist service with the given one and rewires the observer. */
    private MusicPlayerFacade installPlaylistService(PlaylistService replacement)
            throws Exception {
        MusicPlayerFacade instance = MusicPlayerFacade.getInstance();
        Field field = MusicPlayerFacade.class.getDeclaredField("playlistService");
        field.setAccessible(true);
        PlaylistService previous = (PlaylistService) field.get(instance);
        instance.removeObserver(previous);
        field.set(instance, replacement);
        instance.addObserver(replacement);
        return instance;
    }

    /** Clears the {@link MusicPlayerFacade} singleton instance via reflection. */
    private void resetFacadeSingleton() throws Exception {
        Field field = MusicPlayerFacade.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    /** Points the singleton library at the given temporary path via reflection. */
    private void setLibraryPath(Path path) throws Exception {
        Field field = library.getClass().getSuperclass().getDeclaredField("path");
        field.setAccessible(true);
        field.set(library, path.toString());
    }

    /** In-memory {@link PlaylistService} that never persists to disk, used for isolated tests. */
    private static class InMemoryPlaylistService extends PlaylistService {
        InMemoryPlaylistService(String path) throws Exception {
            super(path);
            rules().clear();
        }

        /** No-op: tests keep playlist state in memory and never touch project data. */
        @Override
        public void save() {
            // Tests keep playlist state in memory and never touch project data.
        }

        /** Registers an automatic rule in memory without persisting it. */
        @Override
        public void registerAutomaticPlaylist(
                Playlist playlist, AutomaticPlaylistRule rule) {
            rules().put(playlist.getName(), rule);
        }

        /** Returns whether an automatic rule is registered for the given playlist name. */
        boolean hasRule(String playlistName) {
            return rules().containsKey(playlistName);
        }

        /** Reflectively accesses the parent service's automatic-rules map. */
        @SuppressWarnings("unchecked")
        private Map<String, AutomaticPlaylistRule> rules() {
            try {
                Field field = PlaylistService.class.getDeclaredField("automaticRules");
                field.setAccessible(true);
                return (Map<String, AutomaticPlaylistRule>) field.get(this);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
