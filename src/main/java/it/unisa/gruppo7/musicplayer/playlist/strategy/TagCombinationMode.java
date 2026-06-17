package it.unisa.gruppo7.musicplayer.playlist.strategy;

/** Defines how multiple tags are combined during playlist generation. */
public enum TagCombinationMode {
    /** A track must carry all of the selected tags. */
    ALL,
    /** A track must carry at least one of the selected tags. */
    ANY
}
