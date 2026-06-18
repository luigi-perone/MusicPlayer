package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;

/**
 * Vista "libreria" della facade (Interface Segregation Principle).
 * <p>
 * Raccoglie le sole operazioni di gestione della libreria dei brani: i consumer
 * che lavorano esclusivamente sulla libreria possono dipendere da questo
 * contratto ristretto invece che dall'intera {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface LibraryFacade {

    /** Crea e aggiunge un brano alla libreria. */
    boolean addNewTrackToLibrary(String title, String author, int duration, String genre, Year publicationYear);

    /** Rimuove un brano dalla libreria. */
    boolean removeTrackFromLibrary(Track track);

    /** Modifica i metadati di un brano della libreria. */
    boolean modifyTrack(Track track, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear);

    /** Aggiorna i tag predefiniti di un brano. */
    void updateTrackTags(Track track, Set<TrackTag> tags);

    /** Recupera un brano dalla libreria tramite UUID. */
    Track getTrackFromLibrary(UUID id);

    /** Restituisce tutti i brani della libreria. */
    Collection<Track> getTracksFromLibrary();

    /** Restituisce i brani più riprodotti, in ordine decrescente. */
    List<Track> getMostPlayedTracks(int limit);

    /** Restituisce una rappresentazione testuale della libreria. */
    String printLibrary();

    /** Svuota la libreria. */
    void clearLibrary();
}
