package it.unisa.gruppo7.musicplayer.playlist.command;

import java.util.Optional;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

/**
 * Command to delete an existing playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class DeletePlaylistCommand implements Command<Void> {
    private final PlaylistService service;
    private final Playlist playlist;

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
        Optional<String> error = service.deletePlaylist(playlist);
        if (error.isPresent()) {
            throw new Exception("Impossible to delete: " + error.get());
        }
        return null;
    }
}