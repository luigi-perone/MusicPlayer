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
public class Playlist extends TrackCollection{
    private String playlistName;
    private List<UUID> loadedTrackIds = new ArrayList<>();

    /**
     * Constructor of the class Playlist.
     * * @param playlistName the name of the playlist
     * @param trackIds the list of track IDs loaded from storage
     */
    @JsonCreator
    public Playlist(
            @JsonProperty("name") String playlistName,
            @JsonProperty("trackIds") List<UUID> trackIds) {
        super("", new ArrayList<>());
        this.playlistName = playlistName;
        if (trackIds != null) {
            this.loadedTrackIds = trackIds;
        }
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
     * Returns a string representation of the playlist, including its name and track count.
     * * @return a formatted string with the playlist name and number of songs
     */
    @Override
    public String toString() {
        int trackCount = (this.tracks != null && !this.tracks.isEmpty())
                ? this.tracks.size()
                : (this.loadedTrackIds != null ? this.loadedTrackIds.size() : 0);
        return this.playlistName + " (" + trackCount + " songs)";
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
}