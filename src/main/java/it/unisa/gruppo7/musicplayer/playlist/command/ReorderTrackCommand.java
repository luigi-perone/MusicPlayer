package it.unisa.gruppo7.musicplayer.playlist.command;

import java.util.Optional;
import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

/**
 * Command to reorder a track within a playlist, moving it from one position to
 * another and persisting the new sequence (US-027).
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class ReorderTrackCommand implements Command<Void> {

    private final PlaylistService service;
    private final Playlist        playlist;
    private final int             from;
    private final int             to;

    /**
     * Constructs a new ReorderTrackCommand.
     *
     * @param service  The service for managing playlists.
     * @param playlist The playlist whose tracks are being reordered.
     * @param from     The current index of the track to move.
     * @param to       The target index.
     */
    public ReorderTrackCommand(PlaylistService service, Playlist playlist, int from, int to) {
        this.service  = service;
        this.playlist = playlist;
        this.from     = from;
        this.to       = to;
    }

    /**
     * Executes the reordering of the track within the playlist.
     *
     * @return null upon completion.
     * @throws Exception If the reorder cannot be performed.
     */
    @Override
    public Void execute() throws Exception {
        Optional<String> error = service.reorderTrack(playlist, from, to);
        if (error.isPresent()) {
            throw new Exception("Reordering error: " + error.get());
        }
        return null;
    }
}
