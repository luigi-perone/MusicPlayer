package it.unisa.gruppo7.musicplayer.homepage;

import it.unisa.gruppo7.musicplayer.MainController;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

/**
 * @author francescoLemmo
 */

public class HomePageController {

    @FXML private HBox topPlaylistsContainer;
    @FXML private HBox topTracksContainer;

    @FXML private VBox playlistsSection;
    @FXML private VBox tracksSection;

    private MainController mainController;

    private MusicPlayerFacade musicPlayer = MusicPlayerFacade.getInstance();

    @FXML
    public void initialize() {
        loadTopPlaylists();
        loadTopTracks();
    }

    public void refreshHomePage() {
        topPlaylistsContainer.getChildren().clear();
        topTracksContainer.getChildren().clear();


        playlistsSection.setVisible(true);
        playlistsSection.setManaged(true);

        tracksSection.setVisible(true);
        tracksSection.setManaged(true);

        loadTopPlaylists();
        loadTopTracks();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void loadTopPlaylists() {

        List<Playlist> topPlaylists = musicPlayer.getMostPlayedPlaylists(10);

        if (topPlaylists.isEmpty()) {

            playlistsSection.setVisible(false);
            playlistsSection.setManaged(false);
            return;
        }
        for (Playlist playlist : topPlaylists) {
            try {
                // loads the FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("MediaCardView.fxml"));
                VBox card = loader.load();

                // gives the data to the card
                MediaCardController cardController = loader.getController();
                cardController.setData(playlist.getName(), playlist.getPlayCount() + " ascolti");

                // Click on the card to play
                card.setOnMouseClicked(event -> {
                    musicPlayer.playFromPlaylist(playlist);
                });

                // Puts the card in the container
                topPlaylistsContainer.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadTopTracks() {
        List<Track> topTracks = musicPlayer.getMostPlayedTracks(10);

        if (topTracks.isEmpty()) {
            tracksSection.setVisible(false);
            tracksSection.setManaged(false);
            return;
        }

        for (Track track : topTracks) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("MediaCardView.fxml"));
                VBox card = loader.load();

                MediaCardController cardController = loader.getController();
                cardController.setData(track.getTitle(), track.getAuthor());

                card.setOnMouseClicked(event -> {
                    musicPlayer.playTrack(track);
                });

                topTracksContainer.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}