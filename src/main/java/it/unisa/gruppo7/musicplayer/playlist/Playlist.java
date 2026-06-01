package main.java.it.unisa.gruppo7.musicplayer.playlist;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import java.util.Collections;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * Models a playlist in the music player, identified by a name 
 * and holding a collection of tracks.
 * 
 * @author Maxim Makhovskyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Playlist extends TrackCollection{
    private String playlistName;
    private List<UUID> loadedTrackIds = new ArrayList<>();
    /**
     * Constructor of the class Playlist
     * 
     * @param playlistName the name of the playlist
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

    public List<UUID> getTrackIds() {
        if (this.tracks != null && !this.tracks.isEmpty()) {
            return this.tracks.stream()
                              .map(Track::getId)
                              .collect(Collectors.toList());
        }
        return this.loadedTrackIds;
    }

    @JsonIgnore
    public List<Track> getPlaylist() {
        return (List<Track>) getTracks();
    }

    @JsonIgnore
    public int getTotalDuration(){
        return this.tracks.stream()
                        .mapToInt(Track::getDuration)
                        .sum();
    }

    public String getName(){
        return this.playlistName;
    }

    @Override
    public String toString() {
        int trackCount = (this.tracks != null && !this.tracks.isEmpty()) 
                            ? this.tracks.size() 
                            : (this.loadedTrackIds != null ? this.loadedTrackIds.size() : 0);
        return this.playlistName + " (" + trackCount + " songs)";
    }
}
