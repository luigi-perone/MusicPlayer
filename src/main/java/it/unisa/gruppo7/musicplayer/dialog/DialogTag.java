package it.unisa.gruppo7.musicplayer.dialog;

import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dialog for assigning predefined visual tags to a track.
 * 
 * @author Matteo Postiglione
 */
public final class DialogTag {

    private DialogTag() {
    }

    public static Optional<Set<TrackTag>> show(Track track) {
        Dialog<Set<TrackTag>> dialog = new Dialog<>();
        dialog.setTitle("Gestisci Tag");
        dialog.setHeaderText(track.getTitle());

        ButtonType confirmButtonType = new ButtonType("Conferma", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        VBox content = new VBox(8);
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (TrackTag tag : TrackTag.values()) {
            CheckBox checkBox = new CheckBox(tag.getDisplayName());
            checkBox.setSelected(track.hasTag(tag));
            checkBox.setUserData(tag);
            checkBoxes.add(checkBox);
            content.getChildren().add(checkBox);
        }
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == confirmButtonType) {
                Set<TrackTag> selectedTags = new HashSet<>();
                for (CheckBox checkBox : checkBoxes) {
                    if (checkBox.isSelected()) {
                        selectedTags.add((TrackTag) checkBox.getUserData());
                    }
                }
                return selectedTags;
            }
            return null;
        });

        return dialog.showAndWait();
    }
}