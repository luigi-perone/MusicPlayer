package it.unisa.gruppo7.musicplayer.track;

/**
 * Predefined visual tags assignable to tracks.
 */
public enum TrackTag {
    FAVOURITE("Preferita", "Fav"),
    EXPLICIT("Esplicita", "Exp"),
    NEW_RELEASE("Nuova uscita", "New"),
    BEST("best","best"),
    WORST("worst","worst");

    private final String displayName;
    private final String shortLabel;

    TrackTag(String displayName, String shortLabel) {
        this.displayName = displayName;
        this.shortLabel = shortLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}