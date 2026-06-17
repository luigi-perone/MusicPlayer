package it.unisa.gruppo7.musicplayer.undo;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UndoToast}, focusing on its behaviour when no toast is
 * currently displayed.
 */
class UndoToastTest {

    /** Ensures no toast is showing before each test. */
    @BeforeEach
    void reset() {
        UndoToast.hide();
    }

    /** Verifies that triggering undo with no active toast is a safe no-op. */
    @Test
    @DisplayName("triggerUndo with no toast showing is a safe no-op")
    void triggerUndo_noActiveToast_doesNothing() {
        assertDoesNotThrow(UndoToast::triggerUndo,"Ctrl+Z outside the undo window must do nothing, not fail");
    }

    /** Verifies that hiding with no active toast is a safe no-op. */
    @Test
    @DisplayName("hide with no toast showing is a safe no-op")
    void hide_noActiveToast_doesNothing() {
        assertDoesNotThrow(UndoToast::hide,"dismissing when nothing is shown must be harmless");
    }
}