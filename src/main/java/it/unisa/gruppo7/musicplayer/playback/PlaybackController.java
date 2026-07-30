package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.MainController;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;

import java.time.Year;

/**
 * Controller for the playback user interface.
 * It observes the playback system to dynamically update track details,
 * simulated elapsed time, state transitions, and the progress slider.
 * It provides controls for play/pause, track skipping, and queue visualization toggling.
 *
 * @author Francesco Lemmo
 */
public class PlaybackController implements PlaybackObserver, TrackObserver {
    private static final String SHUFFLE_ACTIVE_CLASS = "player-button-active";
    private static final String LOOP_ACTIVE_CLASS = "player-button-active";

    @FXML private Label timeLabel;
    @FXML private Button playPauseButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button skipPlaylistPrevButton;
    @FXML private Button skipPlaylistNextButton;
    @FXML private Button queueButton;
    @FXML private Label trackAuthorLabel;
    @FXML private Label trackDurationLabel;
    @FXML private Label trackTitleLabel;
    @FXML private Label trackYearLabel;
    @FXML private Button shuffleButton;
    @FXML private Button repeatButton;
    @FXML private Button loopButton;
    @FXML private Slider progressSlider;

    // used to avoid conflict between user and timer
    private boolean isUserSeeking = false;

    private MainController mainController;

    private Track currentTrack;
    private final MusicPlayerFacade musicPlayer;

    /**
     * Creates the controller with the facade injected by the controller factory.
     *
     * @param musicPlayer the shared application facade.
     */
    public PlaybackController(MusicPlayerFacade musicPlayer) {
        this.musicPlayer = musicPlayer;
    }

