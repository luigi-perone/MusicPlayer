package it.unisa.gruppo7.musicplayer.track;

import it.unisa.gruppo7.musicplayer.command.UndoableCommand;
import it.unisa.gruppo7.musicplayer.library.LibraryMemento;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;

import java.time.Year;

/**
 * Command to add a new track to the music library.
 * Captures a snapshot of the library before the insertion, so {@link #undo()} can
 * restore the library to its previous state (the snapshot does not contain the new
 * track).
 *
 */
public class AddTrackToLibraryCommand implements UndoableCommand<Boolean> {

    private final MusicPlayerFacade facade;
    private final String title;
    private final String author;
    private final int    duration;
    private final String genre;
    private final Year   publicationYear;

    private LibraryMemento libraryMemento;

    /**
     * Constructs a new AddTrackToLibraryCommand.
     *
     * @param facade          The application facade exposing the library operations.
     * @param title           The title of the track to add.
     * @param author          The author/artist of the track.
     * @param duration        The duration of the track in seconds.
     * @param genre           The music genre of the track.
     * @param publicationYear The release year of the track, or null if unspecified.
     */
    public AddTrackToLibraryCommand(MusicPlayerFacade facade,
                                    String title,
                                    String author,
                                    int duration,
                                    String genre,
                                    Year publicationYear) {
        this.facade          = facade;
        this.title           = title;
        this.author          = author;
        this.duration        = duration;
        this.genre           = genre;
        this.publicationYear = publicationYear;
    }

    /**
     * Executes the addition of the new track, capturing the library state needed to undo it.
     *
     * @return true if the track was successfully added, false otherwise.
     * @throws IllegalArgumentException If the track fields violate domain constraints
     *                                  or a track with the same signature already exists.
     */
    @Override
    public Boolean execute() throws Exception {
        this.libraryMemento = facade.captureLibraryState();
        return facade.addNewTrackToLibrary(title, author, duration, genre, publicationYear);
    }

    /**
     * Reverts the addition by restoring the library to its pre-insertion state.
     */
    @Override
    public void undo(){
        facade.restoreLibraryState(libraryMemento);
    }
}
