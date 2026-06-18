package it.unisa.gruppo7.musicplayer.playlist.strategy;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;

/**
 * Factory che associa al criterio di una {@link AutomaticPlaylistRule} la
 * {@link PlaylistGenerationStrategy} corrispondente.
 * <p>
 * Centralizza in un unico registry la logica di selezione che prima era
 * duplicata in due {@code switch} divergenti ({@code PlaylistService} con i
 * codici {@code "GENRE"/"YEAR"/"TAG"} e {@code PlaylistSidebarController} con le
 * etichette {@code "Genere"/"Anno"/"Tag"}). Aggiungere un criterio richiede ora
 * solo un valore in {@link GenerationCriterion} e una voce nel registry
 * (rispetto del principio Open/Closed).
 *
 * @author Gruppo 7
 */
public final class PlaylistGenerationStrategyFactory {

    /** Mappa criterio &rarr; costruttore della strategia corrispondente. */
    private static final Map<GenerationCriterion, Function<AutomaticPlaylistRule, PlaylistGenerationStrategy>> REGISTRY =
            new EnumMap<>(GenerationCriterion.class);

    static {
        REGISTRY.put(GenerationCriterion.GENRE, rule -> new GenreGenerationStrategy(rule.getTarget()));
        REGISTRY.put(GenerationCriterion.YEAR,  rule -> new YearGenerationStrategy(Integer.parseInt(rule.getTarget())));
        REGISTRY.put(GenerationCriterion.TAG,   rule -> new TagGenerationStrategy(rule.getTags(), rule.getCombinationMode()));
    }

    private PlaylistGenerationStrategyFactory() {
        // classe di utilità: non istanziabile
    }

    /**
     * Crea la strategia di generazione corrispondente al criterio della regola.
     *
     * @param rule la regola che descrive il criterio di generazione
     * @return la {@link PlaylistGenerationStrategy} corrispondente
     * @throws IllegalArgumentException se il criterio non è supportato
     */
    public static PlaylistGenerationStrategy from(AutomaticPlaylistRule rule) {
        Function<AutomaticPlaylistRule, PlaylistGenerationStrategy> creator = REGISTRY.get(rule.getCriterion());
        if (creator == null) {
            throw new IllegalArgumentException(
                    "Criterio automatico non supportato: " + rule.getCriterion());
        }
        return creator.apply(rule);
    }
}
