package it.unisa.gruppo7.musicplayer.playlist;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.library.Library;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import it.unisa.gruppo7.musicplayer.core.PersistenceService;

import it.unisa.gruppo7.musicplayer.library.Library;

/**
 * Manages the collection of playlists in the music player,
 * handling creation with name validation and access to the playlists.
 * 
 * @author Maxim Makhovskyy
 */
public class PlaylistService implements PersistenceService{
    private static final String DEFAULT_PATH = "src/main/java/it/unisa/gruppo7/musicplayer/playlist/playlist.json";
    private final String path;
    private final List<Playlist> playlists = new ArrayList<>();
    private final ObjectMapper mapper;

    public PlaylistService(){
        this(DEFAULT_PATH);
    }

    public PlaylistService(String path){
        this.path = path;
        this.mapper = new ObjectMapper();
    }

    /**
     * Creates a new playlist with the given name, if the name is valid and unique.
     * 
     * @param name the name of the playlist
     * @return an empty Optional if the playlist creation is successful, or an Optional
     *          containing an error message if the name is empty or already in use.
     */
    public Optional<String> createPlaylist(String name) {
        if (name == null || name.trim().isEmpty())
            return Optional.of("A name for the playlist must be provided");
        if (existsByName(name))
            return Optional.of("This playlist name already exists");
        playlists.add(new Playlist(name, new ArrayList<>()));
        return Optional.empty();
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    private boolean existsByName(String name) {
        return playlists.stream()
                        .anyMatch(p -> p.getName().equals(name));
    }

    @Override
    public void save() {
        try {
            mapper.writeValue(new File(path), playlists);
        } catch (IOException e) {
            System.err.println("Error saving playlists: " + path);
            e.printStackTrace();
        }
    }

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
}
