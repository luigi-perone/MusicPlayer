package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Command to start the playback of a specific track.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlayTrackCommand implements Command<Void> {
    private final MusicPlayerFacade facade;
    private final Playlist playlist;
    private final Track track;

    /**
     * Constructs a new PlayTrackCommand.
     *
     * @param facade   The facade interface of the music player.
     * @param playlist The playlist the track is played from.
     * @param track    The track to play.
     */
    public PlayTrackCommand(MusicPlayerFacade facade, Playlist playlist, Track track) {
        this.facade = facade;
        this.playlist = playlist;
        this.track = track;
    }

    /**
     * Executes the command by starting the playback of the track, if present.
     *
     * @return null upon completion.
     */
    @Override
    public Void execute() {
        if (track != null) {
            facade.playFromPlaylistFrom(playlist, track);
        }
        return null;
    }
}