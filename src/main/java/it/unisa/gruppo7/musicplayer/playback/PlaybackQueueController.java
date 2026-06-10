package it.unisa.gruppo7.musicplayer.playback;

import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.ArrayList;

/**
 * JavaFX controller responsible for managing and displaying the active playback queue.
 * It observes both playback state changes and library track modifications to keep
 * the visual list synchronized with the underlying audio engine.
 *
 * @author francescoLemmo
 */
public class PlaybackQueueController implements PlaybackObserver, TrackObserver {

    @FXML private ListView<Track> queueListView;

    private MusicPlayerFacade musicPlayer;
    private Track currentTrack;

    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded. It sets up the facade instance,
     * registers the necessary observers, configures the custom cell rendering
     * for the ListView, and binds mouse events for track selection.
     */
    @FXML
    public void initialize() {
        musicPlayer = MusicPlayerFacade.getInstance();
        musicPlayer.getPlaybackService().addObserver(this);
        musicPlayer.addObserver(this);


        queueListView.setCellFactory(param -> new ListCell<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " - " + item.getAuthor());
                }
            }
        });

        // handles double click
        queueListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Track selectedTrack = queueListView.getSelectionModel().getSelectedItem();
                if (selectedTrack != null) {
                    musicPlayer.playFromQueue(selectedTrack);
                }
            }
        });

        refreshQueue();
    }

    /**
     * Updates the visual representation of the queue on the JavaFX Application Thread.
     * If the player is in the start-up state with no current track, it displays the entire queue.
     * Otherwise, it displays only the tracks scheduled to play after the current track.
     */
    public void refreshQueue() {
        Platform.runLater(() -> {
            if (currentTrack == null && musicPlayer.getPlaybackState() == PlaybackState.START_UP) {
                queueListView.getItems().setAll(musicPlayer.getPlaybackService().getQueue().getTracks());
            } else {
                queueListView.getItems().setAll(musicPlayer.getUpNextQueueFrom(currentTrack));
            }
        });
    }

    /**
     * Triggered when the playback engine transitions to a new track.
     * Updates the local track reference and refreshes the upcoming queue list.
     *
     * @param newTrack The newly loaded track.
     */
    @Override
    public void onTrackChanged(Track newTrack) {
        this.currentTrack = newTrack;
        refreshQueue();
    }

    /**
     * Triggered periodically during playback to indicate progress.
     * Ignored in this controller as it does not handle progress bars.
     *
     * @param simulatedSeconds The current playback time elapsed in seconds.
     */
    @Override
    public void onTimeTick(int simulatedSeconds) {}

    /**
     * Triggered when the playback state transitions (e.g., PLAYING, PAUSED).
     * Ignored in this controller as it only manages the queue order.
     *
     * @param newState The new state of the playback engine.
     */
    @Override
    public void onStateChanged(PlaybackState newState) {}

    /**
     * Triggered when a track is permanently removed from the application.
     * Refreshes the visual list to ensure deleted tracks are no longer displayed.
     *
     * @param track The track that was deleted.
     */
    @Override
    public void onTrackDeleted(Track track) {
        refreshQueue();
    }

    /**
     * Triggered when a track's metadata (e.g., title, author) is modified.
     * Refreshes the queue to ensure the ListView displays the updated information.
     *
     * @param track The track that was modified.
     */
    @Override
    public void onTrackEdit(Track track) {
        refreshQueue();
    }

    /**
     * Triggered when the structural order of the playback queue changes
     * (e.g., shuffle toggled, tracks appended).
     * Refreshes the ListView to reflect the new canonical or shuffled order.
     */
    @Override
    public void onQueueChanged() {
        refreshQueue();
    }
}