package it.unisa.gruppo7.musicplayer.playlist.command;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;

/**
 * Command to create a new playlist.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class CreatePlaylistCommand implements UndoableCommand<Playlist> {

    private final PlaylistService service;
    private final String playlistName;
    private Playlist created;

    /**
     * Constructs a new CreatePlaylistCommand.
     *
     * @param service      The service for managing playlists.
     * @param playlistName The name of the new playlist to create.
     */
    public CreatePlaylistCommand(PlaylistService service, String playlistName) {
        this.service = service;
        this.playlistName = playlistName;
    }

    /**
     * Executes the creation of the playlist.
     *
     * @return The newly created playlist.
     * @throws Exception If an error occurs during creation.
     */
    @Override
    public Playlist execute() throws Exception {
        this.created = service.createPlaylist(playlistName);
        return created;
    }

    /**
     * Undoes the creation by deleting the playlist that was created.
     */
    @Override
    public void undo() {
        if (created != null) {
            service.deletePlaylist(created);
        }
    }
}