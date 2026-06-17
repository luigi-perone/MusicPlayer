package it.unisa.gruppo7.musicplayer.track;

import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.Year;
import java.util.Optional;

/**
 * Manages the communication between the model and the view of the track form.
 * Handles both creating a new track and modifying an existing track record.
 *
 * @author Matteo Postiglione
 */
public class TrackFormController {

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField durationField;
    @FXML private TextField yearField;
    @FXML private TextField genreField;
    @FXML private Label errorLabel;

    private Track trackToModify;

    /**
     * Sets the track to be modified and populates the form fields with its current data.
     *
     * @param track The track model to edit, or null if creating a new track.
     */
    public void setTrack(Track track){
        this.trackToModify=track;
        if(track != null){

            titleField.setText(track.getTitle());
            authorField.setText(track.getAuthor());
            durationField.setText(Integer.toString(track.getDuration()));

            if (track.getPublicationYear() != null) {
                yearField.setText(track.getPublicationYear().toString());
            }
            genreField.setText(track.getGenre());

        }

    }

    /**
     * Handles the action event when the user clicks the cancel button.
     * Closes the form window without saving any changes.
     */
    @FXML
    private void onCancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    /**
     * Handles the action event when the user clicks the save button.
     * Validates form field inputs and executes the track creation or update operation
     * via the application facade.
     */
    @FXML
    private void onSave() {
        errorLabel.setVisible(false);
        Stage stage = (Stage) titleField.getScene().getWindow();

        MusicPlayerFacade musicplayer = MusicPlayerFacade.getInstance();

        try {
            String title = titleField.getText();
            String author = authorField.getText();
            String durationStr = durationField.getText();
            String yearStr = yearField.getText();
            String genre = genreField.getText();

            int duration = 0;
            if (!durationStr.isEmpty()) {
                try {
                    duration = Integer.parseInt(durationStr);
                } catch (NumberFormatException e) {
                    showError("La durata deve essere un numero intero (secondi).");
                    return;
                }
            }

            Year pubYear = null;
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    pubYear = Year.parse(yearStr);
                } catch (Exception e) {
                    showError("L'anno di pubblicazione non è valido.");
                    return;
                }
            }

            boolean success;

            if(trackToModify != null){
                success = musicplayer.modifyTrack(trackToModify, title, author, duration, genre, pubYear);
            } else {
                /*
                Route the library insertion through the command pipeline. The custom
                strategy keeps validation errors inline in the form instead of showing
                the default error pop-up.
                */
                Optional<Boolean> result = CommandInvoker.execute(
                        new AddTrackToLibraryCommand(musicplayer, title, author, duration, genre, pubYear),
                        e -> showError(e.getMessage()));
                if (!result.isPresent()) {
                    return;
                }
                success = result.get();
            }

            if(success){
                stage.close();
            } else {
                showError("Impossibile salvare la traccia.");
            }

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Si è verificato un errore inatteso.");
            e.printStackTrace();
        }
    }

    /**
     * Displays a specific error message string on the form UI container.
     *
     * @param msg The error message text to present.
     */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}