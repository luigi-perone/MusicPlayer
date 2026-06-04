package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.track.Track;
import java.util.List;

public class AddTracksCommand implements Command<AdditionResult> {
    
    private final PlaylistService service;
    private final String playlistName;
    private final List<Track> tracksToAdd;

    public AddTracksCommand(PlaylistService service, String playlistName, List<Track> tracksToAdd) {
        this.service = service;
        this.playlistName = playlistName;
        this.tracksToAdd = tracksToAdd;
    }

    @Override
    public AdditionResult execute() throws Exception {
        if (tracksToAdd == null || tracksToAdd.isEmpty()) {
            throw new Exception("No selected track");
        }
        return service.addTracksToPlaylist(playlistName, tracksToAdd);
    }
}
