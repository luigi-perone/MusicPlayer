package it.unisa.gruppo7.musicplayer.core;

import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.Collection;


/**
 * This abstract class is the base Track collection, extended in TrackLibrary and Playlist
 *
 * @author francescoLemmo
 */

public abstract class TrackCollection {

    protected String path = null;
    protected Collection<Track> tracks;


    /**
     *
     * @param path              the specified file path (e.g., "library.json") where the collection's data will be stored.
     * @param emptyCollection   An empty collection instance (like a HashSet or ArrayList) used to hold the tracks in memory.
     */
    public TrackCollection(String path, Collection<Track> emptyCollection) {
        this.path = path;
        this.tracks = emptyCollection;
    }


    public boolean addTrack(Track t) {
        return this.tracks.add(t);
    }

    public boolean removeTrack(Track t) {
        return this.tracks.remove(t);
    }

    public int getTrackCount() {
        return this.tracks.size();
    }

    public Collection<Track> getTracks() {
        return tracks;
    }
}
