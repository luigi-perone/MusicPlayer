package it.unisa.gruppo7.musicplayer.playlist;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;

/**
 * * Models a playlist in the music player, identified by a name 
 * and holding a collection of tracks.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist extends TrackCollection {
    private String playlistName;
    private List<UUID> loadedTrackIds = new ArrayList<>();
    private int playCount;

    /**
     * Constructor of the class Playlist.
     * @param playlistName  the name of the playlist
     * @param trackIds      the list of track IDs loaded from storage
     * @param playCount     The number of times the playlist has been played.
     */
    @JsonCreator
    public Playlist(
            @JsonProperty("name") String playlistName,
            @JsonProperty("trackIds") List<UUID> trackIds,
            @JsonProperty("playCount") Integer playCount) {
        super("", new ArrayList<>());
        this.playlistName = playlistName;
        if (trackIds != null) {
            this.loadedTrackIds = trackIds;
        }
        this.playCount = (playCount == null) ? 0 : playCount;
    }

    /**
     * Constructor of the class Playlist. Default play count.
     * @param playlistName  the name of the playlist
     * @param trackIds      the list of track IDs loaded from storage
     */
    public Playlist(String playlistName, List<UUID> trackIds) {
        this(playlistName, trackIds, null);
    }

    /**
     * Gets the list of track IDs in the playlist.
     * * @return a list of {@link UUID} representing the tracks
     */
    public List<UUID> getTrackIds() {
        if (this.tracks != null && !this.tracks.isEmpty()) {
            return this.tracks.stream()
                    .map(Track::getId)
                    .collect(Collectors.toList());
        }
        return this.loadedTrackIds;
    }

    /**
     * Retrieves the list of tracks contained in the playlist.
     * * @return a list of {@link Track} objects
     */
    @JsonIgnore
    public List<Track> getPlaylist() {
        return (List<Track>) getTracks();
    }

    /**
     * Retrieves a list containing the titles of all tracks in the playlist.
     * * @return a list of strings representing the track names
     */
    @JsonIgnore
    public List<String> getTrackNames() {
        return getTracks().stream()
                .map(Track::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the total duration of the playlist by summing the durations of all its tracks.
     * * @return the total duration of the playlist
     */
    @JsonIgnore
    public int getTotalDuration(){
        return this.tracks.stream()
                .mapToInt(Track::getDuration)
                .sum();
    }

    /**
     * Gets the name of the playlist.
     * * @return the playlist name
     */
    public String getName(){
        return this.playlistName;
    }

    /**
     * Sets the name of the playlist.
     * * @param name the new name for the playlist
     */
    public void setName(String name) {
        this.playlistName = name;
    }

    /**
     * Gets the play count of the track.
     *
     * @return The number of times the playlist has been played.
     */
    public int getPlayCount() {
        return playCount;
    }

    /**
     * Increments the play count of the playlist.
     *
     */
    public void incrementPlayCount() {
        this.playCount++;
    }

    /**
     * Returns a string representation of the playlist, including its name and track count.
     * * @return a formatted string with the playlist name and number of songs
     */
    @Override
    public String toString() {
        int trackCount = (this.tracks != null && !this.tracks.isEmpty())
                ? this.tracks.size()
                : (this.loadedTrackIds != null ? this.loadedTrackIds.size() : 0);
        return this.playlistName + " (" + trackCount + " songs)" + " (" + playCount + " playCount)";
    }

    /**
     * Gets the collection of tracks.
     * Overridden and annotated with @JsonIgnore to prevent serialization of the base collection.
     * * @return a collection of {@link Track} objects
     */
    @Override
    @JsonIgnore
    public Collection<Track> getTracks(){
        return super.getTracks();
    }

    @Override
    public void clear() {
        super.clear();
        loadedTrackIds.clear();
    }

    /**
     * Inserts a track at the specified position, shifting the track currently at that
     * position (and any subsequent tracks) to the right. Used to restore a track to
     * its original index when an undo is performed.
     *
     * @param index the position at which to insert the track.
     * @param track the track to insert.
     */

    @JsonIgnore
    public void insertTrack(int index, Track track){
        ((List<Track>) this.tracks).add(index, track);
    }

    /**
     * Returns the position of the given track in the playlist, or -1 if absent.
     *
     * @param track the track to locate.
     * @return the zero-based index of the track, or -1 if not found.
     */
    @JsonIgnore
    public int indexOf(Track track) {
        return ((List<Track>) this.tracks).indexOf(track);
    }

    /**
     * Captures the current ordered tracks of the playlist into an immutable memento,
     * for later restoration on undo.
     *
     * @return a snapshot of the playlist's track list.
     */
    @JsonIgnore
    public PlaylistMemento snapshot() {
        return new PlaylistMemento(this.tracks);
    }

    /**
     * Restores the playlist to a previously captured state, replacing its tracks
     * (and their order) with the snapshot's.
     *
     * @param memento the state to restore; ignored if null.
     */
    @JsonIgnore
    public void restore(PlaylistMemento memento) {
        if (memento == null) return;
        this.tracks.clear();
        this.tracks.addAll(memento.getTracks());
    }
}