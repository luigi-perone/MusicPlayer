package it.unisa.gruppo7.musicplayer.library;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackFormController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Year;
import java.util.Optional;

/**
 * Manages the communication between the model and the view of form to add, edit and eliminated the tracks.
 * 
 * @author Matteo Postiglione
 */

public class LibraryController {

    @FXML
    private TableView<Track> trackTable;
    @FXML
    private TableColumn<Track, String> titleColumn;
    @FXML
    private TableColumn<Track, String> authorColumn;
    @FXML
    private TableColumn<Track, String> durationColumn;
    @FXML
    private TableColumn<Track, String> genreColumn;
    @FXML
    private TableColumn<Track, Year> yearColumn;

    private MusicPlayerFacade musicPlayer;

    private ObservableList<Track> observableTracks;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formattedTime = MusicPlayerFacade.getInstance().formatDuration(track.getDuration());
            return new SimpleStringProperty(formattedTime);
        });
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));

        trackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        musicPlayer = MusicPlayerFacade.getInstance();

        observableTracks = FXCollections.observableArrayList(musicPlayer.getTracksFromLibrary());
        trackTable.setItems(observableTracks);
    }

    @FXML
    private void onAddTrackClick() {
        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/it/unisa/gruppo7/musicplayer/TrackFormView.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Add new track");
            stage.setScene(new javafx.scene.Scene(root));
            
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            stage.showAndWait();

            observableTracks.setAll(musicPlayer.getTracksFromLibrary());

        } catch (Exception e) {
            e.printStackTrace();
            mostraAvviso("Error", "Unable to load the form.");
        }
    }

    @FXML
    private void onEditTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("No selection", "Select a table track to edit it.");
            return;
        }

        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/it/unisa/gruppo7/musicplayer/TrackFormView.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            TrackFormController controller = fxmlLoader.getController();
            controller.setTrack(selectedTrack);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit track");
            stage.setScene(new javafx.scene.Scene(root));
            
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            stage.showAndWait();

            observableTracks.setAll(musicPlayer.getTracksFromLibrary());
            
        } catch (Exception e) {
            e.printStackTrace();
            mostraAvviso("Error", "Unable to load the form.");
        }    

        System.out.println("Open form to modify the track: " + selectedTrack.getTitle());
    }

    @FXML
    private void onDeleteTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("No selection", "Select a table track to delete it..");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm elimination");
        alert.setHeaderText("Elimination track");
        alert.setContentText("Are you sure you want to eliminate permanently '" + selectedTrack.getTitle() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            musicPlayer.removeTrackFromLibrary(selectedTrack);

            observableTracks.remove(selectedTrack);

            System.out.println("Track eliminated!");
        }
    }

    private void mostraAvviso(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}