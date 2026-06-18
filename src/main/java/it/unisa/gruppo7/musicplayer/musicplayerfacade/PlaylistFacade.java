package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.util.List;
import java.util.Optional;

import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;

/**
 * Vista "playlist" della facade (Interface Segregation Principle).
 * <p>
 * Raccoglie le sole operazioni di gestione delle playlist (manuali e
 * automatiche), così i consumer che operano solo sulle playlist dipendono da
 * questo contratto ristretto invece che dall'intera {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface PlaylistFacade {

    /** Crea una nuova playlist. */
    Playlist createPlaylist(String name);

    /** Elimina una playlist. */
    Optional<String> deletePlaylist(Playlist playlist);

    /** Rinomina una playlist. */
    Optional<String> renamePlaylist(Playlist playlist, String newName);

    /** Restituisce tutte le playlist. */
    List<Playlist> getPlaylists();

    /** Recupera una playlist per nome esatto. */
    Playlist getPlaylist(String name);

    /** Restituisce le playlist più riprodotte, in ordine decrescente. */
    List<Playlist> getMostPlayedPlaylists(int limit);

    /** Genera e registra una playlist automatica filtrando la libreria con la strategia. */
    Playlist createAutoPlaylist(String playlistName, PlaylistGenerationStrategy strategy, AutomaticPlaylistRule rule);

    /** Espone il servizio playlist sottostante. */
    PlaylistService getPlaylistService();
}
