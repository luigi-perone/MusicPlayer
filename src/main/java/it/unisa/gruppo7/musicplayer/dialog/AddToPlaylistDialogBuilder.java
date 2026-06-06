package it.unisa.gruppo7.musicplayer.dialog;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class AddToPlaylistDialogBuilder implements DialogBuilder<String> {

    private final List<String> playlistNames;
    private final List<Track>  selectedTracks;

    private Dialog<String>  dialog;
    private ButtonType      confirmType;
    private ListView<String> playlistView;

    public AddToPlaylistDialogBuilder(List<String> playlistNames,
                                List<Track> selectedTracks) {
        this.playlistNames  = playlistNames;
        this.selectedTracks = selectedTracks;
        this.dialog         = new Dialog<>();
    }

    @Override
    public void buildHeader() {
        dialog.setTitle("Aggiungi alla playlist");
        dialog.setHeaderText(
            selectedTracks.size() == 1
                ? "Scegli la playlist di destinazione per \""
                  + selectedTracks.get(0).getTitle() + "\""
                : "Scegli la playlist di destinazione per "
                  + selectedTracks.size() + " tracce"
        );
    }

    @Override
    public void buildButtons() {
        confirmType = new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane()
              .getButtonTypes()
              .addAll(confirmType, ButtonType.CANCEL);
    }

    @Override
    public void buildContent() {
        playlistView = new ListView<>();
        playlistView.getItems().addAll(playlistNames);
        playlistView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playlistView.setPrefSize(340, 220);

        Label hint = new Label("Seleziona una playlist");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");

        playlistView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<String>) c -> {
                    String chosen = playlistView.getSelectionModel().getSelectedItem();
                    hint.setText(chosen == null ? "Seleziona una playlist" : "→  " + chosen);
                });

        Button addButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        addButton.setDisable(true);
        playlistView.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, now) -> addButton.setDisable(now == null));

        VBox content = new VBox(8, playlistView, hint);
        content.setPadding(new Insets(8, 0, 0, 0));
        dialog.getDialogPane().setContent(content);
    }

    @Override
    public void buildResultConverter() {
        final String[] snapshot = {null};

        Button addButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        addButton.addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                e -> snapshot[0] = playlistView.getSelectionModel().getSelectedItem()
        );

        dialog.setResultConverter(btn ->
                btn == confirmType ? snapshot[0] : null
        );
    }

    @Override
    public Dialog<String> getResult() {
        return dialog;
    }
}
