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
import java.time.Year;
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
    private static final String DEFAULT_PATH = "data/track-library.json";

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
            throw new IllegalArgumentException("Track already in library");
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

    public boolean modifyTrackInLibrary(Track t, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear) {
        String oldTitle = t.getTitle();
        String oldAuthor = t.getAuthor();
        int oldDuration = t.getDuration();
        String oldGenre = t.getGenre();
        Year oldPublicationYear = t.getPublicationYear();

        this.signatures.remove(generateSignature(t));
        try {
            t.modifyTrack(newTitle, newAuthor, newDuration, newGenre, newPublicationYear);

            boolean success = this.signatures.add(generateSignature(t));
            //if the modified signature is already in the library, do a rollback of the modification
            if (!success) {
                t.modifyTrack(oldTitle, oldAuthor, oldDuration, oldGenre, oldPublicationYear);
                this.signatures.add(generateSignature(t));
            }

            return success;

        } catch (IllegalArgumentException e) {
            this.signatures.add(generateSignature(t));

            System.err.println("Validation Error: " + e.getMessage());
            throw new IllegalArgumentException(e.getMessage());            
        }
    }

    public void clearLibrary() {
        this.tracks.clear();
        this.signatures.clear();
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

        // If the file does not exist, it is created and the loading process is interrupted
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("File created: " + file.getName());
                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
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
