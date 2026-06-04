package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.command.Command;

public class DeletePlaylistCommand implements Command<Void>{
    private final PlaylistService service;
    private final String playlistName;

    public DeletePlaylistCommand(PlaylistService service, String playlistName) {
        this.service = service;
        this.playlistName = playlistName;
    }

    public Void execute() throws Exception {
        Optional<String> error = service.deletePlaylist(playlistName);
        if (error.isPresent()) {
            throw new Exception("Impossible to delete: " + error.get());
        }
        return null;
    }
    
}
