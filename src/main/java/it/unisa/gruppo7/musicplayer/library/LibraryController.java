package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.MainController;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.dialog.AddToPlaylistDialogBuilder;
import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;
import it.unisa.gruppo7.musicplayer.dialog.DialogTag;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.AddToQueueCommand;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.track.RemoveTrackFromLibraryCommand;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import it.unisa.gruppo7.musicplayer.track.TrackFormController;
import it.unisa.gruppo7.musicplayer.undo.UndoToast;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the primary music library view interface.
 * It manages the track table overview, multi-selection operations, active track row highlighting,
 * and handles main interface button actions such as track creation, modifications, deletions, and playlist mapping.
 *
 */
public class LibraryController implements PlaybackObserver, TrackObserver {

    @FXML private TableView<Track>           trackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;
    @FXML private TableColumn<Track, String> genreColumn;
    @FXML private TableColumn<Track, Year>    yearColumn;
    @FXML private Button                      addToPlaylistBtn;
    @FXML private TableColumn<Track, String> tagColumn;
    @FXML private Button                      manageTagsBtn;

    private final MusicPlayerFacade musicPlayer;
    private ObservableList<Track> observableTracks;
    private Track playingTrack = null;

    private MainController mainController;

    /**
     * Creates the controller with the facade injected by the controller factory.
     *
     * @param musicPlayer the shared application facade.
     */
    public LibraryController(MusicPlayerFacade musicPlayer) {
        this.musicPlayer = musicPlayer;
    }

