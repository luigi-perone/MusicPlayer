package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.track.Track;

public class RemoveTrackCommand implements Command<Void>{
    private final Playlist playlist;
    private final Track trackToRemove;

    public RemoveTrackCommand(Playlist playlist, Track trackToRemove) {
        this.playlist = playlist;
        this.trackToRemove = trackToRemove;
    }

    @Override
    public Void execute() throws Exception {
        if (trackToRemove == null) {
            throw new Exception("No track selected.");
        }
        playlist.removeTrack(trackToRemove);
        return null;
    }
}
