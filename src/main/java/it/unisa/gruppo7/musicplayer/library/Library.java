package it.unisa.gruppo7.musicplayer.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.PersistenceService;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * @author francescoLemmo
 */
public class Library extends TrackCollection implements PersistenceService{

    // pattern singleton
    private static Library instance;
    private static final String DEFAULT_PATH = "src/main/java/it/unisa/gruppo7/musicplayer/library/track-library.json";

    private transient HashSet<String> signatures = new HashSet<>();

    private Library() {
        super(DEFAULT_PATH, new HashSet<>());
        this.load();
    }

    // --- Methods ---

    private String generateSignature(Track track) {
        return track.getTitle().toLowerCase() + "|" + track.getAuthor().toLowerCase();
    }

    public static Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }

        return instance;
    }

    @Override
    public boolean addTrack(Track t) {
        String signature = this.generateSignature(t);
        if (!this.signatures.add(signature)){
            return false;
        }
        return super.addTrack(t);
    }

    @Override
    public boolean removeTrack(Track track) {
        this.signatures.remove(generateSignature(track));
        return super.removeTrack(track);
    }

    public Track getTrackById(UUID id) {
        return tracks.stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .orElse(null);
    }

    @Override
    public void save() {

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

    @Override
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

        } catch (IOException e) {
            System.err.println("Error loading " + path);
            e.printStackTrace();
        }

        this.signatures.clear();
        for (Track t : this.tracks) {
            this.signatures.add(generateSignature(t));
        }
    }

    // -- toString --

    @Override
    public String toString() {
        return "Track Library:\n" +
                this.tracks.stream()
                        .map(String::valueOf)
                        .map(p -> " - " + p)
                        .collect(Collectors.joining(System.lineSeparator()));
    }

}
