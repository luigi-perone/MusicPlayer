package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.util.List;

/**
 * Reusable dialog: given a pre-selected list of tracks, lets the user pick a
 * destination playlist and confirms the operation.
 *
 * Handles four cases explicitly:
 * <ul>
 *   <li>All tracks new       → success notification with count</li>
 *   <li>Some already present → partial success + list of skipped titles</li>
 *   <li>All already present  → dedicated warning, nothing added</li>
 *   <li>Cancelled            → silent close, no side effects</li>
 * </ul>
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
     * Opens the dialog (blocking) and processes the result.
     * Safe to call even when no playlists exist: shows a dedicated alert instead.
     */
    public void show() {
        List<String> playlistNames = playlistService.getPlaylistNames();

        // --- guard: no playlists yet ---
        if (playlistNames.isEmpty()) {
            showInfo("No playlist available",
                    "There are no playlists yet. Create one from the sidebar first.");
            return;
        }

        // --- build dialog ---
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add to playlist");
        dialog.setHeaderText(buildHeader());

        ButtonType confirmType =
                new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(confirmType, ButtonType.CANCEL);

        ListView<String> playlistView = new ListView<>();
        playlistView.getItems().addAll(playlistNames);
        playlistView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playlistView.setPrefSize(340, 220);

        Label hint = new Label("Select a playlist");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");
        playlistView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<String>) c -> {
                    String chosen =
                            playlistView.getSelectionModel().getSelectedItem();
                    hint.setText(chosen == null ? "Select a playlist" : "→  " + chosen);
                });

        // disable Add until a playlist is chosen
        Button addButton =
                (Button) dialog.getDialogPane().lookupButton(confirmType);
        addButton.setDisable(true);
        playlistView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> addButton.setDisable(now == null));

        // snapshot on MOUSE_PRESSED — immune to focus-shift deselection
        final String[] snapshot = {null};
        addButton.addEventFilter(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                e -> snapshot[0] = playlistView.getSelectionModel().getSelectedItem());

        dialog.getDialogPane().setContent(
                buildContent(playlistView, hint));

        dialog.setResultConverter(btn ->
                btn == confirmType ? snapshot[0] : null);

        // --- process result ---
        dialog.showAndWait().ifPresent(playlistName -> {
            // null means the snapshot was never set (shouldn't happen, but guard anyway)
            if (playlistName == null) return;

            try {
                AdditionResult result =
                        playlistService.addTracksToPlaylist(playlistName, selectedTracks);
                showResultFeedback(result, playlistName);
            } catch (IllegalArgumentException e) {
                showError("Operation failed", e.getMessage());
            }
        });
        // ifPresent fires only on OK; CANCEL falls through here → no side effects
    }

    // -------------------------------------------------------------------------
    // Feedback helpers — one method per case
    // -------------------------------------------------------------------------

    private void showResultFeedback(AdditionResult result, String playlistName) {
        if (result.hasAdded() && !result.hasSkipped()) {
            // Case 1 — all tracks added successfully
            showInfo("Done",
                    result.getAdded() + " track(s) added to \"" + playlistName + "\".");

        } else if (result.hasAdded() && result.hasSkipped()) {
            // Case 2 — partial: some added, some skipped
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            showWarning("Partial addition",
                    result.getAdded() + " track(s) added to \"" + playlistName + "\".\n\n"
                            + "The following track(s) were already present and skipped:\n"
                            + "  • " + skippedList);

        } else {
            // Case 3 — nothing added: all were duplicates
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            showWarning("Already in playlist",
                    "All selected track(s) are already in \"" + playlistName + "\":\n"
                            + "  • " + skippedList);
        }
    }

    // -------------------------------------------------------------------------
    // UI builders
    // -------------------------------------------------------------------------

    private String buildHeader() {
        return selectedTracks.size() == 1
                ? "Choose the destination playlist for \""
                  + selectedTracks.get(0).getTitle() + "\""
                : "Choose the destination playlist for "
                  + selectedTracks.size() + " tracks";
    }

    private javafx.scene.layout.VBox buildContent(ListView<String> listView,
                                                  Label hint) {
        javafx.scene.layout.VBox box =
                new javafx.scene.layout.VBox(8, listView, hint);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    // -------------------------------------------------------------------------
    // Alert helpers
    // -------------------------------------------------------------------------

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