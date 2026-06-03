package it.unisa.gruppo7.musicplayer.musicplayerfacade;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;

import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private final PlaylistService playlistService;

    private MusicPlayerFacade() {
        this.library = Library.getInstance();
        this.playlistService = new PlaylistService();
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
            throw new IllegalArgumentException(e.getMessage());            
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

    public Playlist createPlaylist(String name) {
        return playlistService.createPlaylist(name);
    }

    public Optional<String> deletePlaylist(String name) {
        return playlistService.deletePlaylist(name);
    }

    public Optional<String> renamePlaylist(String oldName, String newName) {
        return playlistService.renamePlaylist(oldName, newName);
    }

    public List<Playlist> getPlaylists() {
        return playlistService.getPlaylists();
    }

    public Playlist getPlaylist(String name) {
        return playlistService.getPlaylist(name);
    }

    public PlaylistService getPlaylistService(){
        return playlistService;
    }

    public void savePlaylists() {
        playlistService.save();
    }
}