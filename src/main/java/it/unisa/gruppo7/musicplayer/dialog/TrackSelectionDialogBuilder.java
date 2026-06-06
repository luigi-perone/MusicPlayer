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
        dialog.setTitle("Aggiungi tracce");
        dialog.setHeaderText("Seleziona tracce");
    }

    @Override
    public void buildButtons() {
        addButtonType = new ButtonType("Aggiungi", ButtonBar.ButtonData.OK_DONE);
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

    public static class AddToPlaylistDialog {

        private final PlaylistService playlistService;
        private final List<Track>     selectedTracks;

        public AddToPlaylistDialog(PlaylistService playlistService,
                                   List<Track> selectedTracks) {
            this.playlistService = playlistService;
            this.selectedTracks  = selectedTracks;
        }

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
                if (playlist == null) return;

                try {
                    AdditionResult result =
                            playlistService.addTracksToPlaylist(playlist, selectedTracks);
                    showResultFeedback(result, playlist.getName());
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

        private VBox buildContent(ListView<Playlist> listView, Label hint) {
            VBox box =
                    new VBox(8, listView, hint);
            box.setPadding(new Insets(8, 0, 0, 0));
            return box;
        }

        private void showInfo(String title, String message) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
        }

        private void showWarning(String title, String message) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
        }

        private void showError(String title, String message) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
        }
    }
}
