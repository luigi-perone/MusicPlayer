package it.unisa.gruppo7.musicplayer.playlist.command;

import java.util.Optional;
import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

/**
 * Command to rename an existing playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class RenamePlaylistCommand implements Command<Void> {

    private final PlaylistService service;
    private final Playlist playlist;
    private final String          newName;

    /**
     * Constructs a new RenamePlaylistCommand.
     *
     * @param service  The service for managing playlists.
     * @param playlist The playlist to rename.
     * @param newName  The new name to assign to the playlist.
     */
    public RenamePlaylistCommand(PlaylistService service, Playlist playlist, String newName) {
        this.service  = service;
        this.playlist = playlist;
        this.newName  = newName;
    }

    /**
     * Executes the renaming of the playlist.
     * If the new name is identical to the current one, the operation is ignored.
     *
     * @return null upon completion.
     * @throws Exception If an error occurs during the renaming.
     */
    @Override
    public Void execute() throws Exception {
        if (playlist.getName().equals(newName)) {
            return null;
        }

        Optional<String> error = service.renamePlaylist(playlist, newName);
        if (error.isPresent()) {
            throw new Exception("Renaming error: " + error.get());
        }

        return null;
    }
}