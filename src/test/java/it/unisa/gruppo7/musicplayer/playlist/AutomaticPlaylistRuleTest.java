package it.unisa.gruppo7.musicplayer.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unisa.gruppo7.musicplayer.playlist.strategy.GenerationCriterion;
import it.unisa.gruppo7.musicplayer.playlist.strategy.TagCombinationMode;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the JSON persistence of AutomaticPlaylistRule.
 * Verifies that tag and genre rules retain their configuration
 * after serialization and deserialization.
 *
 * @author Matteo Postiglione
 */

class AutomaticPlaylistRuleTest {
    private final ObjectMapper mapper = new ObjectMapper();
    /** 
     * Verifies that a tag rule retains its data after JSON serialization and deserialization. */
    @Test
    void tagRuleSurvivesJsonRoundTrip() throws Exception {
        AutomaticPlaylistRule rule = new AutomaticPlaylistRule();
        rule.criterion = GenerationCriterion.TAG;
        rule.tags = EnumSet.of(TrackTag.FAVOURITE, TrackTag.NEW_RELEASE);
        rule.combinationMode = TagCombinationMode.ALL;

        String json = mapper.writeValueAsString(rule);
        AutomaticPlaylistRule restored = mapper.readValue(
                json, AutomaticPlaylistRule.class);

        assertEquals(GenerationCriterion.TAG, restored.criterion);
        assertEquals(rule.tags, restored.tags);
        assertEquals(TagCombinationMode.ALL, restored.combinationMode);
        assertNull(restored.target);
    }
    /** 
     * Verifies that a genre rule retains its data after JSON serialization and deserialization. */
    @Test
    void genreRuleSurvivesJsonRoundTrip() throws Exception {
        AutomaticPlaylistRule rule = new AutomaticPlaylistRule();
        rule.criterion = GenerationCriterion.GENRE;
        rule.target = "Rock";

        String json = mapper.writeValueAsString(rule);
        AutomaticPlaylistRule restored = mapper.readValue(
                json, AutomaticPlaylistRule.class);

        assertEquals(GenerationCriterion.GENRE, restored.criterion);
        assertEquals("Rock", restored.target);
        assertNull(restored.tags);
        assertNull(restored.combinationMode);
    }
}

