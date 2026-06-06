package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.command.Command;

public class DeletePlaylistCommand implements Command<Void>{
    private final PlaylistService service;
    private final Playlist playlist;

    public DeletePlaylistCommand(PlaylistService service, Playlist playlist) {
        this.service = service;
        this.playlist = playlist;
    }

    public Void execute() throws Exception {
        Optional<String> error = service.deletePlaylist(playlist);
        if (error.isPresent()) {
            throw new Exception("Impossible to delete: " + error.get());
        }
        return null;
    }
    
}
