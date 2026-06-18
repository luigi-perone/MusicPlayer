package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;

/**
 * Library-facing view of the facade (Interface Segregation Principle).
 * <p>
 * Groups only the track-library management operations, so consumers that work
 * exclusively with the library can depend on this narrow contract instead of the
 * whole {@link MusicPlayerFacade}.
 *
 * @author Gruppo 7
 */
public interface LibraryFacade {

    /** Creates a track and adds it to the library. */
    boolean addNewTrackToLibrary(String title, String author, int duration, String genre, Year publicationYear);

    /** Removes a track from the library. */
    boolean removeTrackFromLibrary(Track track);

    /** Updates the metadata of a track in the library. */
    boolean modifyTrack(Track track, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear);

    /** Updates the predefined tags assigned to a track. */
    void updateTrackTags(Track track, Set<TrackTag> tags);

    /** Looks up a track in the library by its UUID. */
    Track getTrackFromLibrary(UUID id);

    /** Returns every track currently in the library. */
    Collection<Track> getTracksFromLibrary();

    /** Returns the most played tracks, in descending play-count order. */
    List<Track> getMostPlayedTracks(int limit);

    /** Returns a textual representation of the library. */
    String printLibrary();

    /** Removes all tracks from the library. */
    void clearLibrary();
}
