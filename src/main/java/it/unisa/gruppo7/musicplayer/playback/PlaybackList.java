package it.unisa.gruppo7.musicplayer.playback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unisa.gruppo7.musicplayer.core.TrackCollection;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;

public class PlaybackList extends TrackCollection implements TrackObserver{
    private static final String DEFAULT_PATH = null;

    public PlaybackList(){
        super(DEFAULT_PATH, new ArrayList<>());
    }

    public Track getNextTrack(Track current){
        if (this.tracks == null || this.tracks.isEmpty()) return null;
    
        Iterator<Track> iterator = this.tracks.iterator();

        while (iterator.hasNext()){
            Track t = iterator.next();

            if(t.equals(current)){
                if(iterator.hasNext()){
                    return iterator.next();
                }else{
                    return null;
                }
            }
        }

        return null;
    }

    public Track getPreviousTrack(Track current) {
        if (this.tracks == null || this.tracks.isEmpty()) return null;

        Track previous = null;
        for (Track t : this.tracks) {
            if (t.equals(current)) {
                return previous;
            }
            previous = t;
        }
        return null;
    }

    public void loadTracks(List<Track> tracks) {
        this.clear();
        this.tracks.addAll(tracks);
    }

    public void appendTracks(List<Track> tracks) {
        this.tracks.addAll(tracks);
    }

    public void clear(){
        this.tracks.clear();
    }


    public List<Track> getUpNextQueue(Track current) {
        List<Track> trackList = (List<Track>) this.tracks;
        if (trackList == null || trackList.isEmpty() || current == null) {
            return new ArrayList<>();
        }

        // get the index of the current track
        int currentIndex = trackList.indexOf(current);

        // if the track is not in the list, or it is in the last position, return an empty list
        if (currentIndex == -1 || currentIndex >= trackList.size() - 1) {
            return new ArrayList<>();
        }

        // return a list with only the up next tracks
        return new ArrayList<>(trackList.subList(currentIndex + 1, trackList.size()));
    }


    @Override
    public void onTrackDeleted(Track track){
        if(this.tracks != null){
            this.tracks.remove(track);
        }
    }
}
