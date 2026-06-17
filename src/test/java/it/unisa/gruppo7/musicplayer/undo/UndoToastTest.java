package it.unisa.gruppo7.musicplayer.undo;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class UndoToastTest {

    @BeforeEach
    void reset() {
        UndoToast.hide();
    }

    @Test
    @DisplayName("triggerUndo with no toast showing is a safe no-op")
    void triggerUndo_noActiveToast_doesNothing() {
        assertDoesNotThrow(UndoToast::triggerUndo,"Ctrl+Z outside the undo window must do nothing, not fail");
    }

    @Test
    @DisplayName("hide with no toast showing is a safe no-op")
    void hide_noActiveToast_doesNothing() {
        assertDoesNotThrow(UndoToast::hide,"dismissing when nothing is shown must be harmless");
    }
}