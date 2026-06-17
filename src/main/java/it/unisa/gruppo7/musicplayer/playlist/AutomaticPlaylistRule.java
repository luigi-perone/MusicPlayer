package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Set;

import it.unisa.gruppo7.musicplayer.playlist.strategy.GenerationCriterion;
import it.unisa.gruppo7.musicplayer.playlist.strategy.TagCombinationMode;
import it.unisa.gruppo7.musicplayer.track.TrackTag;

/**
 * Describes the persisted criteria used to (re)generate an automatic playlist.
 * <p>
 * The rule stores the generation {@code criterion} (e.g. genre, year or tag),
 * the optional {@code target} value for that criterion, and, when the criterion
 * is tag-based, the set of {@link TrackTag tags} together with the
 * {@link TagCombinationMode} that controls how they are combined.
 */
public class AutomaticPlaylistRule {
    /** The generation criterion ({@link GenerationCriterion#GENRE}, {@code YEAR} or {@code TAG}). */
    public GenerationCriterion criterion;
    /** The target value for the criterion (e.g. the genre name or year). */
    public String target;
    /** The tags used by a tag-based rule. */
    public Set<TrackTag> tags;
    /** The mode used to combine the selected tags. */
    public TagCombinationMode combinationMode;

    /**
     * Creates an empty rule. Fields are expected to be populated afterwards,
     * typically through the setters during deserialization.
     */
    public AutomaticPlaylistRule() {
    }

    /**
     * Returns the generation criterion (e.g. genre, year or tag).
     *
     * @return the criterion identifier
     */
    public GenerationCriterion getCriterion() {
        return criterion;
    }

    /**
     * Sets the generation criterion.
     *
     * @param criterion the criterion identifier
     */
    public void setCriterion(GenerationCriterion criterion) {
        this.criterion = criterion;
    }

    /**
     * Returns the target value associated with the criterion.
     *
     * @return the target value, or {@code null} when not applicable
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target value associated with the criterion.
     *
     * @param target the target value
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Returns the tags used by a tag-based rule.
     *
     * @return the set of selected tags, or {@code null} when not applicable
     */
    public Set<TrackTag> getTags() {
        return tags;
    }

    /**
     * Sets the tags used by a tag-based rule.
     *
     * @param tags the set of selected tags
     */
    public void setTags(Set<TrackTag> tags) {
        this.tags = tags;
    }

    /**
     * Returns the mode used to combine the selected tags.
     *
     * @return the tag combination mode
     */
    public TagCombinationMode getCombinationMode() {
        return combinationMode;
    }

    /**
     * Sets the mode used to combine the selected tags.
     *
     * @param combinationMode the tag combination mode
     */
    public void setCombinationMode(TagCombinationMode combinationMode) {
        this.combinationMode = combinationMode;
    }
}
