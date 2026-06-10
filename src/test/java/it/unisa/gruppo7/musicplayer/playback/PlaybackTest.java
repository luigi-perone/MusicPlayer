package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Arrays;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackTest {

    private PlaybackList playbackList;
    private PlaybackService playbackService;
    private Playlist playlist;
    private Playlist emptyPlaylist;
    private MusicPlayerFacade musicPlayer;
    private Track trk1;
    private Track trk2;
    private Track trk3;

    @BeforeEach
    void setUp() {
        playbackList = new PlaybackList();
        playbackService = new PlaybackService();

        trk1 = new Track("Bohemian Rhapsody", "Queen", 354, "Rock", Year.of(1975));
        trk2 = new Track("Billie Jean", "Michael Jackson", 294, "Pop", Year.of(1982));
        trk3 = new Track("Lose Yourself", "Eminem", 326, "Hip-Hop", Year.of(2002));
         playlist = new Playlist("Test Playlist", new ArrayList<>());
        playlist.addTrack(trk1);
        playlist.addTrack(trk2);
        playlist.addTrack(trk3);
        musicPlayer = MusicPlayerFacade.getInstance();
        emptyPlaylist = new Playlist("Empty Playlist", new ArrayList<>());
    }

    @Nested
    class WhenPlaybackListIsEmpty {

        @Test
        void getNextTrackReturnsNull() {
            assertNull(playbackList.getNextTrack(trk1));
        }

        @Test
        void getPreviousTrackReturnsNull() {
            assertNull(playbackList.getPreviousTrack(trk1));
        }
    }

    @Nested
    class WhenLoadingTracks {

        @Test
        void loadTracksPopulatesQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
            assertEquals(3, playbackList.getTrackCount());
        }

        @Test
        void loadTracksOverwritesPreviousQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2)));
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk3)));
            assertEquals(1, playbackList.getTrackCount());
        }

        @Test
        void appendTracksAddsToExistingQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1)));
            playbackList.appendTracks(new ArrayList<>(Arrays.asList(trk2, trk3)));
            assertEquals(3, playbackList.getTrackCount());
        }
    }

    @Nested
    class WhenNavigatingNext {

        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        @Test
        void getNextTrackFromFirstReturnsSecond() {
            assertEquals(trk2, playbackList.getNextTrack(trk1));
        }

        @Test
        void getNextTrackFromMiddleReturnsThird() {
            assertEquals(trk3, playbackList.getNextTrack(trk2));
        }

        @Test
        void getNextTrackFromLastReturnsNull() {
            assertNull(playbackList.getNextTrack(trk3));
        }

        @Test
        void getNextTrackFromUnknownTrackReturnsNull() {
            Track unknown = new Track("Unknown", "Unknown", 100, "Pop", Year.of(2000));
            assertNull(playbackList.getNextTrack(unknown));
        }
    }

    @Nested
    class WhenNavigatingPrevious {

        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        @Test
        void getPreviousTrackFromLastReturnsSecond() {
            assertEquals(trk2, playbackList.getPreviousTrack(trk3));
        }

        @Test
        void getPreviousTrackFromMiddleReturnsFirst() {
            assertEquals(trk1, playbackList.getPreviousTrack(trk2));
        }

        @Test
        void getPreviousTrackFromFirstReturnsNull() {
            assertNull(playbackList.getPreviousTrack(trk1));
        }

        @Test
        void getPreviousTrackFromUnknownTrackReturnsNull() {
            Track unknown = new Track("Unknown", "Unknown", 100, "Pop", Year.of(2000));
            assertNull(playbackList.getPreviousTrack(unknown));
        }
    }
    
    @Nested
    class WhenReadingUpNextQueue {

        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        @Test
        void getUpNextQueueFromFirstReturnsFollowingTracks() {
            assertEquals(
                Arrays.asList(trk2, trk3),
                playbackList.getUpNextQueue(trk1)
            );
        }

        @Test
        void getUpNextQueueFromLastReturnsEmptyList() {
            assertTrue(playbackList.getUpNextQueue(trk3).isEmpty());
        }

        @Test
        void getUpNextQueueFromUnknownTrackReturnsEmptyList() {
            Track unknown = new Track("Unknown", "Unknown", 100, "Pop", Year.of(2000));
            assertTrue(playbackList.getUpNextQueue(unknown).isEmpty());
        }

        @Test
        void getUpNextQueueFromNullTrackReturnsEmptyList() {
            assertTrue(playbackList.getUpNextQueue(null).isEmpty());
        }
    }

    @Nested
    class WhenShufflingQueue {

        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        @Test
        void shuffleIsInactiveByDefault() {
            assertFalse(playbackList.isShuffleActive());
        }

        @Test
        void setShuffleTrueActivatesShuffleMode() {
            playbackList.setShuffle(true, trk1);

            assertTrue(playbackList.isShuffleActive());
        }

        @Test
        void setShuffleFalseDeactivatesShuffleMode() {
            playbackList.setShuffle(true, trk1);
            playbackList.setShuffle(false, trk1);

            assertFalse(playbackList.isShuffleActive());
        }

        @Test
        void enablingShuffleKeepsCurrentTrackAtQueueHead() {
            playbackList.setShuffle(true, trk2);

            assertNull(playbackList.getPreviousTrack(trk2));
        }

        @Test
        void shuffledUpNextQueueExcludesCurrentTrackAndKeepsRemainingTracks() {
            playbackList.setShuffle(true, trk2);

            ArrayList<Track> upNext = new ArrayList<>(playbackList.getUpNextQueue(trk2));

            assertEquals(2, upNext.size());
            assertFalse(upNext.contains(trk2));
            assertTrue(upNext.contains(trk1));
            assertTrue(upNext.contains(trk3));
        }

        @Test
        void disablingShuffleRestoresOriginalQueueOrderForNavigation() {
            playbackList.setShuffle(true, trk2);
            playbackList.setShuffle(false, trk2);

            assertEquals(trk3, playbackList.getNextTrack(trk2));
        }
    }


    @Nested
    class WhenRepeatModeIsActive {

        @BeforeEach
        void load() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
        }

        @Test
        void repeatPlaylistGoesBackToFirstTrack() {
            playbackService.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

            playbackService.playNext();
            playbackService.playNext();

            playbackService.playNext();

            assertEquals(trk1, playbackService.getCurrentTrack());
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        @Test
        void repeatSingleTrackRestartsSameTrack() {
            playbackService.setRepeatMode(RepeatMode.REPEAT_ONE);

            Track currentTrack = playbackService.getCurrentTrack();

            playbackService.playNext();

            assertEquals(currentTrack, playbackService.getCurrentTrack());
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        @Test
        void repeatModeSequenceIsCorrect() {
            assertEquals(RepeatMode.OFF, playbackService.getRepeatMode());

            playbackService.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);
            assertEquals(RepeatMode.REPEAT_PLAYLIST, playbackService.getRepeatMode());

            playbackService.setRepeatMode(RepeatMode.REPEAT_ONE);
            assertEquals(RepeatMode.REPEAT_ONE, playbackService.getRepeatMode());

            playbackService.setRepeatMode(RepeatMode.OFF);
            assertEquals(RepeatMode.OFF, playbackService.getRepeatMode());
        }

        @Test
        void changingRepeatModeDoesNotInterruptPlayback() {
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
            Track playingTrack = playbackService.getCurrentTrack();

            playbackService.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
            assertEquals(playingTrack, playbackService.getCurrentTrack());

            playbackService.setRepeatMode(RepeatMode.REPEAT_ONE);

            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
            assertEquals(playingTrack, playbackService.getCurrentTrack());
        }
    }





    @Nested
    class WhenSkippingInPlaybackService {

        @BeforeEach
        void load() {
            playbackService.loadSourceFrom(
                new ArrayList<>(Arrays.asList(trk1, trk2, trk3)), trk1
            );
        }

        @Test
        void playNextFromFirstMovesToSecond() {
            playbackService.playNext();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        @Test
        void playNextFromMiddleMovesToThird() {
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(trk3, playbackService.getCurrentTrack());
        }

        @Test
        void playNextFromLastStopsPlayback() {
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(PlaybackState.STOPPED, playbackService.getCurrentState());
        }

        @Test
        void playPreviousFromLastMovesToSecond() {
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playPrevious();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        @Test
        void playPreviousFromSecondMovesToFirst() {
            playbackService.playNext();
            playbackService.playPrevious();
            assertEquals(trk1, playbackService.getCurrentTrack());
        }

        @Test
        void playPreviousFromFirstDoesNothing() {
            playbackService.playPrevious();
            assertEquals(trk1, playbackService.getCurrentTrack());
        }
    }

    @Nested
    class WhenLoadingPlaylist {

        @Test
        void populatedPlaylistLoadsQueueAndStartsFromFirst() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(trk1, playbackService.getCurrentTrack());
        }

        @Test
        void populatedPlaylistLoadsAllTracksInQueue() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(3, playbackService.getQueue().getTrackCount());
        }

        @Test
        void populatedPlaylistSetsStateToPlaying() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        @Test
        void emptyPlaylistDoesNotChangeCurrentTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(trk1, playbackService.getCurrentTrack());
           
        }
    }

    @Nested
    class WhenOverwritingQueue {

        @Test
        void newPlaylistOverwritesPreviousQueue() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));

            Playlist newPlaylist = new Playlist("New Playlist", new ArrayList<>());
            newPlaylist.addTrack(trk3);
            playbackService.loadSource(new ArrayList<>(newPlaylist.getTracks()));

            assertEquals(1, playbackService.getQueue().getTrackCount());
        }

        @Test
        void newPlaylistStartsFromItsFirstTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));

            Playlist newPlaylist = new Playlist("New Playlist", new ArrayList<>());
            newPlaylist.addTrack(trk3);
            playbackService.loadSource(new ArrayList<>(newPlaylist.getTracks()));

            assertEquals(trk3, playbackService.getCurrentTrack());
        }
    }

    @Nested
    class WhenAdvancingAutomatically {

        @Test
        void playNextAdvancesToSecondTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        @Test
        void playNextAdvancesToThirdTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(trk3, playbackService.getCurrentTrack());
        }

        @Test
        void playNextFromLastStopsPlayback() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(PlaybackState.STOPPED, playbackService.getCurrentState());
        }

        @Test
        void playNextFromLastSetsCurrentTrackToNull() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playNext();
            assertNull(playbackService.getCurrentTrack());
        }
    }
}