package it.unisa.gruppo7.musicplayer.dialog;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

public class TrackSelectionDialogBuilder implements DialogBuilder<List<Track>> {
    
    private Dialog<List<Track>> dialog;
    private ListView<Track> listView;
    private ButtonType addButtonType;
    private List<Track> availableTracks;

    public TrackSelectionDialogBuilder(List<Track> availableTracks) {
        this.availableTracks = availableTracks;
        this.dialog = new Dialog<>();
    }

    @Override
    public void buildHeader() {
        dialog.setTitle("Add tracks");
        dialog.setHeaderText("Select tracks");
    }

    @Override
    public void buildButtons() {
        addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
    }

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

    @Override
    public void buildResultConverter() {
        dialog.setResultConverter(dialogButton -> 
            dialogButton == addButtonType 
                ? new ArrayList<>(listView.getSelectionModel().getSelectedItems()) 
                : null
        );
    }

    @Override
    public Dialog<List<Track>> getResult() {
        return dialog;
    }
}
