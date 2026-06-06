package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Dialog;

/**
 * Generic Builder to build dialog interfaces in JavaFX, this interface implements
 * the Builder pattern.
 * Methods must be called in this order:
 * buildHeader() -> buildButtons() -> buildContent() -> buildResultConverter()
 */
public interface DialogBuilder<T>{
    /**
     * Sets the dialog title and header text.
     */
    void buildHeader();

    /** 
     * Registers the ButtonTypes on the DialogPane.
     * Must be called before buildContent() and buildResultConverter().
     */
    void buildButtons();

    /**
     * Builds and sets the main content of the dialog.
     * Requires buildButtons() to have been called first.
     */
    void buildContent();

    /**
     * Registers the ResultConverter that maps the pressed ButtonType
     * to the dialog's return value.
     * Requires buildButtons() to have been called first.
     */
    void buildResultConverter();

    /**
     * Returns the assembled dialog, ready to be shown.
     * Should only be called after all build steps are complete.
     * @return <T> type of the value returned by the dialog when closed.
     */
    Dialog<T> getResult();
}
