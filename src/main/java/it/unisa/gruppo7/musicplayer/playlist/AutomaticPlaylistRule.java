package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Set;

import it.unisa.gruppo7.musicplayer.playlist.strategy.TagCombinationMode;
import it.unisa.gruppo7.musicplayer.track.TrackTag;

public class AutomaticPlaylistRule {
    public String criterion;
    public String target;
    public Set<TrackTag> tags;
    public TagCombinationMode combinationMode;

    public AutomaticPlaylistRule() {
    }
}
