package it.unisa.gruppo7.musicplayer.playlist;

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

    public AdditionResult(int added, List<String> skippedTitles) {
        this.added         = added;
        this.skippedTitles = Collections.unmodifiableList(new ArrayList<>(skippedTitles));
    }

    /** Number of tracks actually inserted. */
    public int getAdded() { return added; }

    /** Titles of tracks that were already in the playlist and therefore skipped. */
    public List<String> getSkippedTitles() { return skippedTitles; }

    public boolean hasAdded()   { return added > 0; }
    public boolean hasSkipped() { return !skippedTitles.isEmpty(); }
}