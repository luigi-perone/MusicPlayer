package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.QueueMemento;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.util.List;

/**
 * Command to add a list of tracks to a specific playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class AddTracksCommand implements UndoableCommand<AdditionResult> {

    private final PlaylistService playlistService;
    private final PlaybackService playbackService;
    private final Playlist playlist;
    private final List<Track> tracks;

    private List<Track> addedTracks;
    private QueueMemento queueSnapshot;

    /**
     * Constructs a new AddTracksCommand.
     *
     * @param playlistService The service for managing playlists.
     * @param playbackService The service managing the playback queue, used to snapshot/restore it on undo.
     * @param playlist        The playlist to add the tracks to.
     * @param tracks          The list of tracks to add.
     */
    public AddTracksCommand(PlaylistService playlistService,
                            PlaybackService playbackService,
                            Playlist playlist,
                            List<Track> tracks) {
        this.playlistService = playlistService;
        this.playbackService = playbackService;
        this.playlist        = playlist;
        this.tracks          = tracks;
    }

    /**
     * Executes the addition of the tracks to the playlist, capturing the state needed to undo it.
     *
     * @return The result of the addition operation.
     */
    @Override
    public AdditionResult execute() {
        this.queueSnapshot = (playbackService != null) ? playbackService.captureQueueState() : null;

        AdditionResult result = playlistService.addTracksToPlaylist(playlist, tracks);
        this.addedTracks = result.getAddedTracks();
        return result;
    }

    /**
     * Undoes the addition by removing the previously added tracks from the playlist
     * and restoring the playback queue to its captured state.
     */
    @Override
    public void undo() {
        if (addedTracks != null && !addedTracks.isEmpty()) {
            playlistService.removeTracksFromPlaylist(playlist, addedTracks);
        }
        if (queueSnapshot != null) {
            playbackService.restoreQueueState(queueSnapshot);
        }
    }
}