package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Arrays;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the playback queue ({@link PlaybackList}) and {@link PlaybackService}.
 * Covers queue population, next/previous navigation, the up-next view, shuffle,
 * repeat modes, manual skipping, playlist loading/overwriting and automatic advance.
 */
class PlaybackTest {

    private PlaybackList playbackList;
    private PlaybackService playbackService;
    private Playlist playlist;
    private Track trk1;
    private Track trk2;
    private Track trk3;

    /** Builds a fresh queue/service and a three-track playlist before each test. */
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
    }

    /** Tests behaviour of an empty playback queue. */
    @Nested
    class WhenPlaybackListIsEmpty {

        /** Verifies that the next track is null on an empty queue. */
        @Test
        void getNextTrackReturnsNull() {
            assertNull(playbackList.getNextTrack());
        }

        /** Verifies that the previous track is null on an empty queue. */
        @Test
        void getPreviousTrackReturnsNull() {
            assertNull(playbackList.getPreviousTrack());
        }
    }

    /** Tests how loading and appending tracks populate the queue. */
    @Nested
    class WhenLoadingTracks {

        /** Verifies that loading tracks populates the queue. */
        @Test
        void loadTracksPopulatesQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
            assertEquals(3, playbackList.getTrackCount());
        }

        /** Verifies that loading new tracks overwrites the previous queue. */
        @Test
        void loadTracksOverwritesPreviousQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2)));
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk3)));
            assertEquals(1, playbackList.getTrackCount());
        }

        /** Verifies that appending tracks adds them to the existing queue. */
        @Test
        void appendTracksAddsToExistingQueue() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1)));
            playbackList.appendTracks(new ArrayList<>(Arrays.asList(trk2, trk3)));
            assertEquals(3, playbackList.getTrackCount());
        }
    }

    /** Tests forward navigation through the queue. */
    @Nested
    class WhenNavigatingNext {

        /** Loads three tracks and positions the cursor at the first one. */
        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
            playbackList.setCurrentIndex(0);
        }

        /** Verifies that next from the first track returns the second. */
        @Test
        void getNextTrackFromFirstReturnsSecond() {
            assertEquals(trk2, playbackList.getNextTrack());
        }

        /** Verifies that next from the middle track returns the third. */
        @Test
        void getNextTrackFromMiddleReturnsThird() {
            playbackList.setCurrentIndex(1);
            assertEquals(trk3, playbackList.getNextTrack());
        }

        /** Verifies that next from the last track returns null. */
        @Test
        void getNextTrackFromLastReturnsNull() {
            playbackList.setCurrentIndex(2);
            assertNull(playbackList.getNextTrack());
        }

        /** Verifies that next from an out-of-range cursor returns null. */
        @Test
        void getNextTrackFromUnknownTrackReturnsNull() {
            playbackList.setCurrentIndex(2);
            assertNull(playbackList.getNextTrack());
        }
    }

    /** Tests backward navigation through the queue. */
    @Nested
    class WhenNavigatingPrevious {

        /** Loads three tracks and positions the cursor at the last one. */
        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
            playbackList.setCurrentIndex(2);
        }

        /** Verifies that previous from the last track returns the second. */
        @Test
        void getPreviousTrackFromLastReturnsSecond() {
            assertEquals(trk2, playbackList.getPreviousTrack());
        }

        /** Verifies that previous from the middle track returns the first. */
        @Test
        void getPreviousTrackFromMiddleReturnsFirst() {
            playbackList.setCurrentIndex(1);
            assertEquals(trk1, playbackList.getPreviousTrack());
        }

        /** Verifies that previous from the first track returns null. */
        @Test
        void getPreviousTrackFromFirstReturnsNull() {
            playbackList.setCurrentIndex(0);
            assertNull(playbackList.getPreviousTrack());
        }

        /** Verifies that previous from an out-of-range cursor returns null. */
        @Test
        void getPreviousTrackFromUnknownTrackReturnsNull() {
            playbackList.setCurrentIndex(0);
            assertNull(playbackList.getPreviousTrack());
        }
    }

    /** Tests the up-next view of the queue. */
    @Nested
    class WhenReadingUpNextQueue {

        /** Loads three tracks before each test. */
        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        /** Verifies that the up-next queue from the first track lists the following tracks. */
        @Test
        void getUpNextQueueFromFirstReturnsFollowingTracks() {
            assertEquals(
                Arrays.asList(trk2, trk3),
                playbackList.getUpNextQueue()
            );
        }

        /** Verifies that the up-next queue from the last track is empty. */
        @Test
        void getUpNextQueueFromLastReturnsEmptyList() {
            playbackList.setCurrentIndex(2);
            assertTrue(playbackList.getUpNextQueue().isEmpty());
        }

        /** Verifies that the up-next queue from an out-of-range cursor is empty. */
        @Test
        void getUpNextQueueFromUnknownTrackReturnsEmptyList() {
            playbackList.setCurrentIndex(-1);
            assertTrue(playbackList.getUpNextQueue().isEmpty());
        }

        /** Verifies that the up-next queue with no current track is empty. */
        @Test
        void getUpNextQueueFromNullTrackReturnsEmptyList() {
            playbackList.setCurrentIndex(-1);
            assertTrue(playbackList.getUpNextQueue().isEmpty());
        }
    }

    /** Tests enabling, disabling and effects of shuffle mode. */
    @Nested
    class WhenShufflingQueue {

        /** Loads three tracks before each test. */
        @BeforeEach
        void load() {
            playbackList.loadTracks(new ArrayList<>(Arrays.asList(trk1, trk2, trk3)));
        }

        /** Verifies that shuffle is inactive by default. */
        @Test
        void shuffleIsInactiveByDefault() {
            assertFalse(playbackList.isShuffleActive());
        }

        /** Verifies that enabling shuffle activates shuffle mode. */
        @Test
        void setShuffleTrueActivatesShuffleMode() {
            playbackList.setShuffle(true, trk1);

            assertTrue(playbackList.isShuffleActive());
        }

        /** Verifies that disabling shuffle deactivates shuffle mode. */
        @Test
        void setShuffleFalseDeactivatesShuffleMode() {
            playbackList.setShuffle(true, trk1);
            playbackList.setShuffle(false, trk1);

            assertFalse(playbackList.isShuffleActive());
        }

        /** Verifies that enabling shuffle keeps the current track at the head of the queue. */
        @Test
        void enablingShuffleKeepsCurrentTrackAtQueueHead() {
            playbackList.setShuffle(true, trk2);

            assertNull(playbackList.getPreviousTrack());
        }

        /** Verifies that the shuffled up-next queue excludes the current track but keeps the rest. */
        @Test
        void shuffledUpNextQueueExcludesCurrentTrackAndKeepsRemainingTracks() {
            playbackList.setShuffle(true, trk2);

            ArrayList<Track> upNext = new ArrayList<>(playbackList.getUpNextQueue());

            assertEquals(2, upNext.size());
            assertFalse(upNext.contains(trk2));
            assertTrue(upNext.contains(trk1));
            assertTrue(upNext.contains(trk3));
        }

        /** Verifies that disabling shuffle restores the original order for navigation. */
        @Test
        void disablingShuffleRestoresOriginalQueueOrderForNavigation() {
            playbackList.setShuffle(true, trk2);
            playbackList.setShuffle(false, trk2);

            assertEquals(trk3, playbackList.getNextTrack());
        }
    }


    /** Tests playback behaviour under the different repeat modes. */
    @Nested
    class WhenRepeatModeIsActive {

        /** Loads the playlist into the playback service before each test. */
        @BeforeEach
        void load() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
        }

        /** Verifies that repeat-playlist wraps back to the first track after the last. */
        @Test
        void repeatPlaylistGoesBackToFirstTrack() {
            playbackService.setRepeatMode(RepeatMode.REPEAT_PLAYLIST);

            playbackService.playNext();
            playbackService.playNext();

            playbackService.playNext();

            assertEquals(trk1, playbackService.getCurrentTrack());
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        /** Verifies that repeat-one restarts the same track on advance. */
        @Test
        void repeatSingleTrackRestartsSameTrack() {
            playbackService.setRepeatMode(RepeatMode.REPEAT_ONE);

            Track currentTrack = playbackService.getCurrentTrack();

            playbackService.playNext();

            assertEquals(currentTrack, playbackService.getCurrentTrack());
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        /** Verifies that repeat modes can be set and read back correctly. */
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

        /** Verifies that changing the repeat mode does not interrupt the current playback. */
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




    /** Tests manual next/previous skipping through the playback service. */
    @Nested
    class WhenSkippingInPlaybackService {

        /** Loads three tracks starting from the first one before each test. */
        @BeforeEach
        void load() {
            playbackService.loadSourceFrom(
                new ArrayList<>(Arrays.asList(trk1, trk2, trk3)), trk1
            );
        }

        /** Verifies that next from the first track moves to the second. */
        @Test
        void playNextFromFirstMovesToSecond() {
            playbackService.playNext();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        /** Verifies that next from the middle track moves to the third. */
        @Test
        void playNextFromMiddleMovesToThird() {
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(trk3, playbackService.getCurrentTrack());
        }

        /** Verifies that next from the last track stops playback. */
        @Test
        void playNextFromLastStopsPlayback() {
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(PlaybackState.STOPPED, playbackService.getCurrentState());
        }

        /** Verifies that previous from the last track moves to the second. */
        @Test
        void playPreviousFromLastMovesToSecond() {
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playPrevious();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        /** Verifies that previous from the second track moves to the first. */
        @Test
        void playPreviousFromSecondMovesToFirst() {
            playbackService.playNext();
            playbackService.playPrevious();
            assertEquals(trk1, playbackService.getCurrentTrack());
        }

        /** Verifies that previous from the first track does nothing. */
        @Test
        void playPreviousFromFirstDoesNothing() {
            playbackService.playPrevious();
            assertEquals(trk1, playbackService.getCurrentTrack());
        }
    }

    /** Tests loading a playlist into the playback service. */
    @Nested
    class WhenLoadingPlaylist {

        /** Verifies that loading a populated playlist queues it and starts from the first track. */
        @Test
        void populatedPlaylistLoadsQueueAndStartsFromFirst() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(trk1, playbackService.getCurrentTrack());
        }

        /** Verifies that loading a populated playlist queues all of its tracks. */
        @Test
        void populatedPlaylistLoadsAllTracksInQueue() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(3, playbackService.getQueue().getTrackCount());
        }

        /** Verifies that loading a populated playlist sets the state to playing. */
        @Test
        void populatedPlaylistSetsStateToPlaying() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(PlaybackState.PLAYING, playbackService.getCurrentState());
        }

        /** Verifies that loading does not unexpectedly change the current track. */
        @Test
        void emptyPlaylistDoesNotChangeCurrentTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            assertEquals(trk1, playbackService.getCurrentTrack());

        }
    }

    /** Tests overwriting the current queue with a new playlist. */
    @Nested
    class WhenOverwritingQueue {

        /** Verifies that loading a new playlist overwrites the previous queue. */
        @Test
        void newPlaylistOverwritesPreviousQueue() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));

            Playlist newPlaylist = new Playlist("New Playlist", new ArrayList<>());
            newPlaylist.addTrack(trk3);
            playbackService.loadSource(new ArrayList<>(newPlaylist.getTracks()));

            assertEquals(1, playbackService.getQueue().getTrackCount());
        }

        /** Verifies that the new playlist starts from its first track. */
        @Test
        void newPlaylistStartsFromItsFirstTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));

            Playlist newPlaylist = new Playlist("New Playlist", new ArrayList<>());
            newPlaylist.addTrack(trk3);
            playbackService.loadSource(new ArrayList<>(newPlaylist.getTracks()));

            assertEquals(trk3, playbackService.getCurrentTrack());
        }
    }

    /** Tests automatic advance through the queue. */
    @Nested
    class WhenAdvancingAutomatically {

        /** Verifies that advancing once moves to the second track. */
        @Test
        void playNextAdvancesToSecondTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            assertEquals(trk2, playbackService.getCurrentTrack());
        }

        /** Verifies that advancing twice moves to the third track. */
        @Test
        void playNextAdvancesToThirdTrack() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(trk3, playbackService.getCurrentTrack());
        }

        /** Verifies that advancing past the last track stops playback. */
        @Test
        void playNextFromLastStopsPlayback() {
            playbackService.loadSource(new ArrayList<>(playlist.getTracks()));
            playbackService.playNext();
            playbackService.playNext();
            playbackService.playNext();
            assertEquals(PlaybackState.STOPPED, playbackService.getCurrentState());
        }

        /** Verifies that advancing past the last track clears the current track. */
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
