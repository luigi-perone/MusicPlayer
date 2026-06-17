package it.unisa.gruppo7.musicplayer;

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

    @FXML private VBox queue;
    @FXML private PlaybackQueueController queueController;
    private PlaylistDetailController currentDetailController;

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
                currentDetailController = controller;
                controller.setPlaylist(playlist);
                controller.setMusicPlayer(MusicPlayerFacade.getInstance());
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
        if (contentArea != null && libraryView != null) {
            contentArea.setCenter(libraryView);
        }
    }

    /**
     * Mostra o nasconde il pannello laterale della coda.
     */
    public void toggleQueueVisibility() {
        if (queue != null) {
            boolean isNowVisible = !queue.isVisible();
            queue.setVisible(isNowVisible);
            queue.setManaged(isNowVisible);

            // Se la coda diventa visibile, aggiorniamola per sicurezza
            if (isNowVisible && queueController != null) {
                queueController.refreshQueue();
            }
        }
    }

    /**
     * Forza l'aggiornamento grafico della lista dei brani in coda.
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
        } else if (MusicPlayerFacade.getInstance().undoLastAction()) {
            refreshAllViews();
        }
    }

    public void refreshAllViews() {
        if (playlistSidebarController != null) {
            playlistSidebarController.refreshList();
        }
        if (libraryController != null) {
            libraryController.reload();
        }
        if (currentDetailController != null) {
            Playlist shown = currentDetailController.getCurrentPlaylist();
            if (shown != null && !MusicPlayerFacade.getInstance().getPlaylists().contains(shown)) {
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