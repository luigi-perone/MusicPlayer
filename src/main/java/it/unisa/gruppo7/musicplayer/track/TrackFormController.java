package it.unisa.gruppo7.musicplayer.track;


import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.Year;

public class TrackFormController {

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField durationField;
    @FXML private TextField yearField;
    @FXML private TextField genreField;
    @FXML private Label errorLabel;




    @FXML
    private void onCancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onSave() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        MusicPlayerFacade musicplayer = MusicPlayerFacade.getInstance();

        try {
            String title = titleField.getText();
            String author = authorField.getText();
            String durationStr = durationField.getText();
            String yearStr = yearField.getText();
            String genre = genreField.getText();

            int duration = 0;
            if (durationStr != null && !durationStr.isEmpty()) {
                duration = Integer.parseInt(durationStr);
            } else {
                throw new IllegalArgumentException("La durata è obbligatoria.");
            }

            Year pubYear = null;
            if (yearStr != null && !yearStr.isEmpty()) {
                pubYear = Year.parse(yearStr);
            }

            musicplayer.addNewTrackToLibrary(title, author, duration, genre, pubYear);
                
        } catch (NumberFormatException e) {
            mostraErrore("Durata e Anno devono essere numeri validi.");
        } catch (java.time.format.DateTimeParseException e) {
            mostraErrore("Formato anno non valido (Es. 1980).");
        } catch (IllegalArgumentException e) {
            // Qui catturiamo ESATTAMENTE i messaggi di errore che avevi scritto tu nella classe Track!
            mostraErrore(e.getMessage());
        } catch (Exception e) {
            mostraErrore("Errore imprevisto durante il salvataggio.");
            e.printStackTrace();
        }
    }

    private void mostraErrore(String messaggio) {
        errorLabel.setText(messaggio);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}