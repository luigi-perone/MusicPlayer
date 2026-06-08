package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Dialog;

/**
 * Generic Builder interface to build dialog interfaces in JavaFX.
 * This interface formalizes the step-by-step assembly sequence of the Builder pattern.
 * Methods must be called in this specific order:
 * buildHeader() -> buildButtons() -> buildContent() -> buildResultConverter()
 *
 * @param <T> The type of data returned by the built dialog when closed.
 * @author Maxim Makhovskyy, Luigi Perone
 */
public interface DialogBuilder<T>{

    /**
     * Sets the dialog frame title and graphic header panel text description.
     */
    void buildHeader();

    /** * Registers the operational standard ButtonTypes on the UI DialogPane.
     * Must be called before buildContent() and buildResultConverter().
     */
    void buildButtons();

    /**
     * Builds, styles, and attaches the main graphical content nodes inside the dialog pane layout body.
     * Requires buildButtons() to have been called first.
     */
    void buildContent();

    /**
     * Registers the matching ResultConverter function layer that maps UI click actions
     * to the dialog's strongly-typed object payload return parameters.
     * Requires buildButtons() to have been called first.
     */
    void buildResultConverter();

    /**
     * Retrieves the fully assembled and ready-to-render concrete JavaFX Dialog instance framework.
     * Should only be called after all build processing steps are complete.
     *
     * @return The assembled Dialog container object.
     */
    Dialog<T> getResult();
}