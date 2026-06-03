package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Reusable dialog: given a pre-selected list of tracks, lets the user
 * pick a destination playlist and confirms the operation.
 *
 * Usage:
 * <pre>
 *   new AddToPlaylistDialog(playlistService, selectedTracks).show();
 * </pre>
 */
public class AddToPlaylistDialog {

    private final PlaylistService playlistService;
    private final List<Track>     selectedTracks;

    public AddToPlaylistDialog(PlaylistService playlistService,
                               List<Track> selectedTracks) {
        this.playlistService = playlistService;
        this.selectedTracks  = selectedTracks;
    }

    /**
     * Opens the dialog and processes the result.
     * Blocks until the user closes the window (showAndWait semantics).
     */
    public void show() {
        List<String> playlistNames = playlistService.getPlaylistNames();

        // --- guard: no playlists exist yet ---
        if (playlistNames.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("No playlist available");
            info.setHeaderText(null);
            info.setContentText(
                    "There are no playlists yet. Create one from the sidebar first.");
            info.showAndWait();
            return;
        }

        // --- dialog setup ---
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add to playlist");
        dialog.setHeaderText(
                selectedTracks.size() == 1
                        ? "Choose the destination playlist for \"" + selectedTracks.get(0).getTitle() + "\""
                        : "Choose the destination playlist for " + selectedTracks.size() + " tracks"
        );

        ButtonType confirmType =
                new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(confirmType, ButtonType.CANCEL);

        // --- playlist ListView ---
        ListView<String> playlistView = new ListView<>();
        playlistView.getItems().addAll(playlistNames);
        playlistView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playlistView.setPrefSize(340, 220);

        // hint label (updates on selection change)
        Label hint = new Label("Select a playlist");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");

        playlistView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<String>) c -> {
                    String chosen = playlistView.getSelectionModel().getSelectedItem();
                    hint.setText(chosen == null
                            ? "Select a playlist"
                            : "→  " + chosen);
                });

        // disable the Add button until a playlist is chosen
        Button addButton =
                (Button) dialog.getDialogPane().lookupButton(confirmType);
        addButton.setDisable(true);
        playlistView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> addButton.setDisable(now == null));

        // snapshot on MOUSE_PRESSED (before focus shift clears selection)
        final String[] selectionSnapshot = {null};
        addButton.addEventFilter(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> selectionSnapshot[0] =
                        playlistView.getSelectionModel().getSelectedItem()
        );

        VBox content = new VBox(8, playlistView, hint);
        content.setPadding(new Insets(8, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn ->
                btn == confirmType ? selectionSnapshot[0] : null
        );

        // --- process result ---
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(playlistName -> {
            Optional<String> error =
                    playlistService.addTracksToPlaylist(playlistName, selectedTracks);

            if (error.isPresent()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Operation failed");
                alert.setHeaderText(null);
                alert.setContentText(error.get());
                alert.showAndWait();
            } else {
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Done");
                ok.setHeaderText(null);
                ok.setContentText(
                        selectedTracks.size() + " track(s) added to \"" + playlistName + "\"");
                ok.showAndWait();
            }
        });
    }
}