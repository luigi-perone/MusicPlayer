package it.unisa.gruppo7.musicplayer.playback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Represents the playback queue of the music player.
 * Extends {@link TrackCollection} and observes track changes via {@link TrackObserver}.
 */
public class PlaybackList extends TrackCollection implements TrackObserver {
    private static final String DEFAULT_PATH = null;

    /**
     * Constructs a new empty playback list.
     */
    public PlaybackList() {
        super(DEFAULT_PATH, new ArrayList<>());
    }

    /**
     * Returns the next track relative to the current one.
     *
     * @param current The track currently playing.
     * @return The next track, or null if the list is empty, the track is not present, or it is the last one.
     */
    public Track getNextTrack(Track current) {
        if (this.tracks == null || this.tracks.isEmpty()) return null;

        Iterator<Track> iterator = this.tracks.iterator();

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
        if (this.tracks == null || this.tracks.isEmpty()) return null;

        Track previous = null;
        for (Track t : this.tracks) {
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
    }

    /**
     * Appends the specified tracks to the end of the playback list.
     *
     * @param tracks The list of tracks to append.
     */
    public void appendTracks(List<Track> tracks) {
        this.tracks.addAll(tracks);
    }

    /**
     * Removes all tracks from the playback list.
     */
    public void clear() {
        this.tracks.clear();
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
}