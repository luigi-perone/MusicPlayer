package it.unisa.gruppo7.musicplayer.playlist.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a batch track-addition operation.
 * Carries both the counts and the duplicate titles so the UI
 * can build a precise, localised message without touching the service.
 */
public class AdditionResult {

    private final int        added;
    private final List<String> skippedTitles;

    /**
     * Constructs an AdditionResult with the specified number of added tracks
     * and the list of skipped track titles.
     *
     * @param added         the number of tracks successfully added
     * @param skippedTitles the list of track titles that were skipped
     */
    public AdditionResult(int added, List<String> skippedTitles) {
        this.added         = added;
        this.skippedTitles = Collections.unmodifiableList(new ArrayList<>(skippedTitles));
    }

    /**
     * Gets the number of tracks successfully added.
     * * @return the number of inserted tracks
     */
    public int getAdded() { return added; }

    /** * Gets the titles of tracks that were already in the playlist and therefore skipped.
     * * @return an unmodifiable list of skipped track titles
     */
    public List<String> getSkippedTitles() { return skippedTitles; }

    /**
     * Checks if any tracks were successfully added.
     * * @return true if at least one track was added, false otherwise
     */
    public boolean hasAdded()   { return added > 0; }

    /**
     * Checks if any tracks were skipped during the addition process.
     * * @return true if at least one track was skipped, false otherwise
     */
    public boolean hasSkipped() { return !skippedTitles.isEmpty(); }
}