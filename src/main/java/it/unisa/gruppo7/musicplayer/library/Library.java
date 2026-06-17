package it.unisa.gruppo7.musicplayer.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.PersistenceService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.io.File;
import java.io.IOException;
import java.time.Year;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents the primary music library registry, managing the collection of all available tracks.
 * It enforces track uniqueness using a case-insensitive title and author signature combination.
 * Implements the Singleton pattern and provides persistence capabilities via JSON storage.
 *
 * @author Francesco Lemmo
 */
public class Library extends TrackCollection implements PersistenceService {

    private static volatile Library instance;
    private static final String DEFAULT_PATH = "data/track-library.json";

    private transient HashSet<String> signatures = new HashSet<>();

    /**
     * Private constructor initializing the track collection path and loading existing data.
     */
    private Library() {
        super(DEFAULT_PATH, new HashSet<>());
        this.load();
    }

    // --- Methods ---

    /**
     * Generates a unique, case-insensitive string signature for a track using its title and author.
     *
     * @param track The track to generate a signature for.
     * @return A formatted string signature tracking uniqueness.
     */
    private String generateSignature(Track track) {
        return track.getTitle().toLowerCase() + "|" + track.getAuthor().toLowerCase();
    }

    /**
     * Retrieves the unique singleton instance of the Library.
     * If the instance does not exist, it is lazily initialized.
     *
     * @return The active singleton Library instance.
     */
    public static synchronized Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }

        return instance;
    }

    /**
     * Adds a track to the library if it does not already exist.
     * Uniqueness is validated via a signature generated from the track's title and author.
     *
     * @param t The track to be added.
     * @return true if the track was successfully added.
     * @throws IllegalArgumentException If a track with the same title and author already exists in the library.
     */
    @Override
    public synchronized boolean addTrack(Track t) {
        String signature = this.generateSignature(t);
        if (!this.signatures.add(signature)){
            throw new IllegalArgumentException("Track already in library");
        }
        return super.addTrack(t);
    }

    /**
     * Removes a track from the library and clears its uniqueness signature.
     *
     * @param track The track to be removed.
     * @return true if the track was successfully removed.
     */
    @Override
    public synchronized boolean removeTrack(Track track) {
        this.signatures.remove(generateSignature(track));
        return super.removeTrack(track);
    }

    /**
     * Finds and retrieves a track from the library using its unique identifier.
     *
     * @param id The UUID of the track to find.
     * @return The matching Track object, or null if no track matches the given ID.
     */
    public Track getTrackById(UUID id) {
        return tracks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the most played tracks, ordered by descending play count.
     * Tracks that have never been played are excluded.
     *
     * @param limit the maximum number of tracks to return.
     * @return the most played tracks, at most {@code limit} entries.
     */
    public List<Track> getMostPlayed(int limit) {
        return tracks.stream()
                .filter(track -> track.getPlayCount() > 0)
                .sorted(Comparator.comparingInt(Track::getPlayCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Modifies the metadata of an existing track in the library.
     * If the updated title and author generate a signature conflict with an existing track,
     * the modification is automatically rolled back to preserve library uniqueness constraints.
     *
     * @param t                  The track instance to modify.
     * @param newTitle           The new title to assign.
     * @param newAuthor          The new author to assign.
     * @param newDuration        The new duration in seconds.
     * @param newGenre           The new genre description.
     * @param newPublicationYear The new release year.
     * @return true if the modification succeeded without signature conflicts, false otherwise.
     * @throws IllegalArgumentException If the new arguments fail domain validation checks.
     */
    public synchronized boolean modifyTrackInLibrary(Track t, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear) {
        String oldTitle = t.getTitle();
        String oldAuthor = t.getAuthor();
        int oldDuration = t.getDuration();
        String oldGenre = t.getGenre();
        Year oldPublicationYear = t.getPublicationYear();

        this.signatures.remove(generateSignature(t));
        try {
            t.modifyTrack(newTitle, newAuthor, newDuration, newGenre, newPublicationYear);

            boolean success = this.signatures.add(generateSignature(t));
            // if the modified signature is already in the library, do a rollback of the modification
            if (!success) {
                t.modifyTrack(oldTitle, oldAuthor, oldDuration, oldGenre, oldPublicationYear);
                this.signatures.add(generateSignature(t));
            }

            return success;

        } catch (IllegalArgumentException e) {
            this.signatures.add(generateSignature(t));

            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Purges all tracks and uniqueness signatures currently held in the library memory.
     */
    public synchronized void clearLibrary() {
        this.tracks.clear();
        this.signatures.clear();
    }

    /**
     * Serializes and saves the current state of the track library to a JSON file.
     */
    @Override
    public void save() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Take a defensive snapshot under the same lock used by the mutators, then
        // serialize the copy outside the lock to avoid ConcurrentModificationException
        // when a background save races with library mutations on another thread.
        HashSet<Track> snapshot;
        synchronized (this) {
            snapshot = new HashSet<>(this.tracks);
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), snapshot);
        } catch (IOException e) {
            System.err.println("Error saving " + path);
            e.printStackTrace();
        }
    }

    /**
     * Deserializes and loads the track library data from the designated JSON storage file.
     * If the target file does not exist, it creates a new empty file.
     */
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
            // Jackson reads the file as a standard List
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

    /**
     * Captures the current contents of the library into an immutable memento,
     * for later restoration on undo.
     *
     * @return a snapshot of the library state.
     */
    public LibraryMemento snapshot() {
        return new LibraryMemento(this.tracks);
    }

    /**
     * Restores the library to a previously captured state, replacing the current
     * tracks and rebuilding the uniqueness signatures from them.
     *
     * @param memento the state to restore; ignored if null.
     */
    public void restore(LibraryMemento memento) {
        if (memento == null) return;
        this.tracks.clear();
        this.signatures.clear();
        this.tracks.addAll(memento.getTracks());
        for (Track t : this.tracks) {
            this.signatures.add(generateSignature(t));
        }
    }

    /**
     * Returns a string representation of the track library, formatting each contained track item.
     *
     * @return A formatted detail text listing all current tracks.
     */
    @Override
    public String toString() {
        return "Track Library:\n" +
                this.tracks.stream()
                        .map(String::valueOf)
                        .map(p -> " - " + p)
                        .collect(Collectors.joining(System.lineSeparator()));
    }
}