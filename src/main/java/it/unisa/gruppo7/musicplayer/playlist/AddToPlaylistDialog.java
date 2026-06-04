package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.util.List;

public class AddToPlaylistDialog {

    private final PlaylistService playlistService;
    private final List<Track>     selectedTracks;

    public AddToPlaylistDialog(PlaylistService playlistService,
                               List<Track> selectedTracks) {
        this.playlistService = playlistService;
        this.selectedTracks  = selectedTracks;
    }

    public void show() {
        List<String> playlistNames = playlistService.getPlaylistNames();

        if (playlistNames.isEmpty()) {
            showInfo("Nessuna playlist disponibile",
                    "Non ci sono ancora playlist. Creane prima una dalla barra laterale.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Aggiungi alla playlist");
        dialog.setHeaderText(buildHeader());

        ButtonType confirmType =
                new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(confirmType, ButtonType.CANCEL);

        ListView<String> playlistView = new ListView<>();
        playlistView.getItems().addAll(playlistNames);
        playlistView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playlistView.setPrefSize(340, 220);

        Label hint = new Label("Seleziona una playlist");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");
        playlistView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<String>) c -> {
                    String chosen =
                            playlistView.getSelectionModel().getSelectedItem();
                    hint.setText(chosen == null ? "Seleziona una playlist" : "→  " + chosen);
                });

        Button addButton =
                (Button) dialog.getDialogPane().lookupButton(confirmType);
        addButton.setDisable(true);
        playlistView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> addButton.setDisable(now == null));

        final String[] snapshot = {null};
        addButton.addEventFilter(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> snapshot[0] = playlistView.getSelectionModel().getSelectedItem());

        dialog.getDialogPane().setContent(
                buildContent(playlistView, hint));

        dialog.setResultConverter(btn ->
                btn == confirmType ? snapshot[0] : null);

        dialog.showAndWait().ifPresent(playlistName -> {
            if (playlistName == null) return;

            try {
                AdditionResult result =
                        playlistService.addTracksToPlaylist(playlistName, selectedTracks);
                showResultFeedback(result, playlistName);
            } catch (IllegalArgumentException e) {
                showError("Operazione fallita", e.getMessage());
            }
        });
    }

    private void showResultFeedback(AdditionResult result, String playlistName) {
        if (result.hasAdded() && !result.hasSkipped()) {
            showInfo("Completato",
                    result.getAdded() + " traccia/e aggiunta/e a \"" + playlistName + "\".");

        } else if (result.hasAdded() && result.hasSkipped()) {
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            showWarning("Aggiunta parziale",
                    result.getAdded() + " traccia/e aggiunta/e a \"" + playlistName + "\".\n\n"
                            + "Le seguenti tracce erano già presenti e sono state ignorate:\n"
                            + "  • " + skippedList);

        } else {
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            showWarning("Già nella playlist",
                    "Tutte le tracce selezionate sono già in \"" + playlistName + "\":\n"
                            + "  • " + skippedList);
        }
    }

    private String buildHeader() {
        return selectedTracks.size() == 1
                ? "Scegli la playlist di destinazione per \""
                  + selectedTracks.get(0).getTitle() + "\""
                : "Scegli la playlist di destinazione per "
                  + selectedTracks.size() + " tracce";
    }

    private javafx.scene.layout.VBox buildContent(ListView<String> listView,
                                                  Label hint) {
        javafx.scene.layout.VBox box =
                new javafx.scene.layout.VBox(8, listView, hint);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}