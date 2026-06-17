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
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategyFactory;

/**
 * Manages the collection of playlists in the music player,
 * handling creation with name validation and access to the playlists.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlaylistService implements PersistenceService, TrackObserver {
    private static final String DEFAULT_PATH = "data/playlist.json";
    private static final String RULES_PATH = "data/automatic-playlists.json";
    private final String path;
    private final List<Playlist> playlists = new ArrayList<>();
    private final ObjectMapper mapper;
    private final Map<String, AutomaticPlaylistRule> automaticRules = new HashMap<>();

    /**
     * Default constructor that initializes the service with the default file path.
     */
    public PlaylistService() {
        this(DEFAULT_PATH);
    }

    /**
     * Constructor that initializes the service with a custom file path.
     *
     * @param path the file path where playlists will be saved and loaded from
     */
    public PlaylistService(String path) {
        this.path = path;
        this.mapper = new ObjectMapper();
        this.load();
        this.loadAutomaticRules();
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
            throw new IllegalArgumentException("Esiste già una playlist con questo nome");
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

        AutomaticPlaylistRule removedRule = automaticRules.remove(playlist.getName());

        if (removedRule != null) {
            saveAutomaticRules();
        }

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

        String oldName = playlist.getName();
        playlist.setName(newName);

        AutomaticPlaylistRule rule = automaticRules.remove(oldName);

        if (rule != null) {
            automaticRules.put(newName, rule);
            saveAutomaticRules();
        }

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
        List<Track>  addedTracks   = new ArrayList<>();

        for (Track t : tracks) {
            if (alreadyIn.contains(t)) {
                skippedTitles.add(t.getTitle());
            } else {
                playlist.addTrack(t);
                addedTracks.add(t);
            }
        }

        if (!addedTracks.isEmpty()) save(); // single save only if something actually changed

        return new AdditionResult(addedTracks, skippedTitles);
    }

    /**
     * Reorders a track within the given playlist, moving it from one position to
     * another and persisting the new sequence automatically (US-027).
     *
     * @param playlist the playlist whose tracks are being reordered
     * @param from     the current index of the track
     * @param to       the target index
     * @return an empty Optional if the reorder is successful, or an Optional
     * containing an error message otherwise.
     */
    public Optional<String> reorderTrack(Playlist playlist, int from, int to) {
        if (playlist == null || !playlists.contains(playlist))
            return Optional.of("Playlist to reorder not found");

        List<Track> tracks = (List<Track>) playlist.getTracks();
        int size = tracks.size();

        if (from < 0 || from >= size || to < 0 || to >= size)
            return Optional.of("Track position out of range");

        if (from == to)
            return Optional.empty(); // No changes needed

        Track moved = tracks.remove(from);
        tracks.add(to, moved);
        save();
        return Optional.empty();
    }

    /**
     * Retrieves the list of all playlists managed by this service.
     *
     * @return a list of {@link Playlist} objects
     */
    public List<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * Returns the most played playlists, ordered by descending play count.
     * Playlists that have never been played are excluded.
     *
     * @param limit the maximum number of playlists to return.
     * @return the most played playlists, at most {@code limit} entries.
     */
    public List<Playlist> getMostPlayed(int limit) {
        return playlists.stream()
                .filter(playlist -> playlist.getPlayCount() > 0)
                .sorted(Comparator.comparingInt(Playlist::getPlayCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a playlist by its exact name.
     *
     * @param name the name of the playlist to find
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
     *
     * @return a list of strings representing the playlist names
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
     *
     * @param name the name to verify
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
     *
     * @return a formatted string describing the service
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
     *
     * @param track the track that was deleted
     */
    @Override
    public void onTrackDeleted(Track track) {
        for (Playlist playlist : this.getPlaylists()) {
            playlist.removeTrack(track);
        }
        this.save();
    }

    /**
     * Observer method triggered when a track is edited in the library.
     * Re-evaluates all automatic playlists so their contents stay consistent
     * with the updated track metadata.
     *
     * @param track the track that was edited
     */
    @Override
    public void onTrackEdit(Track track) {

            refreshAutomaticPlaylists(Library.getInstance().getTracks());
    }


    /**
     * Removes the given tracks from the playlist and persists the change.
     * Used to undo a previous batch addition.
     *
     * @param playlist the playlist to remove the tracks from
     * @param tracks   the tracks to remove
     */
    public void removeTracksFromPlaylist(Playlist playlist, List<Track> tracks) {
        if (playlist == null || tracks == null || tracks.isEmpty()) return;

        boolean changed = false;
        for (Track t : tracks) {
            if (playlist.removeTrack(t)) changed = true;
        }

        if (changed) save();
    }

    /**
     * Re-inserts a track into the playlist at the given index and persists the change.
     * The index is clamped to the current bounds in case the playlist size changed.
     * Used to undo a previous removal, restoring the track to its original position.
     *
     * @param playlist the playlist to insert the track into
     * @param track    the track to re-insert
     * @param index    the original position of the track
     */
    public void insertTrackAt(Playlist playlist, Track track, int index) {
        if (playlist == null || track == null) return;

        int size = playlist.getTrackCount();
        int safeIndex = Math.max(0, Math.min(index, size));
        playlist.insertTrack(safeIndex, track);
        save();
    }

    /**
     * Captures a snapshot of every playlist that currently contains the given track.
     * Used to undo a cascading library removal: only the affected playlists are
     * recorded, so each can be restored to its exact previous content and order.
     *
     * @param track the track to look for
     * @return a map from each affected playlist to its snapshot (empty if none)
     */
    public Map<Playlist, PlaylistMemento> capturePlaylistsContaining(Track track) {
        Map<Playlist, PlaylistMemento> snapshots = new HashMap<>();
        if (track == null) return snapshots;

        for (Playlist p : playlists) {
            if (p.getTracks().contains(track)) {
                snapshots.put(p, p.snapshot());
            }
        }
        return snapshots;
    }

    /**
     * Restores the given playlists from their snapshots and persists once.
     * Used to undo a cascading library removal.
     *
     * @param snapshots the playlist snapshots to restore
     */
    public void restorePlaylists(Map<Playlist, PlaylistMemento> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return;

        for (Map.Entry<Playlist, PlaylistMemento> entry : snapshots.entrySet()) {
            entry.getKey().restore(entry.getValue());
        }
        save();
    }

    /**
     * Re-inserts a playlist at the given position and persists the change.
     * The index is clamped to the current bounds. Used to undo a playlist deletion,
     * restoring it to its original place in the list.
     *
     * @param index    the original position of the playlist
     * @param playlist the playlist to re-insert
     */
    public void insertPlaylistAt(int index, Playlist playlist) {
        if (playlist == null) return;

        int safeIndex = Math.max(0, Math.min(index, playlists.size()));
        playlists.add(safeIndex, playlist);
        save();
    }

    /* Automatic rules */

    /**
     * Registers an automatic generation rule for the given playlist and persists it.
     * From now on the playlist's contents can be (re)generated from the rule.
     *
     * @param playlist the playlist the rule applies to
     * @param rule     the generation rule to associate with the playlist
     * @throws IllegalArgumentException if the playlist or the rule is {@code null}
     */
    public void registerAutomaticPlaylist( Playlist playlist, AutomaticPlaylistRule rule) {
        if (playlist == null || rule == null) {
            throw new IllegalArgumentException("Playlist e regola devono essere specificate");
        }
        automaticRules.put(playlist.getName(), rule);
        saveAutomaticRules();
    }

    /**
     * Persists the automatic playlist rules to their JSON file,
     * creating the parent directory if necessary.
     *
     * @throws RuntimeException if the rules cannot be written
     */
    private void saveAutomaticRules() {
        File file = new File(RULES_PATH);
        File parent = file.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        try {
            mapper.writeValue(file, automaticRules);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile salvare le regole automatiche", e);
        }
    }

    /**
     * Loads the automatic playlist rules from their JSON file, if it exists.
     *
     * @throws RuntimeException if the rules file exists but cannot be read
     */
    private void loadAutomaticRules() {
        File file = new File(RULES_PATH);

        if (!file.exists()) {
            return;
        }

        try {
            Map<String, AutomaticPlaylistRule> loaded =
                    mapper.readValue(
                            file,
                            new TypeReference<Map<String, AutomaticPlaylistRule>>() {}
                    );

            automaticRules.clear();

            if (loaded != null) {
                automaticRules.putAll(loaded);
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare le regole automatiche", e);
        }
    }

    /**
     * Creates the generation strategy matching the rule's criterion
     * ({@code TAG}, {@code GENRE} or {@code YEAR}).
     *
     * @param rule the rule describing the generation criterion
     * @return the corresponding {@link PlaylistGenerationStrategy}
     * @throws IllegalArgumentException if the rule's criterion is not supported
     */
    private PlaylistGenerationStrategy createStrategy(AutomaticPlaylistRule rule) {
        return PlaylistGenerationStrategyFactory.from(rule);
    }

    /**
     * Re-evaluates every registered automatic playlist against the given tracks,
     * replacing the contents of any playlist whose matching tracks have changed,
     * and persists once if at least one playlist was updated.
     *
     * @param allTracks the tracks to evaluate the rules against
     */
    public void refreshAutomaticPlaylists(Collection<Track> allTracks) {
        boolean changed = false;

        for (Map.Entry<String, AutomaticPlaylistRule> entry
                : automaticRules.entrySet()) {

            Playlist playlist = getPlaylist(entry.getKey());

            if (playlist == null) {
                continue;
            }

            PlaylistGenerationStrategy strategy =
                    createStrategy(entry.getValue());

            List<Track> matchingTracks =
                    strategy.generate(allTracks);

            List<Track> currentTracks =
                    new ArrayList<>(playlist.getTracks());

            if (!currentTracks.equals(matchingTracks)) {
                playlist.clear();
                matchingTracks.forEach(playlist::addTrack);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }



}