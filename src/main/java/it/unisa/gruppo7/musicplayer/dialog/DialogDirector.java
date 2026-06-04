package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Dialog;

public class DialogDirector {
    public <T> Dialog<T> construct(DialogBuilder<T> builder) {
        builder.buildHeader();
        builder.buildButtons();
        builder.buildContent();
        builder.buildResultConverter();
        
        return builder.getResult();
    }
}
