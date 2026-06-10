package it.unisa.gruppo7.musicplayer.playback;

import java.util.*;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Represents the playback queue of the music player.
 * Extends {@link TrackCollection} and observes track changes via {@link TrackObserver}.
 */
public class PlaybackList extends TrackCollection implements TrackObserver {
    private static final String DEFAULT_PATH = null;

    /** A separate list maintaining the randomized order of tracks when shuffle mode is active. */
    private List<Track> shuffledTracks;

    /** Flag indicating whether the playback queue is currently operating in shuffle mode. */
    private boolean isShuffleActive;
    private int currentIndex = -1;


    /**
     * Constructs a new empty playback list.
     */
    public PlaybackList() {
        super(DEFAULT_PATH, new ArrayList<>());
        this.shuffledTracks = new ArrayList<>();
        this.isShuffleActive = false;
    }

    /**
     * Returns the currently active list of tracks based on the shuffle state.
     * * @return The shuffled list if shuffle is active, otherwise the canonical track list.
     */
    private List<Track> getActiveList() {
        if (isShuffleActive()) {
            return this.shuffledTracks;
        }
        else {
            return (List<Track>) this.tracks;
        }
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public Track getNextTrack(){
        List<Track> trackList = getActiveList();
        if (trackList == null || trackList.isEmpty()) return null;
        if (currentIndex < 0 || currentIndex >= trackList.size() - 1) return null;
        System.out.println(currentIndex);
        this.setCurrentIndex(currentIndex + 1);
        return trackList.get(currentIndex);
    }

    public Track getPreviousTrack() {
        List<Track> trackList = getActiveList();
        if (trackList == null || trackList.isEmpty()) return null;
        if (currentIndex <= 0) return null;
        System.out.println(currentIndex);
        this.setCurrentIndex(currentIndex - 1);
        return trackList.get(currentIndex - 1);
    }

    /**
     * Replaces the tracks in the playback list with the provided ones.
     *
     * @param tracks The new list of tracks to load.
     */
    public void loadTracks(List<Track> tracks) {
        this.clear();
        this.tracks.addAll(tracks);

        if (isShuffleActive) {
            shuffleTracks(null);
        }
    }

    /**
     * Appends the specified tracks to the end of the playback list.
     *
     * @param tracks The list of tracks to append.
     */
    public void appendTracks(List<Track> tracks) {
        this.tracks.addAll(tracks);

        System.out.println(this.tracks.size());
        // if the playback is in shuffle mode, append to the shuffled track list
        if (isShuffleActive) {
            shuffledTracks.addAll(tracks);
        }
    }

    /**
     * Removes all tracks from the playback list.
     */
    public void clear() {
        this.tracks.clear();
        this.shuffledTracks.clear();
        this.currentIndex = -1;
    }

    /**
     * Retrieves the first track in the active playback list.
     * * @return The first track, or null if the list is empty.
     */
    public Track getFirstTrack() {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) {
            return null;
        }

        return trackList.get(0);

    }


    public List<Track> getUpNextQueue() {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty() || currentIndex == -1) {
            return new ArrayList<>();
        }

        // if the track is not in the list, or it is in the last position, return an empty list
        if (currentIndex == -1 || currentIndex >= trackList.size() - 1) {
            return new ArrayList<>();
        }

        // return a list with only the up next tracks
        return new ArrayList<>(trackList.subList(currentIndex + 1, trackList.size()));
    }

    /**
     * Returns the current shuffle mode state.
     * * @return true if shuffle is active, false otherwise.
     */
    public boolean isShuffleActive() {
        return isShuffleActive;
    }

    /**
     * Sets a new shuffle mode state.
     *
     * @param shuffleState The new shuffle state.
     * @param currentTrack The current track playing.
     */
    public void setShuffle(boolean shuffleState, Track currentTrack) {
        this.isShuffleActive = shuffleState;
        if (shuffleState) {
            shuffleTracks(currentTrack);
        } else {
            shuffledTracks.clear();

        }
    }

    /**
     * Generates a randomized version of the current track list.
     * If a track is currently playing, it is moved to the head of the shuffled list.
     * * @param currentTrack The currently playing track, or null.
     */
    private void shuffleTracks(Track currentTrack) {
        shuffledTracks = new ArrayList<>(this.tracks);
        Collections.shuffle(shuffledTracks);

        // if a track is playing, it is positioned at the head of the queue
        if (currentTrack != null && shuffledTracks.contains(currentTrack)) {
            shuffledTracks.remove(currentTrack);
            shuffledTracks.add(0, currentTrack);
        }
    }

    // In PlaybackList.java

    /**
     * Inserts a track at a random position within the shuffled queue,
     * after the current track (index 0 is reserved for the playing track).
     * Falls back to appending at the end if the shuffled list is empty.
     *
     * @param track The track to insert.
     */
    public void insertTrackAtRandom(Track track) {
        if (shuffledTracks.isEmpty()) {
            shuffledTracks.add(track);
            return;
        }
        // Insert anywhere after position 0 (position 0 = currently playing)
        int insertIndex = shuffledTracks.size() == 1
                ? 1
                : 1 + new Random().nextInt(shuffledTracks.size() - 1);
        shuffledTracks.add(insertIndex, track);
    }

    /**
     * Adds a track to the canonical list without triggering the shuffle-append side-effect of appendTracks.
     *
     * @param track The track to add.
     */
    void addToCanonicalList(Track track) {
        this.tracks.add(track);
    }

    /**
     * Removes a track from both the main list and the shuffled list.
     * Overrides the base class to keep the two lists consistent.
     *
     * @param track The track to remove.
     * @return true if the main list contained the track.
     */
    @Override
    public boolean removeTrack(Track track) {
        shuffledTracks.remove(track);
        return super.removeTrack(track);
    }


    /**
     * Handles the track deletion event by removing it from the list if present.
     *
     * @param track The track that was deleted.
     */
    @Override
    public void onTrackDeleted(Track track) {
        if (this.tracks != null) {
            this.tracks.remove(track);
        }
    }

    /**
     * Handles the track edit event.
     * Currently, a no-op in this context as track metadata changes do not inherently affect queue order.
     * * @param track The track that was edited.
     */
    @Override
    public void onTrackEdit(Track track) {

    }
}