package it.unisa.gruppo7.musicplayer.core;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;

/**
 * Abstract base class representing a generic collection of music tracks.
 * It provides fundamental operations to manage tracks in memory and holds a
 * reference file path specification used for persistence features.
 *
 * @author Francesco Lemmo
 */
public abstract class TrackCollection {

    protected String path = null;
    protected Collection<Track> tracks;

    /**
     * Constructs a new TrackCollection with a designated storage path and an underlying collection implementation.
     *
     * @param path              The specific file path (e.g., "library.json") where the collection's data will be stored.
     * @param emptyCollection   An empty collection instance (such as a HashSet or ArrayList) used to hold the tracks in memory.
     */
    public TrackCollection(String path, Collection<Track> emptyCollection) {
        this.path = path;
        this.tracks = emptyCollection;
    }

    /**
     * Adds a track to the underlying memory collection.
     *
     * @param t The track to be added.
     * @return true if the collection changed as a result of the call.
     */
    public boolean addTrack(Track t) {
        return this.tracks.add(t);
    }

    /**
     * Removes a track from the underlying memory collection.
     *
     * @param t The track to be removed.
     * @return true if the collection contained the specified track element.
     */
    public boolean removeTrack(Track t) {
        return this.tracks.remove(t);
    }

    /**
     * Retrieves the total number of tracks currently contained in this collection.
     *
     * @return The size of the track collection.
     */
    public int getTrackCount() {
        return this.tracks.size();
    }

    /**
     * Returns the inner data collection backing this tracks container module.
     *
     * @return A Collection view of Track objects.
     */
    public Collection<Track> getTracks() {
        return tracks;
    }

    public void clear() {
        this.tracks.clear();
    }
}