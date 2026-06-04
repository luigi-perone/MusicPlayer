package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;

public class PlayTrackCommand implements Command<Void>{
    private final MusicPlayerFacade facade;
    private final Track track;

    public PlayTrackCommand(MusicPlayerFacade facade, Track track) {
        this.facade = facade;
        this.track = track;
    }

    @Override
    public Void execute() {
        if (track != null) {
            facade.playTrack(track);
        }
        return null;
    }
}
