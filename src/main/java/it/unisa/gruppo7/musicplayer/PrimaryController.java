package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.library.Library;
import it.unisa.gruppo7.musicplayer.track.Track;
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

public class PrimaryController {

    @FXML
    private TableView<Track> trackTable;
    @FXML
    private TableColumn<Track, String> titleColumn;
    @FXML
    private TableColumn<Track, String> authorColumn;
    @FXML
    private TableColumn<Track, Integer> durationColumn;
    @FXML
    private TableColumn<Track, String> genreColumn;
    @FXML
    private TableColumn<Track, Year> yearColumn;

    private Library myLibrary;
    private ObservableList<Track> observableTracks;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));

        trackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        myLibrary = Library.getInstance();
        observableTracks = FXCollections.observableArrayList(myLibrary.getTracks());
        trackTable.setItems(observableTracks);
    }

    @FXML
    private void onAddTrackClick() {
try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("trackform.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Aggiungi Nuova Traccia");
            stage.setScene(new javafx.scene.Scene(root));
            
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            stage.showAndWait();

            observableTracks.setAll(myLibrary.getTracks());
            
        } catch (Exception e) {
            e.printStackTrace();
            mostraAvviso("Errore", "Impossibile caricare il form.");
        }
    }

    @FXML
    private void onEditTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("Nessuna selezione", "Seleziona una traccia dalla tabella per modificarla.");
            return;
        }
        System.out.println("Apro il form per MODIFICARE la traccia: " + selectedTrack.getTitle());
    }

    @FXML
    private void onDeleteTrackClick() {
        Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            mostraAvviso("Nessuna selezione", "Seleziona una traccia dalla tabella per eliminarla.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma Eliminazione");
        alert.setHeaderText("Eliminazione traccia");
        alert.setContentText("Sei sicuro di voler eliminare definitivamente '" + selectedTrack.getTitle() + "'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            myLibrary.removeTrack(selectedTrack);
            myLibrary.save(); // Salva nel JSON
            observableTracks.remove(selectedTrack); // Aggiorna l'interfaccia
            System.out.println("Traccia eliminata!");
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