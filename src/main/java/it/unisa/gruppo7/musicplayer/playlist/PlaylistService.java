package it.unisa.gruppo7.musicplayer.playlist;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.library.Library;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import it.unisa.gruppo7.musicplayer.core.PersistenceService;

/**
 * Manages the collection of playlists in the music player,
 * handling creation with name validation and access to the playlists.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlaylistService implements PersistenceService, TrackObserver {
    private static final String DEFAULT_PATH = "data/playlist.json";
    private final String path;
    private final List<Playlist> playlists = new ArrayList<>();
    private final ObjectMapper mapper;

    /**
     * Default constructor that initializes the service with the default file path.
     */
    public PlaylistService() {
        this(DEFAULT_PATH);
    }

    /**
     * Constructor that initializes the service with a custom file path.
     * * @param path the file path where playlists will be saved and loaded from
     */
    public PlaylistService(String path) {
        this.path = path;
        this.mapper = new ObjectMapper();
        this.load();
    }

    /**
     * Creates a new playlist with the given name, if the name is valid and unique.
     *
     * @param name the name of the playlist
     * @return the newly created Playlist object
     * @throws IllegalArgumentException if the name is empty or already in use
     */
    public Playlist createPlaylist(String name) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("A name for the playlist must be provided");
        }

        if (existsByName(name)) {
            throw new IllegalArgumentException("This playlist name already exists");
        }

        Playlist newPlaylist = new Playlist(name, new ArrayList<>());
        playlists.add(newPlaylist);
        save();

        return newPlaylist;
    }

    /**
     * Deletes the given playlist, removing it permanently from the list
     * and persisting the change. Tracks contained in the playlist are left
     * untouched in the library.
     *
     * @param playlist the playlist to delete
     * @return an empty Optional if the deletion is successful, or an Optional
     * containing an error message if the playlist is null or not found.
     */
    public Optional<String> deletePlaylist(Playlist playlist) {
        if (playlist == null)
            return Optional.of("A playlist must be provided");

        boolean removed = playlists.remove(playlist);

        if (!removed)
            return Optional.of("No playlist found: " + playlist.getName());

        save();
        return Optional.empty();
    }

    /**
     * Renames the given playlist with a valid and unique new name.
     *
     * @param playlist the playlist to rename
     * @param newName  the new name for the playlist
     * @return an empty Optional if the rename is successful, or an Optional
     * containing an error message otherwise.
     */
    public Optional<String> renamePlaylist(Playlist playlist, String newName) {
        if (playlist == null || !playlists.contains(playlist))
            return Optional.of("Playlist to rename not found");

        if (newName == null || newName.trim().isEmpty())
            return Optional.of("The new playlist name cannot be empty");

        if (playlist.getName().equals(newName))
            return Optional.empty(); // No changes needed

        if (existsByName(newName))
            return Optional.of("A playlist with this name already exists");

        playlist.setName(newName);
        save();
        return Optional.empty();
    }

    /**
     * Adds a list of tracks to the given playlist in a single batch.
     * Duplicates are skipped and reported in the returned {@link AdditionResult}.
     * Persists once at the end, only if at least one track was actually added.
     *
     * @param playlist the destination playlist
     * @param tracks   the tracks to add
     * @return an {@link AdditionResult} describing what was added and what was skipped
     * @throws IllegalArgumentException if the playlist is null, not managed by this
     * service, or the track list is null/empty
     */
    public AdditionResult addTracksToPlaylist(Playlist playlist, List<Track> tracks) {
        if (playlist == null || !playlists.contains(playlist))
            throw new IllegalArgumentException("Playlist not found");

        if (tracks == null || tracks.isEmpty())
            throw new IllegalArgumentException("No tracks selected");

        List<Track>  alreadyIn     = (List<Track>) playlist.getTracks();
        List<String> skippedTitles = new ArrayList<>();
        int          added         = 0;

        for (Track t : tracks) {
            if (alreadyIn.contains(t)) {
                skippedTitles.add(t.getTitle());
            } else {
                playlist.addTrack(t);
                added++;
            }
        }

        if (added > 0) save(); // single save only if something actually changed

        return new AdditionResult(added, skippedTitles);
    }

    /**
     * Retrieves the list of all playlists managed by this service.
     * * @return a list of {@link Playlist} objects
     */
    public List<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * Retrieves a playlist by its exact name.
     * * @param name the name of the playlist to find
     * @return the {@link Playlist} if found, or null if it does not exist
     */
    public Playlist getPlaylist(String name) {
        return playlists.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the names of all managed playlists.
     * * @return a list of strings representing the playlist names
     */
    public List<String> getPlaylistNames() {
        return playlists.stream()
                .map(Playlist::getName)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of track titles contained in the given playlist.
     *
     * @param playlist the playlist whose track names are requested
     * @return list of track titles, or an empty list if the playlist is null
     */
    public List<String> getTrackNamesFromPlaylist(Playlist playlist) {
        if (playlist == null) return Collections.emptyList();

        return playlist.getTracks().stream()
                .map(Track::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a playlist with the given name already exists.
     * * @param name the name to verify
     * @return true if the playlist name exists, false otherwise
     */
    private boolean existsByName(String name) {
        return playlists.stream()
                .anyMatch(p -> p.getName().trim().equals(name.trim()));
    }

    /**
     * Saves the current list of playlists to the designated JSON file.
     */
    @Override
    public void save() {
        try {
            mapper.writeValue(new File(path), playlists);
        } catch (IOException e) {
            System.err.println("Error saving playlists: " + path);
            e.printStackTrace();
        }
    }

    /**
     * Loads the list of playlists from the designated JSON file
     * and maps the track IDs to the corresponding {@link Track} objects from the {@link Library}.
     */
    @Override
    public void load() {
        File file = new File(path);
        if (!file.exists()) return;

        try {
            List<Playlist> loaded = mapper.readValue(
                    file, new TypeReference<List<Playlist>>() {});

            playlists.clear();

            for (Playlist p : loaded) {
                List<UUID> ids = p.getTrackIds();
                if (ids != null && !ids.isEmpty()) {
                    Library library = Library.getInstance();
                    for (UUID id : ids) {
                        Track t = library.getTrackById(id);
                        if (t != null) p.addTrack(t);
                    }
                }
                playlists.add(p);
            }

        } catch (Exception e) {
            System.err.println("Error loading playlists: " + path);
            e.printStackTrace();
            throw new RuntimeException("Failed to load playlists from " + path, e);
        }
    }

    /**
     * Returns a string representation of the PlaylistService,
     * including its save path and the list of managed playlists.
     * * @return a formatted string describing the service
     */
    @Override
    public String toString() {
        if (playlists.isEmpty()) {
            return "PlaylistService: there is no playlist.";
        }

        return "PlaylistService (Saving in: " + path + "):\n" +
                playlists.stream()
                        .map(String::valueOf)
                        .map(p -> " - " + p)
                        .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Observer method triggered when a track is deleted from the library.
     * Removes the deleted track from all playlists and saves the changes.
     * * @param track the track that was deleted
     */
    @Override
    public void onTrackDeleted(Track track) {
        for (Playlist playlist : this.getPlaylists()) {
            playlist.removeTrack(track);
        }
        this.save();
    }

    @Override
    public void onTrackEdit(Track track) {

    }
}