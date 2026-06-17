package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.QueueMemento;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Command to remove a track from a playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class RemoveTrackCommand implements UndoableCommand<Void> {
    private final PlaylistService playlistService;
    private final PlaybackService playbackService;
    private final Playlist playlist;
    private final Track trackToRemove;

    private int originalIndex = -1;
    private QueueMemento queueSnapshot;

    /**
     * Constructs a new RemoveTrackCommand.
     *
     * @param playlistService The service for managing playlists.
     * @param playbackService The service managing the playback queue.
     * @param playlist      The playlist from which to remove the track.
     * @param trackToRemove The track to remove.
     */
    public RemoveTrackCommand(PlaylistService playlistService,
                              PlaybackService playbackService,
                              Playlist playlist,
                              Track trackToRemove) {
        this.playlistService = playlistService;
        this.playbackService = playbackService;
        this.playlist = playlist;
        this.trackToRemove = trackToRemove;
    }

    /**
     * Executes the removal of the track from the playlist, capturing the state needed to undo it.
     *
     * @return null upon completion.
     * @throws Exception If no track has been selected for removal.
     */
    @Override
    public Void execute() throws Exception {
        if (trackToRemove == null) {
            throw new Exception("No track selected.");
        }
        this.queueSnapshot = (playbackService != null) ? playbackService.captureQueueState() : null;
        this.originalIndex = playlist.indexOf(trackToRemove);

        playlist.removeTrack(trackToRemove);
        playlistService.save();
        return null;
    }

    /**
     * Undoes the removal by re-inserting the track at its original position
     * and restoring the playback queue to its captured state.
     */
    @Override
    public void undo() {
        if (originalIndex >= 0) {
            playlistService.insertTrackAt(playlist, trackToRemove, originalIndex);
        }
        if (queueSnapshot != null) {
            playbackService.restoreQueueState(queueSnapshot);
        }
    }
}