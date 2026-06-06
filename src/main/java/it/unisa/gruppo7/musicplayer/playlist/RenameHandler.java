package it.unisa.gruppo7.musicplayer.playlist;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

public class RenameHandler {

    private final Label    nameLabel;
    private final TextField nameField;
    private final Runnable  onConfirm;

    public RenameHandler(Label nameLabel, TextField nameField, Runnable onConfirm) {
        this.nameLabel = nameLabel;
        this.nameField = nameField;
        this.onConfirm = onConfirm;

        nameLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                enter();
        });
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)  apply();
            if (e.getCode() == KeyCode.ESCAPE) exit();
        });
        nameField.focusedProperty()
                .addListener((obs, was, is) -> { if (!is) exit(); });
    }

    private void enter() {
        nameField.setText(nameLabel.getText());
        nameField.selectAll();
        nameLabel.setVisible(false);
        nameLabel.setManaged(false);
        nameField.setVisible(true);
        nameField.setManaged(true);
        nameField.requestFocus();
    }

    private void apply() {
        onConfirm.run();
        exit();
    }

    private void exit() {
        nameField.setVisible(false);
        nameField.setManaged(false);
        nameLabel.setVisible(true);
        nameLabel.setManaged(true);
    }
}