    /**
     * Initializes the controller and registers this controller instance
     * as a playback observer.
     */
    @FXML
    public void initialize() {
        musicPlayer.addPlaybackObserver(this);
        musicPlayer.addObserver(this);

        updateShuffleButtonState();
        updateRepeatButtonState();
        updatePlaylistSkipButtonsState();

        if (progressSlider != null) {
            // User is moving or clicking on the slider. Stops UI timer
            progressSlider.setOnMousePressed(event -> {
                isUserSeeking = true;
            });

            // Slider released. Unlocks UI timer
            progressSlider.setOnMouseReleased(event -> {
                int seekTime = (int) progressSlider.getValue();
                musicPlayer.seekTo(seekTime);
                isUserSeeking = false;
            });

            // draws the color of the slider progress
            progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                // progress percentage (0 - 100)
                double max = progressSlider.getMax();
                double current = newVal.doubleValue();
                double percentage = (max == 0) ? 0 : (current / max) * 100;

                // seek the UI bar
                javafx.scene.Node track = progressSlider.lookup(".track");

                // if the UI is ready draw the color gradient
                if (track != null) {
                    // Orange (#e0592b) on the left
                    // Grey (#d1d1d6) on the right, split on the calculated percentage
                    String style = String.format(
                            "-fx-background-color: linear-gradient(to right, #e0592b %d%%, #d1d1d6 %d%%);",
                            (int) percentage, (int) percentage
                    );
                    track.setStyle(style);
                }
            });
        }


    }

    /**
     * Handles the action event triggered when the play/pause button is pressed.
     * Determines whether to pause, resume, or start playing the currently selected track.
     *
     */
    @FXML
    void onPlayPause() {
        PlaybackState currentState = musicPlayer.getPlaybackState();

        if (currentState == PlaybackState.PLAYING) {
            musicPlayer.pauseTrack();
        } else if (currentState == PlaybackState.PAUSED) {
            musicPlayer.resumeTrack();
        } else {
            Track trackToPlay = musicPlayer.getSelectedTrack();
            if (trackToPlay != null) {
                musicPlayer.playFromLibraryFrom(trackToPlay);
            }
        }
    }

    /**
     * Handles the action event triggered when the next track button is pressed.
     * Requests the playback service to skip forward to the next item in the execution queue.
     *
     * @param event The action event context.
     */
    @FXML
    void onNextTrack(ActionEvent event) {
        musicPlayer.playNext();
    }

    /**
     * Handles the action event triggered when the previous track button is pressed.
     * Requests the playback service to skip backward to the previous item in the execution queue.
     *
     * @param event The action event context.
     */
    @FXML
    void onPreviousTrack(ActionEvent event) {
        musicPlayer.playPrevious();
    }

    /**
     * Handles the action event triggered when the "next playlist" button is pressed.
     * Requests the playback service to skip to the next playlist block in the queue (US-029).
     *
     * @param event The action event context.
     */
    @FXML
    void onSkipPlaylistNext(ActionEvent event) {
        musicPlayer.skipToNextPlaylist();
    }

    /**
     * Handles the action event triggered when the "previous playlist" button is pressed.
     * Requests the playback service to skip to the previous playlist block in the queue (US-029).
     *
     * @param event The action event context.
     */
    @FXML
    void onSkipPlaylistPrev(ActionEvent event) {
        musicPlayer.skipToPreviousPlaylist();
    }

    /**
     * Handles the action event triggered when the queue visibility button is pressed.
     * Toggles the display state of the execution play queue panel in the user interface.
     *
     * @param event The action event context.
     */
    @FXML
    void onToggleQueue(ActionEvent event) {
        if (mainController != null) {
            // calls the method in the main controller
            mainController.toggleQueueVisibility();
        }
    }

    /**
     * Handles the shuffle button: toggles shuffle mode for the queue, refreshes
     * the queue view and updates the shuffle button's visual state.
     *
     * @param event The action event context.
     */
    @FXML
    void onShuffle(ActionEvent event) {
        boolean newShuffleState = !musicPlayer.isShuffleActive();
        musicPlayer.shuffleQueue(newShuffleState, currentTrack);


        if (mainController != null) {
            mainController.refreshQueueView();
        }

        updateShuffleButtonState();
    }

    /**
     * Handles the repeat button: advances to the next repeat mode and updates
     * the repeat button's visual state.
     *
     * @param event The action event context.
     */
    @FXML
    void onRepeat(ActionEvent event) {
        musicPlayer.changeRepeatMode();
        updateRepeatButtonState();
    }


    /**
     * Updates the shuffle button's style class and tooltip to reflect whether
     * shuffle mode is currently active.
     */
    private void updateShuffleButtonState() {
        boolean isActive = musicPlayer.isShuffleActive();

        shuffleButton.getStyleClass().remove(SHUFFLE_ACTIVE_CLASS);
        shuffleButton.getStyleClass().remove("active");

        if (isActive) {
            shuffleButton.getStyleClass().add(SHUFFLE_ACTIVE_CLASS);
        }

        shuffleButton.setTooltip(new Tooltip(isActive ? "Casuale: attivo" : "Casuale: disattivo"));
    }

    /**
     * Updates the repeat button's text, style class and tooltip to reflect the
     * current {@link RepeatMode}.
     */
    private void updateRepeatButtonState() {
        RepeatMode repeatMode = musicPlayer.getCurrentRepeatMode();

        repeatButton.getStyleClass().remove(LOOP_ACTIVE_CLASS);
        repeatButton.getStyleClass().remove("active");

        String tooltipText;
        if (repeatMode == RepeatMode.REPEAT_PLAYLIST) {
            setIcon(repeatButton, "icon-repeat", true);
            repeatButton.getStyleClass().add(LOOP_ACTIVE_CLASS);
            tooltipText = "Ripeti: playlist";
        } else if (repeatMode == RepeatMode.REPEAT_ONE) {
            setIcon(repeatButton, "icon-repeat-one", true);
            repeatButton.getStyleClass().add(LOOP_ACTIVE_CLASS);
            tooltipText = "Ripeti: un brano";
        } else {
            setIcon(repeatButton, "icon-repeat");
            tooltipText = "Ripeti: off";
        }

        repeatButton.setTooltip(new Tooltip(tooltipText));
    }

    /**
     * Enables/disables the playlist-skip buttons based on whether a previous/next
     * playlist block exists in the queue (US-029). Safe to call off the FX thread
     * only via {@link Platform#runLater}; callers on the FX thread may call directly.
     */
    private void updatePlaylistSkipButtonsState() {
        if (skipPlaylistPrevButton != null) {
            skipPlaylistPrevButton.setDisable(!musicPlayer.hasPreviousPlaylist());
        }
        if (skipPlaylistNextButton != null) {
            skipPlaylistNextButton.setDisable(!musicPlayer.hasNextPlaylist());
        }
    }

    /**
     * Reacts to a blocked playlist-skip command (queue limit reached or shuffle active)
     * by refreshing the skip buttons' enabled state. Playback is left untouched.
     */
    @Override
    public void onPlaylistSkipBlocked() {
        Platform.runLater(this::updatePlaylistSkipButtonsState);
    }

    /**
     * Updates the time counter label and recalculates the progress slider position
     * at periodic simulated time increments.
     *
     * @param simulatedSeconds The elapsed playback time in seconds.
     */
    @Override
    public void onTimeTick(int simulatedSeconds) {
        Platform.runLater(() -> {
            if (currentTrack != null && !isUserSeeking) {
                timeLabel.setText(musicPlayer.formatDuration(simulatedSeconds));
                progressSlider.setValue(simulatedSeconds);
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
            updatePlaylistSkipButtonsState();
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

                // set the slider for the new track
                progressSlider.setMax(newTrack.getDuration());
                progressSlider.setValue(0);


            } else {
                resetTrackInfo();
            }
        });
    }

    /**
     * Resets all track-info widgets to their idle defaults. Must be called on the FX thread.
     */
    private void resetTrackInfo() {
        trackTitleLabel.setText("Nessun brano");
        trackAuthorLabel.setText("Autore");
        timeLabel.setText("00:00");
        trackYearLabel.setText("Anno");
        trackDurationLabel.setText(musicPlayer.formatDuration(0));

        // reset the slider
        progressSlider.setMax(100);
        progressSlider.setValue(0);
    }

    /**
     * Swaps the icon shown by a button, keeping the shared {@code icon} style class and
     * replacing the shape-specific one.
     *
     * @param button    The button whose graphic must change.
     * @param iconClass The style class carrying the wanted {@code -fx-shape}.
     */
    private void setIcon(Button button, String iconClass) {
        setIcon(button, iconClass, false);
    }

    /**
     * Swaps the icon shown by a button, keeping the shared {@code icon} style class and
     * replacing the shape-specific one.
     *
     * @param button    The button whose graphic must change.
     * @param iconClass The style class carrying the wanted {@code -fx-shape}.
     * @param active    Whether the icon must take the accent colour of an active control.
     *                  The class is applied to the graphic itself because a colour coming
     *                  from a rule on the button is not reapplied when these classes change.
     */
    private void setIcon(Button button, String iconClass, boolean active) {
        Node icon = button.getGraphic();
        if (icon == null) {
            return;
        }
        if (active) {
            icon.getStyleClass().setAll("icon", iconClass, "icon-active");
        } else {
            icon.getStyleClass().setAll("icon", iconClass);
        }
    }

    /**
     * Toggles the displayed graphical symbol representation of the execution action button
     * based on engine state machine updates.
     *
     * @param newState The incoming system operational playback state.
     */
    @Override
    public void onStateChanged(PlaybackState newState) {
        Platform.runLater(() -> {
            if (newState == PlaybackState.PLAYING) {
                setIcon(playPauseButton, "icon-pause");
            } else if (newState == PlaybackState.PAUSED || newState == PlaybackState.STOPPED) {
                setIcon(playPauseButton, "icon-play");
            }
        });
    }


    /**
     * Refreshes the playlist-skip buttons when the queue contents change
     * (e.g. a playlist was appended), so their enabled state stays accurate.
     */
    @Override
    public void onQueueChanged() {
        Platform.runLater(this::updatePlaylistSkipButtonsState);
    }

    /**
     * Sets the main controller used to toggle and refresh the queue view.
     *
     * @param mainController the application's main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Reacts to a track being deleted from the library by blanking the track-info
     * panel only when nothing is playing any more.
     *
     * @param track the track that was deleted
     */
    @Override
    public void onTrackDeleted(Track track) {
        Platform.runLater(() -> {
            // Only blank the info panel when nothing is playing any more.
            // If the engine advanced to another track, onTrackChanged already
            // refreshed the labels, so we must not clobber them here.
            if (musicPlayer.getCurrentPlayingTrack() == null) {
                resetTrackInfo();
            }
        });
    }

    /**
     * Reacts to a track being edited by refreshing the displayed metadata when
     * the edited track is the one currently shown.
     *
     * @param track the track that was edited
     */
    @Override
    public void onTrackEdit(Track track) {
        Platform.runLater(() -> {
            if (track.equals(currentTrack)) {
                trackTitleLabel.setText(track.getTitle());
                trackAuthorLabel.setText(track.getAuthor());
                Year trackYear = track.getPublicationYear();
                if (trackYear == null) {
                    trackYearLabel.setText("");
                } else {
                    trackYearLabel.setText(trackYear.toString());
                }
            }
        });
    }
}
