package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.track.Track;

import java.time.Year;
import java.util.Collection;
import java.util.UUID;

/**
 * Facade Pattern
 *
 * @author francescoLemmo
 */
public class MusicPlayerFacade {
    // pattern singleton
    private static MusicPlayerFacade instance;

    private final Library library;

    private MusicPlayerFacade() {
        this.library = Library.getInstance();
    }

    public static MusicPlayerFacade getInstance() {
        if (instance == null) {
            instance = new MusicPlayerFacade();
        }

        return instance;
    }

    private Track createTrack(String title, String author, int duration, String genre, Year publicationYear) {
        boolean isGenreEmpty = (genre == null || genre.trim().isEmpty());
        boolean isYearEmpty = (publicationYear == null);

        if (isGenreEmpty && isYearEmpty) {
            return new Track(title, author, duration);
        } else if (isGenreEmpty) {
            return new Track(title, author, duration, publicationYear);
        } else if (isYearEmpty) {
            return new Track(title, author, duration, genre);
        } else {
            return new Track(title, author, duration, genre, publicationYear);
        }
    }

    public boolean addNewTrackToLibrary(String title, String author, int duration, String genre, Year publicationYear) {
        try {
            Track newTrack = this.createTrack(title, author, duration, genre, publicationYear);

            boolean success = library.addTrack(newTrack);
            if (success) {
                library.save();
            }
            return success;

        } catch (IllegalArgumentException e) {
            System.err.println("Validation Error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeTrackFromLibrary(Track track) {
        boolean success = library.removeTrack(track);
        if (success) {
            library.save();
        }
        return success;
    }

    public boolean modifyTrack(Track track, String newTitle, String newAuthor, int newDuration, String newGenre, Year newPublicationYear) {
        boolean success = library.modifyTrackInLibrary(track, newTitle, newAuthor, newDuration, newGenre, newPublicationYear);
        if (success) {
            library.save();
        }
        return success;
    }

    public Track getTrackFromLibrary(UUID id) {
        return library.getTrackById(id);
    }

    public Collection<Track> getTracksFromLibrary() {
        return library.getTracks();
    }

    // --print library--

    public String printLibrary() {
        return library.toString();
    }
}