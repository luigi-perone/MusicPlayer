package it.unisa.gruppo7.musicplayer.dialog;

import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
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

    /**
     * Nested controller module managing independent workflow components
     * to add selected tracks into playlist containers.
     */
    public static class AddToPlaylistDialog {

        private final PlaylistService playlistService;
        private final List<Track>     selectedTracks;

        /**
         * Constructs an AddToPlaylistDialog helper controller context instance.
         *
         * @param playlistService The playlist data manager backend engine layer service.
         * @param selectedTracks  The list collection of tracks being reassigned.
         */
        public AddToPlaylistDialog(PlaylistService playlistService,
                                   List<Track> selectedTracks) {
            this.playlistService = playlistService;
            this.selectedTracks  = selectedTracks;
        }

        /**
         * Launches execution operations to construct, validate, and render the selection interface alert dialogue.
         */
        public void show() {
            List<Playlist> playlists = playlistService.getPlaylists();

            if (playlists.isEmpty()) {
                showInfo("Nessuna playlist disponibile",
                        "Non ci sono ancora playlist. Creane prima una dalla barra laterale.");
                return;
            }

            Dialog<Playlist> dialog = new Dialog<>();
            dialog.setTitle("Aggiungi alla playlist");
            dialog.setHeaderText(buildHeader());

            ButtonType confirmType =
                    new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane()
                    .getButtonTypes()
                    .addAll(confirmType, ButtonType.CANCEL);

            ListView<Playlist> playlistView = new ListView<>();
            playlistView.getItems().addAll(playlists);
            playlistView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            playlistView.setPrefSize(340, 220);
            playlistView.setCellFactory(lv -> new ListCell<Playlist>() {
                @Override
                protected void updateItem(Playlist item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : item.getName());
                }
            });

            Label hint = new Label("Seleziona una playlist");
            hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6e6e73;");
            playlistView.getSelectionModel().getSelectedItems()
                    .addListener((ListChangeListener<Playlist>) c -> {
                        Playlist chosen = playlistView.getSelectionModel().getSelectedItem();
                        hint.setText(chosen == null ? "Seleziona una playlist" : "→  " + chosen.getName());
                    });

            Button addButton =
                    (Button) dialog.getDialogPane().lookupButton(confirmType);
            addButton.setDisable(true);
            playlistView.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, now) -> addButton.setDisable(now == null));

            final Playlist[] snapshot = {null};
            addButton.addEventFilter(
                    javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                    e -> snapshot[0] = playlistView.getSelectionModel().getSelectedItem());

            dialog.getDialogPane().setContent(buildContent(playlistView, hint));

            dialog.setResultConverter(btn ->
                    btn == confirmType ? snapshot[0] : null);

            dialog.showAndWait().ifPresent(playlist -> {

                try {
                    AdditionResult result =
                            playlistService.addTracksToPlaylist(playlist, selectedTracks);
                    showResultFeedback(result, playlist.getName());
                } catch (IllegalArgumentException e) {
                    showError("Operazione fallita", e.getMessage());
                }
            });
        }

        /**
         * Parses insertion transaction results to provide matching alert feedback notifications.
         *
         * @param result       The structural transaction log report summary outcome tracker.
         * @param playlistName The targeted destination profile name tag string label.
         */
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

        /**
         * Assembles context-dependent window display descriptions matching selection size scales.
         *
         * @return The formatted textual label description instruction sequence.
         */
        private String buildHeader() {
            return selectedTracks.size() == 1
                    ? "Scegli la playlist di destinazione per \""
                      + selectedTracks.get(0).getTitle() + "\""
                    : "Scegli la playlist di destinazione per "
                      + selectedTracks.size() + " tracce";
        }

        /**
         * Generates the structured graphic layout enclosing list objects.
         *
         * @param listView The configured interactive ListView entity node reference.
         * @param hint     The feedback information layout tracker tag text element field node.
         * @return A styled vertical VBox assembly grid container box.
         */
        private VBox buildContent(ListView<Playlist> listView, Label hint) {
            VBox box =
                    new VBox(8, listView, hint);
            box.setPadding(new Insets(8, 0, 0, 0));
            return box;
        }

        /**
         * Internal helper to trigger information modals.
         *
         * @param title   Title string text.
         * @param message Description parameters body details content text string.
         */
        private void showInfo(String title, String message) {
            DialogUtils.showInfo(title, message);
        }

        /**
         * Internal helper to trigger notice warnings alerts.
         *
         * @param title   Title configuration strip string.
         * @param message Text paragraph explaining context concerns details.
         */
        private void showWarning(String title, String message) {
            DialogUtils.showWarning(title, message);
        }

        /**
         * Internal auxiliary function to trigger blocking error status alerts.
         *
         * @param title   Frame header title parameter.
         * @param message Core analysis parameter failure reason message.
         */
        private void showError(String title, String message) {
            DialogUtils.showError(title, message);
        }
    }
}