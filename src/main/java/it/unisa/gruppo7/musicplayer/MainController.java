package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.library.LibraryController;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistDetailController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistSidebarController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

/**
 * Main controller that coordinates the overall application layout.
 * It manages switching the content area view between the main track library
 * and specific playlist detail panels, synchronized with sidebar selection events.
 *
 */
public class MainController {

    @FXML private PlaylistSidebarController playlistSidebarController;
    @FXML private LibraryController libraryController;
    @FXML private BorderPane contentArea;

    private Node libraryView;

    /**
     * Initializes the controller. Sets up the default center view component
     * and binds action handlers to the playlist sidebar.
     */
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

    /**
     * Responds to playlist deletion events. Redirects the view back to the library
     * if the deleted playlist was currently visible in the detail content area.
     *
     * @param deleted The playlist that was deleted.
     */
    private void onPlaylistDeleted(Playlist deleted) {
        // If the detail view of the deleted playlist is currently open, navigate back to the library
        if (contentArea.getCenter() != libraryView) {
            showLibrary();
        }
    }

    /**
     * Loads and displays the specific track breakdown for the selected playlist.
     * Injects the required application services and callback listeners into the loaded view controller.
     *
     * @param playlist The playlist to present details for.
     */
    private void showPlaylistDetail(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unisa/gruppo7/musicplayer/playlist/PlaylistDetail.fxml"));
            Parent playlistDetailView = loader.load();

            PlaylistDetailController controller = loader.getController();
            if (controller != null) {
                controller.setPlaylist(playlist);
                controller.setMusicPlayer(MusicPlayerFacade.getInstance());
                controller.setOnBackAction(() -> contentArea.setCenter(libraryView));
                controller.setOnRenameAction(() -> playlistSidebarController.refreshList());
                controller.setOnDeleteAction(() -> {
                    Platform.runLater(() -> {
                        playlistSidebarController.refreshList();
                        showLibrary();
                    });
                });
            }

            contentArea.setCenter(playlistDetailView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Switches the application view focus back to the primary main music track library screen.
     */
    public void showLibrary() {
        if (contentArea != null && libraryView != null) {
            contentArea.setCenter(libraryView);
        }
    }
}