package it.unisa.gruppo7.musicplayer.track;

import java.util.Map;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.library.LibraryMemento;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.QueueMemento;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistMemento;

/**
 * Command to remove a track from the music library.
 * Removing a track from the library cascades: it is also removed from every playlist
 * that contained it and from the playback queue. Before removing, this command
 * snapshots all three affected areas (the library, the affected playlists and the
 * queue), so {@link #undo()} can restore each to its exact previous state.
 *
 */
public class RemoveTrackFromLibraryCommand implements UndoableCommand<Boolean> {

    private final MusicPlayerFacade        facade;
    private final Track                    track;
    private LibraryMemento                 libraryMemento;
    private Map<Playlist, PlaylistMemento> playlistSnapshots;
    private QueueMemento                   queueMemento;

    /**
     * Constructs a new RemoveTrackFromLibraryCommand.
     *
     * @param facade The application facade exposing the library operations.
     * @param track  The track to remove from the library.
     */
    public RemoveTrackFromLibraryCommand(MusicPlayerFacade facade, Track track) {
        this.facade = facade;
        this.track  = track;
    }

    /**
     * Executes the removal of the track, snapshotting the library, the affected
     * playlists and the queue beforehand so the cascade can be reverted.
     *
     * @return true if the track was successfully removed, false otherwise.
     * @throws Exception If no track has been provided for removal.
     */
    @Override
    public Boolean execute() throws Exception {
        if (track == null) {
            throw new Exception("No track selected.");
        }
        this.libraryMemento    = facade.captureLibraryState();
        this.playlistSnapshots = facade.capturePlaylistsContaining(track);
        this.queueMemento      = facade.captureQueueState();
        return facade.removeTrackFromLibrary(track);
    }

    /**
     * Reverts the removal by restoring the library, the affected playlists and the
     * playback queue from their captured snapshots.
     */
    @Override
    public void undo(){
        facade.restoreLibraryState(libraryMemento);
        facade.restorePlaylists(playlistSnapshots);
        facade.restoreQueueState(queueMemento);
    }
}
