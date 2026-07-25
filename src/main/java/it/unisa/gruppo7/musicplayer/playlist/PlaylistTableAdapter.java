package it.unisa.gruppo7.musicplayer.playlist;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;

/**
 * Object Adapter between the playlist model and the JavaFX table.
 * 
 * The adapter holds no copy of the tracks: every read and every write is delegated to the
 * playlist, so model and table cannot drift apart. When the adaptee is modified by someone
 * else call {@link #refresh()} to re-publish its content.
 * 
 * @author Maxim Makhovskyy
 */
public class PlaylistTableAdapter extends ModifiableObservableListBase<Track>{
    /**
     * The adaptee
     */
    private final Playlist playlist;

    /**
     * The content already published to the listeners. It is never read as data: it only
     * records which elements were visible before a {@link #refresh()}, so the change event
     * can report what was replaced.
     */
    private List<Track> published;

    /**
     * Constructs a new PlaylistTableAdapter for the specified playlist.
     * Records the playlist's current tracks as the initially published content.
     *
     * @param playlist The underlying playlist to adapt.
     */
    public PlaylistTableAdapter(Playlist playlist){
        this.playlist = playlist;
        this.published = new ArrayList<>(playlist.getPlaylist());
    }

    /**
     * Retrieves the observable list of tracks.
     * This list is intended to be bound to a JavaFX TableView or ListView.
     *
     * @return The observable list of tracks.
     */
    public ObservableList<Track> getItems(){
        return this;
    }

    /**
     * Re-publishes the current content of the playlist, firing a change event towards the
     * bound control. Call it whenever the adaptee has been modified through another path,
     * since a plain playlist is not observable and cannot signal those changes by itself.
     */
    public void refresh(){
        List<Track> previous = this.published;
        snapshot();

        if (previous.isEmpty() && this.published.isEmpty()) {
            return;
        }

        beginChange();
        try {
            if (!previous.isEmpty()) {
                nextRemove(0, previous);
            }
            if (!this.published.isEmpty()) {
                nextAdd(0, this.published.size());
            }
        } finally {
            endChange();
        }
    }

    /**
     * Reads the track at the given position from the adaptee.
     *
     * @param index the position of the track.
     * @return the track held by the playlist at that position.
     */
    public Track get(int index){
        return playlist.getPlaylist().get(index);
    }

    /**
     * Reports the number of tracks held by the adaptee.
     *
     * @return the size of the playlist.
     */
    public int size(){
        return playlist.getTrackCount();
    }

    /**
     * Replaces the whole content of the list.
     * The incoming collection is copied first, because it may be the adaptee's own live
     * list: clearing the adapter would otherwise empty the very collection being read.
     *
     * @param col the tracks to publish.
     * @return true, as the list is always replaced.
     */
    @Override
    public boolean setAll(Collection<? extends Track> col){
        List<Track> incoming = new ArrayList<>(col);
        beginChange();
        try {
            clear();
            addAll(incoming);
        } finally {
            endChange();
        }
        return true;
    }

    /**
     * Translates an insertion on the Target into an insertion on the adaptee.
     *
     * @param index   the position at which the track is inserted.
     * @param element the track to insert.
     */
    @Override
    protected void doAdd(int index, Track element){
        playlist.insertTrack(index, element);
        snapshot();
    }

    /**
     * Translates a replacement on the Target into a replacement on the adaptee.
     *
     * @param index   the position to overwrite.
     * @param element the new track.
     * @return the track previously held at that position.
     */
    @Override
    protected Track doSet(int index, Track element){
        Track previous = playlist.getPlaylist().set(index, element);
        snapshot();
        return previous;
    }

    /**
     * Translates a removal on the Target into a removal on the adaptee.
     *
     * @param index the position of the track to remove.
     * @return the removed track.
     */
    @Override
    protected Track doRemove(int index){
        Track removed = playlist.getPlaylist().remove(index);
        snapshot();
        return removed;
    }

    /**
     * Records the adaptee's current content as the one published to the listeners.
     */
    private void snapshot(){
        this.published = new ArrayList<>(playlist.getPlaylist());
    }
}