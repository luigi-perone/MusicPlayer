package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.command.Command;

public class RenamePlaylistCommand implements Command<Void>{
    private final PlaylistService service;
    private final String oldName;
    private final String newName;

    public RenamePlaylistCommand(PlaylistService service, String oldName, String newName) {
        this.service = service;
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public Void execute() throws Exception {
        if (oldName.equals(newName)) {
            return null;
        }

        Optional<String> error = service.renamePlaylist(oldName, newName);
        if (error.isPresent()) {
            throw new Exception("Errore di rinomina: " + error.get());
        }

        return null;
    }
}
