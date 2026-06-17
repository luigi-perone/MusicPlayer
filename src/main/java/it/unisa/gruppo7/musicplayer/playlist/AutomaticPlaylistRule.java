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

    public String getCriterion() {
        return criterion;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Set<TrackTag> getTags() {
        return tags;
    }

    public void setTags(Set<TrackTag> tags) {
        this.tags = tags;
    }

    public TagCombinationMode getCombinationMode() {
        return combinationMode;
    }

    public void setCombinationMode(TagCombinationMode combinationMode) {
        this.combinationMode = combinationMode;
    }
}
