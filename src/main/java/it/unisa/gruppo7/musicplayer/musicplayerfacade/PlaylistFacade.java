package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.util.List;
import java.util.Optional;

import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;

/**
 * Playlist-facing view of the facade (Interface Segregation Principle).
 * <p>
 * Groups only the playlist-management operations (both manual and automatic), so
 * consumers that work exclusively with playlists can depend on this narrow
 * contract instead of the whole {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface PlaylistFacade {

    /** Creates a new playlist. */
    Playlist createPlaylist(String name);

    /** Deletes a playlist. */
    Optional<String> deletePlaylist(Playlist playlist);

    /** Renames a playlist. */
    Optional<String> renamePlaylist(Playlist playlist, String newName);

    /** Returns every playlist. */
    List<Playlist> getPlaylists();

    /** Looks up a playlist by its exact name. */
    Playlist getPlaylist(String name);

    /** Returns the most played playlists, in descending play-count order. */
    List<Playlist> getMostPlayedPlaylists(int limit);

    /** Generates and registers an automatic playlist by filtering the library with the strategy. */
    Playlist createAutoPlaylist(String playlistName, PlaylistGenerationStrategy strategy, AutomaticPlaylistRule rule);

    /** Exposes the underlying playlist service. */
    PlaylistService getPlaylistService();
}
