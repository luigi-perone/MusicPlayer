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
 * Controller for the playback user interface bar.
 * It observes the playback system to dynamically update track details,
 * simulated elapsed time, state transitions, and the progress bar.
 *
 * @author Francesco Lemmo
 */
public class PlaybackController implements PlaybackObserver {

    @FXML private Label timeLabel;
    @FXML private Button playPauseButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label trackAuthorLabel;
    @FXML private Label trackDurationLabel;
    @FXML private Label trackTitleLabel;
    @FXML private Label trackYearLabel;

    private Track currentTrack;
    private MusicPlayerFacade musicPlayer;

    /**
     * Initializes the controller. Resolves the main system facade
     * and registers this controller instance as a playback observer.
     */
    @FXML
    public void initialize() {
        musicPlayer = MusicPlayerFacade.getInstance();
        musicPlayer.getPlaybackService().addObserver(this);
    }

    /**
     * Handles the action event triggered when the play/pause button is pressed.
     * Determines whether to pause, resume, or start playing the currently selected track.
     *
     * @param event The action event context.
     */
    @FXML
    void onPlayPause(ActionEvent event) {
        PlaybackState currentState = musicPlayer.getPlaybackState();

        if (currentState == PlaybackState.PLAYING) {
            musicPlayer.pauseTrack();
        } else if (currentState == PlaybackState.PAUSED) {
            musicPlayer.resumeTrack();
        } else {
            Track trackToPlay = musicPlayer.getSelectedTrack();
            if (trackToPlay != null) {
                musicPlayer.playTrack(trackToPlay);
            }
        }
    }

    /**
     * Updates the time counter label and recalculates the progress bar percentage ratio
     * at periodic simulated time increments.
     *
     * @param simulatedSeconds The elapsed playback time in seconds.
     */
    @Override
    public void onTimeTick(int simulatedSeconds) {
        Platform.runLater(() -> {
            timeLabel.setText(musicPlayer.formatDuration(simulatedSeconds));

            if (currentTrack != null && currentTrack.getDuration() > 0) {
                double progress = (double) simulatedSeconds / currentTrack.getDuration();
                progressBar.setProgress(progress);
            }
        });
    }

    /**
     * Updates the text labels showing track metadata whenever the active track changes.
     * Resets indicators to defaults if the structural reference is null.
     *
     * @param newTrack The updated track metadata model, or null if stopped.
     */
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

    /**
     * Toggles the displayed string label text of the execution action button
     * based on engine state machine updates.
     *
     * @param newState The incoming system operational playback state.
     */
    @Override
    public void onStateChanged(PlaybackState newState) {
        Platform.runLater(() -> {
            if (newState == PlaybackState.PLAYING) {
                playPauseButton.setText("Pausa");
            } else if (newState == PlaybackState.PAUSED || newState == PlaybackState.STOPPED) {
                playPauseButton.setText("Riproduci");
            }
        });
    }
}