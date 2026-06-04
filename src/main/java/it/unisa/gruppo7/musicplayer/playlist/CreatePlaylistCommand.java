package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;

public class CreatePlaylistCommand implements Command<Playlist> {

    private final PlaylistService service;
    private final String playlistName;

    public CreatePlaylistCommand(PlaylistService service, String playlistName) {
        this.service = service;
        this.playlistName = playlistName;
    }

    @Override
    public Playlist execute() throws Exception {
        return service.createPlaylist(playlistName);
    }
}
