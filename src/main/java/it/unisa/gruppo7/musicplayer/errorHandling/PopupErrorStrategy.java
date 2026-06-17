package it.unisa.gruppo7.musicplayer.errorHandling;

import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;

/**
 * An error handling strategy that displays exception messages to the user
 * via a graphical pop-up dialog.
 *
 * @author Maxim Makhovskyy
 */
public class PopupErrorStrategy implements ErrorHandlingStrategy {

    /**
     * Handles the given exception by showing an error dialog with the exception's message.
     *
     * @param e The exception to handle.
     */
    @Override
    public void handleError(Exception e) {
        DialogUtils.showError("Error operation", e.getMessage());
    }
}