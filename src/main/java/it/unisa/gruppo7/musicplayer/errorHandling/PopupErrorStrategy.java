package it.unisa.gruppo7.musicplayer.errorHandling;

import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;

public class PopupErrorStrategy implements ErrorHandlingStrategy {
    @Override
    public void handleError(Exception e) {
        DialogUtils.showError("Error operation", e.getMessage());
    }
}
