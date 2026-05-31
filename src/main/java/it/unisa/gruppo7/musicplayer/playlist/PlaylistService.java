package main.java.it.unisa.gruppo7.musicplayer.playlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaylistService {
    private final List<Playlist> playlists = new ArrayList<>();

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
