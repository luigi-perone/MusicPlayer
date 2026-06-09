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


    // shuffled list and mode state
    private List<Track> shuffledTracks;
    private boolean isShuffleActive;


    /**
     * Constructs a new empty playback list.
     */
    public PlaybackList() {
        super(DEFAULT_PATH, new ArrayList<>());
        this.shuffledTracks = new ArrayList<>();
        this.isShuffleActive = false;
    }

    private List<Track> getActiveList() {
        if (isShuffleActive()) {
            return this.shuffledTracks;
        }
        else {
            return (List<Track>) this.tracks;
        }
    }

    /**
     * Returns the next track relative to the current one.
     *
     * @param current The track currently playing.
     * @return The next track, or null if the list is empty, the track is not present, or it is the last one.
     */
    public Track getNextTrack(Track current){
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) return null;

        Iterator<Track> iterator = trackList.iterator();

        while (iterator.hasNext()) {
            Track t = iterator.next();

            if (t.equals(current)) {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Returns the previous track relative to the current one.
     *
     * @param current The track currently playing.
     * @return The previous track, or null if the list is empty, the track is not present, or it is the first one.
     */
    public Track getPreviousTrack(Track current) {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) return null;

        Track previous = null;
        for (Track t : trackList) {
            if (t.equals(current)) {
                return previous;
            }
            previous = t;
        }
        return null;
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
    }

    public Track getFirstTrack() {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty()) {
            return null;
        }

        return trackList.get(0);

    }


    public List<Track> getUpNextQueue(Track current) {
        List<Track> trackList = getActiveList();

        if (trackList == null || trackList.isEmpty() || current == null) {
            return new ArrayList<>();
        }

        // get the index of the current track
        int currentIndex = trackList.indexOf(current);

        // if the track is not in the list, or it is in the last position, return an empty list
        if (currentIndex == -1 || currentIndex >= trackList.size() - 1) {
            return new ArrayList<>();
        }

        // return a list with only the up next tracks
        return new ArrayList<>(trackList.subList(currentIndex + 1, trackList.size()));
    }

    /**
     * Returns the current shuffle mode state.
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

    private void shuffleTracks(Track currentTrack) {
        shuffledTracks = new ArrayList<>(this.tracks);
        Collections.shuffle(shuffledTracks);

        // if a track is playing, it is positioned at the head of the queue
        if (currentTrack != null && shuffledTracks.contains(currentTrack)) {
            shuffledTracks.remove(currentTrack);
            shuffledTracks.add(0, currentTrack);
        }
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

    @Override
    public void onTrackEdit(Track track) {

    }
}