    /**
     * Initializes the controller class. Configures table cell value factories,
     * selection tracking properties, visual background style row factories, and registers
     * this controller instance into the system playback observer pipeline.
     */
    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formattedTime =
                    musicPlayer.formatDuration(track.getDuration());
            return new SimpleStringProperty(formattedTime);
        });
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        tagColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatTags(cellData.getValue())));

        trackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        trackTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        observableTracks =
                FXCollections.observableArrayList(musicPlayer.getTracksFromLibrary());
        trackTable.setItems(observableTracks);

        addToPlaylistBtn.disableProperty().bind(
                trackTable.getSelectionModel().selectedItemProperty().isNull()
        );
        manageTagsBtn.disableProperty().bind(trackTable.getSelectionModel().selectedItemProperty().isNull());

        trackTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        musicPlayer.setSelectedTrack(newSelection);
                    }
                }
        );

        ContextMenu contextMenu = new ContextMenu();
        MenuItem addToQueueItem = new MenuItem("Aggiungi a coda");

        addToQueueItem.setOnAction(event -> {
            Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
            if (selectedTrack != null) {
                CommandInvoker.execute(new AddToQueueCommand(
                        musicPlayer.getPlaybackService(),
                        () -> musicPlayer.appendTrackToQueue(selectedTrack)));
                mainController.refreshQueueView();
                PlaybackState playbackState = musicPlayer.getPlaybackState();
                if (playbackState == PlaybackState.STOPPED || playbackState == PlaybackState.START_UP){
                    musicPlayer.playFromQueue(selectedTrack);
                }
            }
        });

        contextMenu.getItems().add(addToQueueItem);

        trackTable.setContextMenu(contextMenu);

        trackTable.setOnContextMenuRequested(event -> {
            if (trackTable.getSelectionModel().getSelectedItem() == null) {
                contextMenu.hide();
            }
        });

        trackTable.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<Track>() {
                @Override
                protected void updateItem(Track item, boolean empty) {
                    super.updateItem(item, empty);

                    getStyleClass().remove("playing-row");

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    if (item.equals(playingTrack)) {
                        getStyleClass().add("playing-row");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    musicPlayer.playFromLibraryFrom(row.getItem());
                }
            });
            return row;
        });

        musicPlayer.addPlaybackObserver(this);
        musicPlayer.addObserver(this);
    }

    /**
     * Invoked when the currently playing track changes. Updates the internal reference pointer
     * and triggers a safe background UI thread layout refresh.
     *
     * @param newTrack The newly playing audio track, or null if stopped.
     */
    @Override
    public void onTrackChanged(Track newTrack) {
        Platform.runLater(() -> {
            this.playingTrack = newTrack;
            trackTable.getSelectionModel().clearSelection();
            trackTable.refresh();
        });
    }

    /**
     * Invoked when the system playback operational running state machine shifts.
     * Clears highlighters if the audio engine stops.
     *
     * @param newState The incoming core state metric.
     */
    @Override
    public void onStateChanged(PlaybackState newState) {
        if (newState == PlaybackState.STOPPED) {
            Platform.runLater(() -> {
                this.playingTrack = null;
                trackTable.refresh();
            });
        }
    }

    /**
     * Invoked periodically on ongoing system time tick progress updates.
     *
     * @param simulatedSeconds The elapsed track duration counter position in seconds.
     */
    @Override
    public void onTimeTick(int simulatedSeconds) {
    }

    /**
     * Extracts selected row items and constructs an inline popup dialog
     * via AddToPlaylistDialogBuilder to reassign items onto target playlist nodes.
     */
    @FXML
    private void onAddToPlaylistClick() {
        List<Track> selected = new ArrayList<>(trackTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;

        List<Playlist> playlists = musicPlayer.getPlaylists();
        if (playlists.isEmpty()) {
            mostraAvviso("Nessuna playlist disponibile", "Non ci sono ancora playlist. Creane prima una dalla barra laterale.");
            return;
        }

        List<String> playlistNames = playlists.stream()
                .map(Playlist::getName)
                .collect(Collectors.toList());

        AddToPlaylistDialogBuilder builder = new AddToPlaylistDialogBuilder(playlistNames, selected);
        builder.buildHeader();
        builder.buildButtons();
        builder.buildContent();
        builder.buildResultConverter();

        Dialog<String> dialog = builder.getResult();
        dialog.showAndWait().ifPresent(targetPlaylistName -> {
            Playlist targetPlaylist = musicPlayer.getPlaylist(targetPlaylistName);
            if (targetPlaylist != null) {
                try {
                    AdditionResult result = musicPlayer.getPlaylistService()
                            .addTracksToPlaylist(targetPlaylist, selected);
                    musicPlayer.savePlaylists();
                    showResultFeedback(result, targetPlaylistName);
                } catch (IllegalArgumentException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Operazione fallita");
                    alert.setHeaderText(null);
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
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
        Alert alert;
        if (result.hasAdded() && !result.hasSkipped()) {
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Completato");
            alert.setContentText(result.getAdded() + " traccia/e aggiunta/e a \"" + playlistName + "\".");
        } else if (result.hasAdded() && result.hasSkipped()) {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aggiunta parziale");
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            alert.setContentText(result.getAdded() + " traccia/e aggiunta/e a \"" + playlistName + "\".\n\n"
                    + "Le seguenti tracce erano già presenti e sono state ignorate:\n  • " + skippedList);
        } else {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Già nella playlist");
            String skippedList = String.join("\n  • ", result.getSkippedTitles());
            alert.setContentText("Tutte le tracce selezionate sono già in \"" + playlistName + "\":\n  • " + skippedList);
        }
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Opens a standalone modal scene frame holding input fields to register a new track entry
     * into global tracking files.
     */
    @FXML
    private void onAddTrackClick() {
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(
                            "/it/unisa/gruppo7/musicplayer/TrackFormView.fxml"));
            fxmlLoader.setControllerFactory(new it.unisa.gruppo7.musicplayer.ControllerFactory(musicPlayer));
            javafx.scene.Parent root = fxmlLoader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Aggiungi nuova traccia");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            observableTracks.setAll(musicPlayer.getTracksFromLibrary());

        } catch (Exception e) {
            e.printStackTrace();
            mostraAvviso("Errore", "Impossibile caricare il modulo.");
        }
    }

    /**
     * Launches the track operational entry editor frame panel, preloading structural properties
     * bound onto the currently selected layout entry row.
     */
    @FXML
    private void onEditTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("Nessuna selezione", "Seleziona una traccia dalla tabella per modificarla.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(
                            "/it/unisa/gruppo7/musicplayer/TrackFormView.fxml"));
            fxmlLoader.setControllerFactory(new it.unisa.gruppo7.musicplayer.ControllerFactory(musicPlayer));
            javafx.scene.Parent root = fxmlLoader.load();

            TrackFormController controller = fxmlLoader.getController();
            controller.setTrack(selectedTrack);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Modifica traccia");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            observableTracks.setAll(musicPlayer.getTracksFromLibrary());

        } catch (Exception e) {
            e.printStackTrace();
            mostraAvviso("Errore", "Impossibile caricare il modulo.");
        }
    }

    /**
     * Drops the highlighted selected track(s) entirely out of core persistence registries
     * after validating confirmation popup requests. Supports multi-selection.
     */
    @FXML
    private void onDeleteTrackClick() {
        List<Track> selectedTracks =
                new ArrayList<>(trackTable.getSelectionModel().getSelectedItems());
        if (selectedTracks.isEmpty()) {
            mostraAvviso("Nessuna selezione", "Seleziona una traccia dalla tabella per eliminarla.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma eliminazione");
        alert.setHeaderText("Eliminazione traccia");
        String contentText = selectedTracks.size() == 1
                ? "Sei sicuro di voler eliminare definitivamente '"
                        + selectedTracks.get(0).getTitle() + "'?"
                : "Sei sicuro di voler eliminare definitivamente "
                        + selectedTracks.size() + " tracce?";
        alert.setContentText(contentText);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int undoableCount = 0;
            for (Track track : selectedTracks) {
                if (CommandInvoker.execute(new RemoveTrackFromLibraryCommand(musicPlayer, track)).isPresent()) {
                    undoableCount++;
                }
            }
            observableTracks.removeAll(selectedTracks);

            if (undoableCount > 0) {
                String undoMessage = selectedTracks.size() == 1
                        ? "Traccia eliminata dalla libreria"
                        : selectedTracks.size() + " tracce eliminate dalla libreria";
                showUndoToast(undoMessage, undoableCount);
            }
        }
    }

    /**
     * Auxiliary internal framework utility to prompt basic instructional popups to users.
     *
     * @param titolo    The message alert title header context.
     * @param messaggio The details info description text.
     */
    private void mostraAvviso(String titolo, String messaggio) {
        DialogUtils.showInfo(titolo, messaggio);
    }

    @FXML
    private void onManageTagsClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack != null) {
            showTagDialog(selectedTrack);
        }
    }

    /**
     * Opens the tag management dialog for a selected track.
     *
     * @param track The track whose tags should be edited.
     */
    private void showTagDialog(Track track) {
        DialogTag.show(track).ifPresent(selectedTags -> {
            musicPlayer.updateTrackTags(track, selectedTags);
            trackTable.refresh();
        });
    }

    /**
     * Formats assigned tags for the table column using short labels.
     *
     * @param track The track to format.
     * @return A comma-separated short label list.
     */
    private String formatTags(Track track) {
        if (track == null || track.getTags().isEmpty()) {
            return "";
        }

        List<String> labels = new ArrayList<>();
        for (TrackTag tag : TrackTag.values()) {
            if (track.hasTag(tag)) {
                labels.add(tag.getShortLabel());
            }
        }
        return String.join(", ", labels);
    }

    /**
     * Sets the main controller used to refresh the other views.
     *
     * @param mainController the application's main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Shows the transient undo toast over the current window. Pressing "Annulla"
     * reverts the last {@code steps} undoable actions (a multi-delete produces
     * several) and refreshes the library table.
     *
     * @param message the message to display.
     * @param steps   how many undoable actions the gesture produced.
     */
    private void showUndoToast(String message, int steps) {
        if (trackTable.getScene() == null) return;
        UndoToast.show(trackTable.getScene().getWindow(), message, () -> {
            for (int i = 0; i < steps; i++) {
                musicPlayer.undoLastAction();
            }
            if (mainController != null) {
                mainController.refreshAllViews();
            } else {
                observableTracks.setAll(musicPlayer.getTracksFromLibrary());
                trackTable.refresh();
            }
        });
    }

    /**
     * Reloads the library table from the current library contents.
     */
    public void reload() {
        observableTracks.setAll(musicPlayer.getTracksFromLibrary());
        trackTable.refresh();
    }

    @Override
    public void onTrackDeleted(Track track) {
        //throw new UnsupportedOperationException("Unimplemented method 'onTrackDeleted'");
    }

    @Override
    public void onTrackEdit(Track track) {
        Platform.runLater(() -> trackTable.refresh());
    }
}
