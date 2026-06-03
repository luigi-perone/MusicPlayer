package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.time.Year;


/**
 * @author francescoLemmo
 */
public class PlaybackController implements PlaybackObserver {

    @FXML private Label timeLabel;
    @FXML  private Button playPauseButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label trackAuthorLabel;
    @FXML private Label trackDurationLabel;
    @FXML private Label trackTitleLabel;
    @FXML private Label trackYearLabel;

    private Track currentTrack;
    private MusicPlayerFacade musicPlayer;

    @FXML
    public void initialize() {
        // Controller subscribe to the subject PlaybackService
        musicPlayer = MusicPlayerFacade.getInstance();
        musicPlayer.getPlaybackService().addObserver(this);
    }

    @FXML
    void onPlayPause(ActionEvent event) {

        if (musicPlayer.getPlaybackState() == PlaybackState.PLAYING) {
            musicPlayer.pauseTrack();
        }
        else if (musicPlayer.getPlaybackState() == PlaybackState.PAUSED) {
            musicPlayer.resumeTrack();
        }
    }



    // --- IMPLEMENTAZIONE DELL'OBSERVER ---

    @Override
    public void onTimeTick(int simulatedSeconds) {
        // REGOLA D'ORO: Siamo su un thread in background!
        // Dobbiamo passare il lavoro alla UI con Platform.runLater
        Platform.runLater(() -> {
            // Aggiorna l'etichetta di testo
            timeLabel.setText(musicPlayer.formatDuration(simulatedSeconds));

            // Calcola e aggiorna la ProgressBar
            if (currentTrack != null && currentTrack.getDuration() > 0) {
                double progress = (double) simulatedSeconds / currentTrack.getDuration();
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void onTrackChanged(Track newTrack) {
        this.currentTrack = newTrack;

        Platform.runLater(() -> {
            if (newTrack != null) {
                trackTitleLabel.setText(newTrack.getTitle());
                trackAuthorLabel.setText(newTrack.getAuthor());
                Year trackYear = newTrack.getPublicationYear();
                if (trackYear == null) {
                    trackYearLabel.setText("");
                } else {
                    trackYearLabel.setText(trackYear.toString());
                }

                trackDurationLabel.setText(musicPlayer.formatDuration(newTrack.getDuration()));
            } else {
                trackTitleLabel.setText("Nessun brano");
                progressBar.setProgress(0.0);
                timeLabel.setText("00:00");
            }
        });
    }

    @Override
    public void onStateChanged(PlaybackState newState) {
        // Qui potresti cambiare il testo del bottone Play/Pausa, sempre con Platform.runLater
    }

}
