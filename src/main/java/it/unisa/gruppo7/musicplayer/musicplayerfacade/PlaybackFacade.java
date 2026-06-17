package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.util.List;

import it.unisa.gruppo7.musicplayer.playback.PlaybackService;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.RepeatMode;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * Vista "riproduzione" della facade (Interface Segregation Principle).
 * <p>
 * Raccoglie i comandi di controllo della riproduzione e della coda, così i
 * consumer di playback dipendono da questo contratto ristretto invece che
 * dall'intera {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface PlaybackFacade {

    /** Avvia la riproduzione del brano indicato. */
    void playTrack(Track track);

    /** Mette in pausa la riproduzione. */
    void pauseTrack();

    /** Riprende la riproduzione. */
    void resumeTrack();

    /** Avvia la riproduzione dell'intera libreria. */
    void playFromLibrary();

    /** Avvia la riproduzione di una playlist. */
    void playFromPlaylist(Playlist playlist);

    /** Avvia la riproduzione della libreria a partire da un brano. */
    void playFromLibraryFrom(Track track);

    /** Avvia la riproduzione di una playlist a partire da un brano. */
    void playFromPlaylistFrom(Playlist playlist, Track track);

    /** Riproduce un brano già presente in coda. */
    void playFromQueue(Track track);

    /** Accoda tutti i brani di una playlist. */
    void appendPlaylistToQueue(Playlist playlist);

    /** Accoda un singolo brano. */
    void appendTrackToQueue(Track track);

    /** Restituisce i brani successivi in coda. */
    List<Track> getUpNextQueueFrom();

    /** Salta al blocco playlist successivo in coda. */
    void skipToNextPlaylist();

    /** Salta al blocco playlist precedente in coda. */
    void skipToPreviousPlaylist();

    /** @return true se esiste un blocco playlist successivo. */
    boolean hasNextPlaylist();

    /** @return true se esiste un blocco playlist precedente. */
    boolean hasPreviousPlaylist();

    /** Attiva/disattiva lo shuffle relativamente al brano corrente. */
    void shuffleQueue(boolean shuffleState, Track track);

    /** @return true se lo shuffle è attivo. */
    boolean isShuffleActive();

    /** @return la modalità di ripetizione corrente. */
    RepeatMode getCurrentRepeatMode();

    /** Cicla la modalità di ripetizione. */
    void changeRepeatMode();

    /** @return lo stato di riproduzione corrente. */
    PlaybackState getPlaybackState();

    /** @return il brano attualmente in riproduzione, o null. */
    Track getCurrentPlayingTrack();

    /** Salta a un punto specifico del brano corrente. */
    void seekTo(int seconds);

    /** Espone il servizio di playback sottostante. */
    PlaybackService getPlaybackService();
}
