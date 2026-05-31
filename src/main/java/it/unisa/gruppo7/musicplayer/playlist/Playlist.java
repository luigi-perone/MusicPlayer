package main.java.it.unisa.gruppo7.musicplayer.playlist;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import it.unisa.gruppo7.musicplayer.track.Track;

/**
 * 
 * This class represents the model of the playlist of the music player. 
 * A playlist contains a list of tracks. 
 * 
 * @author Maxim Makhovskyy
 */

public class Playlist{
    private List<Track> playlist;
    private String playlistName;

    /**
     * Constructor of the class Playlist
     * 
     * @param playlistName the name of the playlist
     */
    public Playlist(String playlistName){
        playlist = new ArrayList<>();
        this.playlistName = playlistName;
    }

    public void addTrack(Track t){
        playlist.add(t);
    }

    public boolean removeTrack(Track t){
        return playlist.remove(t);
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getTotalDuration(){
        return playlist.stream()
                        .mapToInt(Track::getDuration)
                        .sum();
    }

    public int getCntTrack(){
        return playlist.size();
    }

    public String getName(){
        return this.playlistName;
    }
}
