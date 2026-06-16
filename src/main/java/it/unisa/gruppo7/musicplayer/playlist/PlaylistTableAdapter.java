package it.unisa.gruppo7.musicplayer.playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Implements the adapter pattern between the Playlist and the PlaylistDetailController.
 * It provides an ObservableList to seamlessly bind the underlying Playlist data to JavaFX UI components.
 * * @author Maxim Makhovskyy
 */
public class PlaylistTableAdapter {
    private final Playlist playlist;
    private final ObservableList<Track> observableItems;

    /**
     * Constructs a new PlaylistTableAdapter for the specified playlist.
     * Initializes the observable list with the current tracks from the playlist.
     *
     * @param playlist The underlying playlist to adapt.
     */
    public PlaylistTableAdapter(Playlist playlist){
        this.playlist = playlist;
        this.observableItems = FXCollections.observableArrayList(playlist.getPlaylist());
    }

    /**
     * Retrieves the observable list of tracks.
     * This list is intended to be bound to a JavaFX TableView or ListView.
     *
     * @return The observable list of tracks.
     */
    public ObservableList<Track> getItems(){
        return observableItems;
    }

    /**
     * Adds a new track to the playlist.
     * The track is added to both the underlying data model and the observable UI list.
     *
     * @param t The track to add.
     */
    public void trackAdded(Track t){
        playlist.addTrack(t);
        observableItems.add(t);
    }

    /**
     * Removes a track from the playlist.
     * The track is removed from both the underlying data model and the observable UI list.
     *
     * @param t The track to remove.
     */
    public void trackRemoved(Track t){
        playlist.removeTrack(t);
        observableItems.remove(t);
    }

    /**
     * Moves a track within the observable UI list from one position to another (US-027).
     * The underlying playlist model is reordered separately (via the service), so this
     * method only keeps the bound UI list in sync. The change fires the table's
     * {@code ListChangeListener}, which persists the new order automatically.
     *
     * @param from the current index of the track.
     * @param to   the target index.
     */
    public void moveTrack(int from, int to){
        int size = observableItems.size();
        if (from < 0 || from >= size || to < 0 || to >= size || from == to) {
            return;
        }
        Track t = observableItems.remove(from);
        observableItems.add(to, t);
    }
}