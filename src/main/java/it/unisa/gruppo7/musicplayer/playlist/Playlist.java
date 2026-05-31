package main.java.it.unisa.gruppo7.musicplayer.playlist;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import java.util.Collections;

/**
 * 
 * Models a playlist in the music player, identified by a name 
 * and holding a collection of tracks.
 * 
 * @author Maxim Makhovskyy
 */

public class Playlist extends TrackCollection{
    private String playlistName;

    /**
     * Constructor of the class Playlist
     * 
     * @param playlistName the name of the playlist
     */
    public Playlist(String playlistName){
        this.tracks = new ArrayList<>();
        this.playlistName = playlistName;
    }

    public List<Track> getPlaylist() {
        return (List<Track>) this.tracks;
    }

    public int getTotalDuration(){
        return this.tracks.stream()
                        .mapToInt(Track::getDuration)
                        .sum();
    }

    public String getName(){
        return this.playlistName;
    }
}
