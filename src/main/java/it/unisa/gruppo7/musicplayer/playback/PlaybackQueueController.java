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
 * @author francescoLemmo
 */
public class PlaybackQueueController implements PlaybackObserver, TrackObserver {

    @FXML private ListView<Track> queueListView;

    private MusicPlayerFacade musicPlayer;
    private Track currentTrack;

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
     * updates the queue
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

    @Override
    public void onTrackChanged(Track newTrack) {
        this.currentTrack = newTrack;
        refreshQueue();
    }


    @Override public void onTimeTick(int simulatedSeconds) {}
    @Override public void onStateChanged(PlaybackState newState) {}

    @Override
    public void onTrackDeleted(Track track) {
        refreshQueue();
    }

    @Override
    public void onTrackEdit(Track track) {
        refreshQueue();
    }
}
