package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Dialog;

/**
 * Director class part of the Builder design pattern implementation.
 * It manages the structural execution order of the dialog interface construction steps.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class DialogDirector {

    /**
     * Coordinates the incremental construction sequence on a given builder
     * and extracts the resulting fully operational JavaFX Dialog framework instance.
     *
     * @param <T>     The expected structural output data return parameter type.
     * @param builder The targeted abstract dialog builder tool structure.
     * @return A ready-to-render JavaFX Dialog window frame container.
     */
    public <T> Dialog<T> construct(DialogBuilder<T> builder) {
        builder.buildHeader();
        builder.buildButtons();
        builder.buildContent();
        builder.buildResultConverter();

        return builder.getResult();
    }
}