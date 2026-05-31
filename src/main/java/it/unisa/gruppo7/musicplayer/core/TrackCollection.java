package it.unisa.gruppo7.musicplayer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.io.File;
import java.io.IOException;
import java.util.Collection;



/**
 * This abstract class is the base Track collection, extended in TrackLibrary and Playlist
 *
 * @author francescoLemmo
 */

public abstract class TrackCollection {

    protected Collection<Track> tracks;
    protected String path = null;

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
    public void saveLibrary() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), this.tracks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
