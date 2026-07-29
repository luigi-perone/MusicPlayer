package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.homepage.HomePageController;
import it.unisa.gruppo7.musicplayer.library.LibraryController;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackController;
import it.unisa.gruppo7.musicplayer.playback.PlaybackQueueController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistDetailController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistSidebarController;
import it.unisa.gruppo7.musicplayer.undo.UndoToast;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

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
    @FXML private PlaybackController playbackController;
    @FXML private BorderPane contentArea;

    @FXML private HomePageController homePageController;

    @FXML private VBox queue;
    @FXML private PlaybackQueueController queueController;
    private PlaylistDetailController currentDetailController;

    private Node libraryView;
    private Node homePageView;

    /** The shared application facade, dependency-injected by the controller factory. */
    private final MusicPlayerFacade musicPlayer;

    /**
     * Creates the main controller with the facade injected by the controller factory.
     *
     * @param musicPlayer the shared application facade.
     */
    public MainController(MusicPlayerFacade musicPlayer) {
        this.musicPlayer = musicPlayer;
    }

    /**
     * Initializes the controller. Sets up the default center view component
     * and binds action handlers to the playlist sidebar.
     */
    @FXML
    public void initialize() {
        PlaylistService playlistService = musicPlayer.getPlaylistService();

        if (contentArea != null) {
            homePageView = contentArea.getCenter();
            UndoToast.setBottomBar(contentArea.getBottom());
            registerUndoShortcut();
        }

        if (playlistSidebarController != null) {
            playlistSidebarController.setOnPlaylistSelected(this::showPlaylistDetail);
            playlistSidebarController.setPlaylistService(playlistService);
            playlistSidebarController.setMainController(this);
        }

        if (playbackController != null) {
            playbackController.setMainController(this);
        }

        if (libraryController != null) {
            libraryController.setMainController(this);
        }

        if (homePageController != null) {
            homePageController.setMainController(this);
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
            loader.setControllerFactory(new ControllerFactory(musicPlayer));
            Parent playlistDetailView = loader.load();

            PlaylistDetailController controller = loader.getController();
            if (controller != null) {
                currentDetailController = controller;
                controller.setPlaylist(playlist);
                controller.setMusicPlayer(musicPlayer);
                controller.setOnBackAction(() -> contentArea.setCenter(libraryView));
                controller.setOnRenameAction(() -> playlistSidebarController.refreshList());
                controller.setOnPlaylistRestored(() -> playlistSidebarController.refreshList());
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
        try {
            if (libraryView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unisa/gruppo7/musicplayer/LibraryView.fxml"));
                loader.setControllerFactory(new ControllerFactory(musicPlayer));
                libraryView = loader.load();

                libraryController = loader.getController();
                libraryController.setMainController(this);
            }

            if (contentArea != null) {
                contentArea.setCenter(libraryView);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nel caricamento della Libreria.");
        }
    }

    /**
     * Switches the application view focus back to the primary main homepage screen.
     */
    public void showHomePage() {
        if (contentArea != null && homePageView != null) {
            if (homePageController != null) {
                homePageController.refreshHomePage();
            }
            contentArea.setCenter(homePageView);
        }
    }

    /**
     * Shows or hides the side queue panel.
     */
    public void toggleQueueVisibility() {
        if (queue != null) {
            boolean isNowVisible = !queue.isVisible();
            queue.setVisible(isNowVisible);
            queue.setManaged(isNowVisible);

            // If the queue becomes visible, refresh it to be safe
            if (isNowVisible && queueController != null) {
                queueController.refreshQueue();
            }
        }
    }

    /**
     * Forces a visual refresh of the queued tracks list.
     */
    public void refreshQueueView() {
        if (queueController != null && queue != null && queue.isVisible()) {
            queueController.refreshQueue();
        }
    }

    /**
     * Binds Ctrl/Cmd+Z to the undo action once the scene is available. The shortcut
     * triggers the action shown by the current undo toast, so it works within the same
     * time window as the toast.
     */
    private void registerUndoShortcut() {
        if (contentArea.getScene() != null) {
            installUndoAccelerator(contentArea.getScene());
        }
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installUndoAccelerator(newScene);
            }
        });
    }

    /**
     * Installs the Ctrl/Cmd+Z accelerator on the given scene, mapping it to the undo action.
     *
     * @param scene the scene to install the accelerator on
     */
    private void installUndoAccelerator(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                this::performUndo
        );
    }

    /**
     * Handles an undo request (Ctrl/Cmd+Z). If an undo toast is showing it triggers that
     * toast's own action (which already refreshes the relevant view); otherwise it undoes
     * the last action globally and refreshes the views.
     */
    private void performUndo() {
        if (UndoToast.isShowing()) {
            UndoToast.triggerUndo();
        } else if (musicPlayer.undoLastAction()) {
            refreshAllViews();
        }
    }

    /**
     * Refreshes every visible view (sidebar, library, open playlist detail and queue)
     * after a global change such as an undo, navigating back to the library if the
     * playlist currently shown no longer exists.
     */
    public void refreshAllViews() {
        if (playlistSidebarController != null) {
            playlistSidebarController.refreshList();
        }
        if (libraryController != null) {
            libraryController.reload();
        }
        if (currentDetailController != null) {
            Playlist shown = currentDetailController.getCurrentPlaylist();
            if (shown != null && !musicPlayer.getPlaylists().contains(shown)) {
                // The playlist being shown was just undone away: go back to the library.
                currentDetailController = null;
                showLibrary();
            } else {
                currentDetailController.reload();
            }
        }
        refreshQueueView();
    }
}