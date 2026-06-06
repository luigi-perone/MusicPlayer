package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import java.util.List;

/**
 * Command to add a list of tracks to a specific playlist.
 */
public class AddTracksCommand implements Command<AdditionResult> {

    private final PlaylistService playlistService;
    private final Playlist playlist;
    private final List<Track>     tracks;

    /**
     * Constructs a new AddTracksCommand.
     *
     * @param playlistService The service for managing playlists.
     * @param playlist        The playlist to add the tracks to.
     * @param tracks          The list of tracks to add.
     */
    public AddTracksCommand(PlaylistService playlistService,
                            Playlist playlist,
                            List<Track> tracks) {
        this.playlistService = playlistService;
        this.playlist        = playlist;
        this.tracks          = tracks;
    }

    /**
     * Executes the addition of the tracks to the playlist.
     *
     * @return The result of the addition operation.
     */
    @Override
    public AdditionResult execute() {
        return playlistService.addTracksToPlaylist(playlist, tracks);
    }
}