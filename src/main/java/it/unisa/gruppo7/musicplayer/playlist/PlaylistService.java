package main.java.it.unisa.gruppo7.musicplayer.playlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the collection of playlists in the music player,
 * handling creation with name validation and access to the playlists.
 * 
 * @author Maxim Makhovskyy
 */
public class PlaylistService {
    private final List<Playlist> playlists = new ArrayList<>();

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
        playlists.add(new Playlist(name));
        return Optional.empty();
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    private boolean existsByName(String name) {
        return playlists.stream()
                        .anyMatch(p -> p.getName().equals(name));
    }
}
