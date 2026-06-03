package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.library.LibraryController;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistDetailController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistSidebarController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

public class MainController {

    @FXML private PlaylistSidebarController playlistSidebarController;
    @FXML private LibraryController libraryController;
    @FXML private BorderPane contentArea;

    private Node libraryView;

    @FXML
    public void initialize() {
        PlaylistService playlistService = MusicPlayerFacade.getInstance().getPlaylistService();

        if (contentArea != null) {
            libraryView = contentArea.getCenter();
        }

        if (playlistSidebarController != null) {
            playlistSidebarController.setOnPlaylistSelected(this::showPlaylistDetail);
            playlistSidebarController.setPlaylistService(playlistService);
        }
    }

    private void showPlaylistDetail(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unisa/gruppo7/musicplayer/playlist/PlaylistDetail.fxml"));
            Parent playlistDetailView = loader.load();

            PlaylistDetailController controller = loader.getController();
            if (controller != null) {
                controller.setPlaylist(playlist);
                controller.setMusicPlayer(MusicPlayerFacade.getInstance());
                controller.setOnBackAction(() -> contentArea.setCenter(libraryView));
            }

            contentArea.setCenter(playlistDetailView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showLibrary() {
        if (contentArea != null && libraryView != null) {
            contentArea.setCenter(libraryView);
        }
    }
}