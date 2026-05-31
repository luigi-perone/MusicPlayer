package it.unisa.gruppo7.musicplayer.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;


/**
 * This abstract class is the base Track collection, extended in TrackLibrary and Playlist
 *
 * @author francescoLemmo
 */

public abstract class TrackCollection {

    protected String path = null;
    protected Collection<Track> tracks;

    // Safety lock in case the file is corrupted (e.g. wrong format, wrong arguments)
    protected boolean isCorrupted = false;

    /**
     *
     * @param path              the specified file path (e.g., "library.json") where the collection's data will be stored.
     * @param emptyCollection   An empty collection instance (like a HashSet or ArrayList) used to hold the tracks in memory.
     */
    public TrackCollection(String path, Collection<Track> emptyCollection) {
        this.path = path;
        this.tracks = emptyCollection;
    }


    public void addTrack(Track t) {
        this.tracks.add(t);
    }

    public void removeTrack(Track t) {
        this.tracks.remove(t);
    }

    public int getTrackCount() {
        return this.tracks.size();
    }

    // Creates a serialized file (JSON) of the collection in the specified path
    public void save() {

        // If the file is corrupted do not overwrite the file
        if (isCorrupted){
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), this.tracks);
        } catch (IOException e) {
            System.err.println("Error saving " + path);
            e.printStackTrace();
        }
    }

    public void load() {
        File file = new File(path);

        // If the file does not exist, the loading process is interrupted
        if (!file.exists()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        try {
            // Jackson to reads the file as a standard List
            List<Track> loadedTracks = mapper.readValue(file, new TypeReference<List<Track>>() {});

            // Clear out whatever is currently in memory
            this.tracks.clear();

            // Add the loaded tracks into the specific collection
            this.tracks.addAll(loadedTracks);

            // Release the safety lock
            this.isCorrupted = false;

        } catch (Exception e) { // Catch IllegalArgumentException and IOException
            // Enable the safety lock
            this.isCorrupted = true;
            System.err.println("Corrupted File");

            System.err.println("Error loading " + path);
            e.printStackTrace();
        }
    }
}
