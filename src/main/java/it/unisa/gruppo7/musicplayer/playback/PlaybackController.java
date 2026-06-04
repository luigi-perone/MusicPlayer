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

    @FXML
    public void initialize() {
        musicPlayer = MusicPlayerFacade.getInstance();
        musicPlayer.getPlaybackService().addObserver(this);
    }

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
        Platform.runLater(() -> {
            if (newState == PlaybackState.PLAYING) {
                playPauseButton.setText("Pausa");
            } else if (newState == PlaybackState.PAUSED || newState == PlaybackState.STOPPED) {
                playPauseButton.setText("Riproduci");
            }
        });
    }
}