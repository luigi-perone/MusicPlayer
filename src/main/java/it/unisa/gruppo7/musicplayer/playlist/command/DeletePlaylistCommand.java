package it.unisa.gruppo7.musicplayer.playlist.command;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

/**
 * Command to delete an existing playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class DeletePlaylistCommand implements UndoableCommand<Void> {
    private final PlaylistService service;
    private final Playlist playlist;
    private int originalIndex = -1;

    /**
     * Constructs a new DeletePlaylistCommand.
     *
     * @param service  The service for managing playlists.
     * @param playlist The playlist to delete.
     */
    public DeletePlaylistCommand(PlaylistService service, Playlist playlist) {
        this.service = service;
        this.playlist = playlist;
    }

    /**
     * Executes the deletion of the playlist.
     *
     * @return null upon completion.
     * @throws Exception If the playlist cannot be deleted or an error occurs.
     */
    public Void execute() throws Exception {
        this.originalIndex = service.getPlaylists().indexOf(playlist);
        Optional<String> error = service.deletePlaylist(playlist);
        if (error.isPresent()) {
            throw new Exception("Impossible to delete: " + error.get());
        }
        return null;
    }

    /**
     * Undoes the deletion by re-inserting the playlist at its original position.
     */
    @Override
    public void undo() {
        if (originalIndex >= 0) {
            service.insertPlaylistAt(originalIndex, playlist);
        }
    }
}