package it.unisa.gruppo7.musicplayer.track;

/**
 * Predefined visual tags assignable to tracks.
 * 
 * @author Matteo Postiglione
 */
public enum TrackTag {
    /** Marks a track the user has flagged as a favourite. */
    FAVOURITE("Preferita", "Fav"),
    /** Marks a track with explicit content. */
    EXPLICIT("Esplicita", "Exp"),
    /** Marks a recently released track. */
    NEW_RELEASE("Nuova uscita", "New"),
    /** Marks a track rated as one of the best. */
    BEST("best","best"),
    /** Marks a track rated as one of the worst. */
    WORST("worst","worst");

    private final String displayName;
    private final String shortLabel;

    /**
     * Associates the tag with its display name and short label.
     *
     * @param displayName the full, user-facing name of the tag
     * @param shortLabel  the compact label used in dense UI elements
     */
    TrackTag(String displayName, String shortLabel) {
        this.displayName = displayName;
        this.shortLabel = shortLabel;
    }

    /**
     * Returns the full, user-facing name of the tag.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the compact label used in dense UI elements.
     *
     * @return the short label
     */
    public String getShortLabel() {
        return shortLabel;
    }
}