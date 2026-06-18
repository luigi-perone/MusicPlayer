package it.unisa.gruppo7.musicplayer.playlist.strategy;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import it.unisa.gruppo7.musicplayer.playlist.AutomaticPlaylistRule;

/**
 * Factory that maps the criterion of an {@link AutomaticPlaylistRule} to the
 * matching {@link PlaylistGenerationStrategy}.
 * <p>
 * Centralizes in a single registry the selection logic that used to be duplicated
 * across two diverging {@code switch} statements ({@code PlaylistService} keyed on
 * the codes {@code "GENRE"/"YEAR"/"TAG"} and {@code PlaylistSidebarController}
 * keyed on the labels {@code "Genere"/"Anno"/"Tag"}). Adding a criterion now
 * requires only a new value in {@link GenerationCriterion} and an entry in the
 * registry (Open/Closed Principle).
 *
 * @author Gruppo 7
 */
public final class PlaylistGenerationStrategyFactory {

    /** Maps each criterion to the constructor of its matching strategy. */
    private static final Map<GenerationCriterion, Function<AutomaticPlaylistRule, PlaylistGenerationStrategy>> REGISTRY =
            new EnumMap<>(GenerationCriterion.class);

    static {
        REGISTRY.put(GenerationCriterion.GENRE, rule -> new GenreGenerationStrategy(rule.getTarget()));
        REGISTRY.put(GenerationCriterion.YEAR,  rule -> new YearGenerationStrategy(Integer.parseInt(rule.getTarget())));
        REGISTRY.put(GenerationCriterion.TAG,   rule -> new TagGenerationStrategy(rule.getTags(), rule.getCombinationMode()));
    }

    private PlaylistGenerationStrategyFactory() {
        // Utility class: not meant to be instantiated.
    }

    /**
     * Creates the generation strategy matching the rule's criterion.
     *
     * @param rule the rule describing the generation criterion
     * @return the matching {@link PlaylistGenerationStrategy}
     * @throws IllegalArgumentException if the criterion is not supported
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
