package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PlaylistDetailController {

    @FXML private Label playlistNameLabel;
        @FXML private TableView<Track> playlistTrackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;

    private Runnable onBackAction;

    @FXML
    public void initialize() {

    }

    public void setPlaylistName(String name) {
        playlistNameLabel.setText(name);
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void onBackClick() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}