package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;
import it.unisa.gruppo7.musicplayer.command.Command;

public class RenamePlaylistCommand implements Command<Void> {

    private final PlaylistService service;
    private final Playlist        playlist;
    private final String          newName;

    public RenamePlaylistCommand(PlaylistService service, Playlist playlist, String newName) {
        this.service  = service;
        this.playlist = playlist;
        this.newName  = newName;
    }

    @Override
    public Void execute() throws Exception {
        if (playlist.getName().equals(newName)) {
            return null;
        }

        Optional<String> error = service.renamePlaylist(playlist, newName);
        if (error.isPresent()) {
            throw new Exception("Errore di rinomina: " + error.get());
        }

        return null;
    }
}