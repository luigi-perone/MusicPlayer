package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Dialog;

public interface DialogBuilder<T>{
    void buildHeader();
    void buildButtons();
    void buildContent();
    void buildResultConverter();
    Dialog<T> getResult();
}
