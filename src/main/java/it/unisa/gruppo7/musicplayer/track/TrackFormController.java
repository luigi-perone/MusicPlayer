package it.unisa.gruppo7.musicplayer.track;


import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.Year;
/**
 * Manages the communication between the model and the view of the library.
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

    @FXML
    private void onCancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

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
                    mostraErrore("La durata deve essere un numero intero (secondi).");
                    return;
                }
            }

            Year pubYear = null;
            if (yearStr != null && !yearStr.isEmpty()) {
                try {
                    pubYear = Year.parse(yearStr);
                } catch (Exception e) {
                    mostraErrore("L'anno di pubblicazione non è valido.");
                    return;
                }
            }

            boolean success;

            if(trackToModify != null){
                success = musicplayer.modifyTrack(trackToModify, title, author, duration, genre, pubYear);
            } else {
                success = musicplayer.addNewTrackToLibrary(title, author, duration, genre, pubYear);
            }

            if(success){
                stage.close();
            } else {
                mostraErrore("Impossibile salvare la traccia.");
            }

        } catch (IllegalArgumentException e) {
            mostraErrore(e.getMessage());
        } catch (Exception e) {
            mostraErrore("Si è verificato un errore inatteso.");
            e.printStackTrace();
        }
    }

    private void mostraErrore(String messaggio) {
        errorLabel.setText(messaggio);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}