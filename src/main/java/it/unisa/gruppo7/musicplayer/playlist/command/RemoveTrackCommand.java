package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Command to remove a track from a playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class RemoveTrackCommand implements Command<Void> {
    private final Playlist playlist;
    private final Track trackToRemove;

    /**
     * Constructs a new RemoveTrackCommand.
     *
     * @param playlist      The playlist from which to remove the track.
     * @param trackToRemove The track to remove.
     */
    public RemoveTrackCommand(Playlist playlist, Track trackToRemove) {
        this.playlist = playlist;
        this.trackToRemove = trackToRemove;
    }

    /**
     * Executes the removal of the track from the playlist.
     *
     * @return null upon completion.
     * @throws Exception If no track has been selected for removal.
     */
    @Override
    public Void execute() throws Exception {
        if (trackToRemove == null) {
            throw new Exception("No track selected.");
        }
        playlist.removeTrack(trackToRemove);
        return null;
    }
}