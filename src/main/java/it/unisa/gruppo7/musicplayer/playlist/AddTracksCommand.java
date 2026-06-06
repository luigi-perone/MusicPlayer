package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.track.Track;
import java.util.List;

public class AddTracksCommand implements Command<AdditionResult> {

    private final PlaylistService playlistService;
    private final Playlist        playlist;
    private final List<Track>     tracks;

    public AddTracksCommand(PlaylistService playlistService,
                            Playlist playlist,
                            List<Track> tracks) {
        this.playlistService = playlistService;
        this.playlist        = playlist;
        this.tracks          = tracks;
    }

    @Override
    public AdditionResult execute() {
        return playlistService.addTracksToPlaylist(playlist, tracks);
    }
}
