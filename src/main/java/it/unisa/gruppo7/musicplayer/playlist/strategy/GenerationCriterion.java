package it.unisa.gruppo7.musicplayer.playlist.strategy;

/**
 * Criterion used to generate an automatic playlist.
 * <p>
 * Each value carries the user-facing UI label (in Italian) shown in the
 * generation dialog, so the label&rarr;criterion mapping lives in a single place
 * ({@link #fromLabel(String)}) instead of being repeated across controllers.
 * Replaces the previous "stringly-typed" criterion.
 *
 * @author Gruppo 7
 */
public enum GenerationCriterion {
    /** Generate by music genre. */
    GENRE("Genere"),
    /** Generate by publication year. */
    YEAR("Anno"),
    /** Generate by tag. */
    TAG("Tag");

    private final String label;

    GenerationCriterion(String label) {
        this.label = label;
    }

    /**
     * @return the user-facing UI label (in Italian) associated with this criterion.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves the criterion from the UI label shown in the dialog.
     *
     * @param label the label (e.g. "Genere", "Anno", "Tag")
     * @return the matching criterion
     * @throws IllegalArgumentException if the label is not recognized
     */
    public static GenerationCriterion fromLabel(String label) {
        for (GenerationCriterion criterion : values()) {
            if (criterion.label.equalsIgnoreCase(label)) {
                return criterion;
            }
        }
        throw new IllegalArgumentException("Criterio non supportato: " + label);
    }
}
