package it.unisa.gruppo7.musicplayer.playlist.strategy;

/**
 * Criterio di generazione di una playlist automatica.
 * <p>
 * Ogni valore porta l'etichetta UI (in italiano) mostrata nel dialog di
 * generazione: in questo modo la mappatura etichetta&rarr;criterio vive in un
 * unico punto ({@link #fromLabel(String)}) invece di essere ripetuta nei
 * controller. Sostituisce il precedente criterio "stringly-typed".
 *
 * @author Gruppo 7
 */
public enum GenerationCriterion {
    /** Generazione per genere musicale. */
    GENRE("Genere"),
    /** Generazione per anno di pubblicazione. */
    YEAR("Anno"),
    /** Generazione per tag. */
    TAG("Tag");

    private final String label;

    GenerationCriterion(String label) {
        this.label = label;
    }

    /**
     * @return l'etichetta UI (in italiano) associata al criterio.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Risolve il criterio a partire dall'etichetta UI mostrata nel dialog.
     *
     * @param label l'etichetta (es. "Genere", "Anno", "Tag")
     * @return il criterio corrispondente
     * @throws IllegalArgumentException se l'etichetta non è riconosciuta
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
