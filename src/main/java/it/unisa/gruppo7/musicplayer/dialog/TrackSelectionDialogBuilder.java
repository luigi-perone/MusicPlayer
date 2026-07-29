package it.unisa.gruppo7.musicplayer.dialog;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete builder implementation for a multiple track selection dialog.
 * This class sets up a multi-selection ListView containing unassigned library tracks.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class TrackSelectionDialogBuilder implements DialogBuilder<List<Track>> {

    private Dialog<List<Track>> dialog;
    private ListView<Track> listView;
    private ButtonType addButtonType;
    private List<Track> availableTracks;

    /**
     * Constructs a TrackSelectionDialogBuilder.
     *
     * @param availableTracks The collection of tracks eligible for display and selection.
     */
    public TrackSelectionDialogBuilder(List<Track> availableTracks) {
        this.availableTracks = availableTracks;
        this.dialog = new Dialog<>();
    }

    /**
     * Assigns standard window frame titles to the dialogue header panel.
     */
    @Override
    public void buildHeader() {
        dialog.setTitle("Aggiungi tracce");
        dialog.setHeaderText("Seleziona tracce");
    }

    /**
     * Generates and pins the operational approval selection button triggers.
     */
    @Override
    public void buildButtons() {
        addButtonType = new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
    }

    /**
     * Configures the inner ListView layout supporting multiple item selection ranges
     * and formats cell display text to show "Title - Author".
     */
    @Override
    public void buildContent() {
        listView = new ListView<>();
        listView.getItems().addAll(availableTracks);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(param -> new ListCell<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getTitle() + " - " + item.getAuthor());
            }
        });
        listView.setPrefSize(350, 400);
        dialog.getDialogPane().setContent(new VBox(listView));
    }

    /**
     * Hooks up conversion algorithms turning active row indices into a list collection output
     * when confirmed via the add button type.
     */
    @Override
    public void buildResultConverter() {
        dialog.setResultConverter(dialogButton ->
                dialogButton == addButtonType
                        ? new ArrayList<>(listView.getSelectionModel().getSelectedItems())
                        : null
        );
    }

    /**
     * Extracts the finished track selector dialogue framework node wrapper.
     *
     * @return The ready-to-use Dialog object container.
     */
    @Override
    public Dialog<List<Track>> getResult() {
        return dialog;
    }
}