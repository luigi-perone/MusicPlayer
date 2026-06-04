package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.AddToPlaylistDialog;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackFormController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibraryController implements PlaybackObserver {

    @FXML private TableView<Track>           trackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;
    @FXML private TableColumn<Track, String> genreColumn;
    @FXML private TableColumn<Track, Year>    yearColumn;
    @FXML private Button                      addToPlaylistBtn;

    private MusicPlayerFacade     musicPlayer;
    private ObservableList<Track> observableTracks;
    private Track playingTrack = null;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formattedTime =
                    MusicPlayerFacade.getInstance().formatDuration(track.getDuration());
            return new SimpleStringProperty(formattedTime);
        });
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));

        trackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        trackTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        musicPlayer = MusicPlayerFacade.getInstance();

        observableTracks =
                FXCollections.observableArrayList(musicPlayer.getTracksFromLibrary());
        trackTable.setItems(observableTracks);

        addToPlaylistBtn.disableProperty().bind(
                trackTable.getSelectionModel().selectedItemProperty().isNull()
        );

        trackTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        musicPlayer.setSelectedTrack(newSelection);
                    }
                }
        );

        trackTable.setRowFactory(tv -> new TableRow<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.equals(playingTrack)) {
                    setStyle("-fx-background-color: #d4edda; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        trackTable.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<Track>() {
                @Override
                protected void updateItem(Track item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if (item.equals(playingTrack)) {
                        setStyle("-fx-background-color: #d4edda; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    musicPlayer.playTrack(row.getItem());
                }
            });
            return row;
        });

        musicPlayer.getPlaybackService().addObserver(this);
    }

    @Override
    public void onTrackChanged(Track newTrack) {
        this.playingTrack = newTrack;
        Platform.runLater(() -> trackTable.refresh());
    }

    @Override
    public void onStateChanged(PlaybackState newState) {
        if (newState == PlaybackState.STOPPED) {
            this.playingTrack = null;
            Platform.runLater(() -> trackTable.refresh());
        }
    }

    @Override
    public void onTimeTick(int simulatedSeconds) {
    }

    @FXML
    private void onAddToPlaylistClick() {
        List<Track> selected =
                new ArrayList<>(trackTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) return;
        new AddToPlaylistDialog(musicPlayer.getPlaylistService(), selected).show();
    }

    @FXML
    private void onAddTrackClick() {
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(
                            "/it/unisa/gruppo7/musicplayer/TrackFormView.fxml"));
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

    @FXML
    private void onDeleteTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("Nessuna selezione", "Seleziona una traccia dalla tabella per eliminarla.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma eliminazione");
        alert.setHeaderText("Eliminazione traccia");
        alert.setContentText(
                "Sei sicuro di voler eliminare definitivamente '"
                        + selectedTrack.getTitle() + "'?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            musicPlayer.removeTrackFromLibrary(selectedTrack);
            observableTracks.remove(selectedTrack);
        }
    }

    @FXML
    private void onPlayTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        MusicPlayerFacade.getInstance().playTrack(selectedTrack);
    }

    private void mostraAvviso(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}