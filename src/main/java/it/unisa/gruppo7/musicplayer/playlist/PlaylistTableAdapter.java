package it.unisa.gruppo7.musicplayer.playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Implements the adapter pattern between the Playlist and the PlaylistDetailController
 * 
 * @author Maxim Makhovskyy
 */
public class PlaylistTableAdapter {
    private final Playlist playlist;
    private final ObservableList<Track> observableItems;

    public PlaylistTableAdapter(Playlist playlist){
        this.playlist = playlist;
        this.observableItems = FXCollections.observableArrayList(playlist.getPlaylist());
    }

    public ObservableList<Track> getItems(){
        return observableItems;
    }

    public void trackAdded(Track t){
        playlist.addTrack(t);
        observableItems.add(t);
    }

    public void trackRemoved(Track t){
        playlist.removeTrack(t);
        observableItems.remove(t);
    }
}
