package it.unisa.gruppo7.musicplayer.playlist;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

/**
 * Handles the UI interactions for renaming an item.
 * It manages the transition between a Label (view mode) and a TextField (edit mode).
 *
 * @author Maxim Makhovskyy
 */
public class RenameHandler {

    private final Label    nameLabel;
    private final TextField nameField;
    private final Runnable  onConfirm;

    /**
     * Constructs a new RenameHandler and initializes the event listeners.
     *
     * @param nameLabel The label displaying the current name.
     * @param nameField The text field used for entering the new name.
     * @param onConfirm The action to execute when the rename is confirmed (e.g., Enter key pressed).
     */
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

    /**
     * Enters edit mode.
     * Hides the label, shows the text field, copies the current text,
     * selects it all, and requests focus.
     */
    private void enter() {
        nameField.setText(nameLabel.getText());
        nameField.selectAll();
        nameLabel.setVisible(false);
        nameLabel.setManaged(false);
        nameField.setVisible(true);
        nameField.setManaged(true);
        nameField.requestFocus();
    }

    /**
     * Applies the changes by running the confirmation callback, then exits edit mode.
     */
    private void apply() {
        onConfirm.run();
        exit();
    }

    /**
     * Exits edit mode.
     * Hides the text field and restores the visibility of the label.
     */
    private void exit() {
        nameField.setVisible(false);
        nameField.setManaged(false);
        nameLabel.setVisible(true);
        nameLabel.setManaged(true);
    }
}