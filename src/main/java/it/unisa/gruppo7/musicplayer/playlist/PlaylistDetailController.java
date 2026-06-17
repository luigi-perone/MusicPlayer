package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.core.TrackObserver;
import it.unisa.gruppo7.musicplayer.dialog.DialogBuilder;
import it.unisa.gruppo7.musicplayer.dialog.DialogDirector;
import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;
import it.unisa.gruppo7.musicplayer.dialog.DialogTag;
import it.unisa.gruppo7.musicplayer.dialog.TrackSelectionDialogBuilder;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.observer.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.command.*;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.undo.UndoToast;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.Collection;
import java.util.List;

/**
 * Controller for the playlist detail view.
 * Manages the display of tracks within a selected playlist, updates track metadata,
 * and handles UI actions such as adding/removing tracks, renaming, or deleting the playlist.
 * * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlaylistDetailController implements PlaybackObserver, TrackObserver {

    @FXML private Label     playlistNameLabel;
    @FXML private TextField playlistNameField;
    @FXML private Label     trackCountLabel;
    @FXML private Label     totalDurationLabel;
    @FXML private Button    addTrack;
    @FXML private Button    manageTagsBtn;
    @FXML private Button    deletePlaylistBtn;
    @FXML private TableView<Track>           playlistTrackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;
    @FXML private TableColumn<Track, String> tagColumn;
    @FXML private TableColumn<Track, Void>   indexColumn;

    private Runnable             onBackAction;
    private Runnable             onDeleteAction;
    private Runnable             onRenameAction;
    private Runnable             onPlaylistRestored;
    private PlaylistTableAdapter adapter;
    private MusicPlayerFacade    facade;
    private Playlist             currentPlaylist;
    private Track playingTrack = null;

    /**
     * Initializes the controller class. Called automatically after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {}

    /**
     * Called when the currently playing track changes. Updates the internal reference
     * and refreshes the table to update row styling.
     *
     * @param newTrack The newly playing track.
     */
    @Override
    public void onTrackChanged(Track newTrack) {
        this.playingTrack = newTrack;
        Platform.runLater(() -> {
            playlistTrackTable.getSelectionModel().clearSelection();
            playlistTrackTable.refresh();
        });
    }

    /**
     * Called when the playback state changes. Clears the playing track styling if playback stops.
     *
     * @param newState The new playback state.
     */
    @Override
    public void onStateChanged(PlaybackState newState) {
        if (newState == PlaybackState.STOPPED) {
            this.playingTrack = null;
            Platform.runLater(() -> playlistTrackTable.refresh());
        }
    }

    /**
     * Called on periodic playback time updates.
     *
     * @param simulatedSeconds The elapsed simulation time in seconds.
     */
    @Override
    public void onTimeTick(int simulatedSeconds) {
    }

    /**
     * Sets the callback action to be executed when the playlist is deleted.
     *
     * @param onDeleteAction The action to execute.
     */
    public void setOnDeleteAction(Runnable onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    /**
     * Sets the callback action to be executed when the playlist is successfully renamed.
     *
     * @param onRenameAction The action to execute.
     */
    public void setOnRenameAction(Runnable onRenameAction) {
        this.onRenameAction = onRenameAction;
    }

    /**
     * Binds a specific playlist to this controller, populating the track table
     * and initializing data listeners.
     *
     * @param playlist The playlist model to display.
     */
    public void setPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
        adapter = new PlaylistTableAdapter(playlist);
        playlistTrackTable.setItems(adapter.getItems());
        playlistNameLabel.setText(playlist.getName());

        refreshLabels();

        adapter.getItems().addListener((ListChangeListener.Change<? extends Track> c) -> {
            refreshLabels();
            if (facade != null) facade.savePlaylists();
        });
    }

    /**
     * Injects the core music player facade, configures the table component,
     * initializes event behaviors, and registers this controller as a playback observer.
     *
     * @param facade The system backend facade.
     */
    public void setMusicPlayer(MusicPlayerFacade facade) {
        this.facade = facade;
        facade.getPlaybackService().addObserver(this);
        facade.addObserver(this);

        this.playingTrack = facade.getCurrentPlayingTrack();

        PlaylistTableConfigurator configurator = new PlaylistTableConfigurator(
                playlistTrackTable, titleColumn, authorColumn,
                durationColumn, tagColumn, indexColumn, facade
        );

        configurator.configure(
                () -> {
                    if (facade.getActivePlaylist() != null && facade.getActivePlaylist().equals(this.currentPlaylist)) {
                        return facade.getCurrentPlayingTrack();
                    }
                    return null;
                },
                (i, track) -> CommandInvoker.execute(new PlayTrackCommand(facade, currentPlaylist, track)),
                this::reorderTrack
        );

        RenameHandler renameHandler = new RenameHandler(
                playlistNameLabel, playlistNameField,
                () -> {
                    if (currentPlaylist == null) return;
                    String newName = playlistNameField.getText().trim();
                    CommandInvoker.execute(new RenamePlaylistCommand(facade.getPlaylistService(), currentPlaylist, newName));
                    playlistNameLabel.setText(newName);
                    if (onRenameAction != null) onRenameAction.run();
                }
        );

        playlistTrackTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> { if (now != null) facade.setSelectedTrack(now); });
        manageTagsBtn.disableProperty().bind(
                playlistTrackTable.getSelectionModel().selectedItemProperty().isNull()
        );        
        refreshLabels();
        playlistTrackTable.refresh();
    }

    /**
     * Sets the callback action to be executed when the back button is clicked.
     *
     * @param onBackAction The action to execute.
     */
    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    /**
     * Updates the UI labels displaying the track count and total aggregated duration.
     */
    private void refreshLabels() {
        if (adapter == null) return;

        List<Track> currentTracks = adapter.getItems();
        trackCountLabel.setText(currentTracks.size() + " tracks");

        if (this.facade != null) {
            int totalDuration = currentTracks.stream()
                    .mapToInt(Track::getDuration)
                    .sum();

            totalDurationLabel.setText(this.facade.formatDuration(totalDuration));
        }
    }

    /**
     * Triggers the navigation back action callback.
     */
    @FXML
    private void onBackClick() {
        if (onBackAction != null) onBackAction.run();
    }

    /**
     * Filters out tracks already present in the playlist from the global music library.
     *
     * @return A list of tracks available to be added.
     */
    private List<Track> getAvailableTracksToAdd() {
        List<Track> alreadyInPlaylist = adapter.getItems();
        Collection<Track> allLibraryTracks = facade.getTracksFromLibrary();
        return allLibraryTracks.stream()
                .filter(t -> !alreadyInPlaylist.contains(t))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Opens a selection dialog containing all library tracks not yet added,
     * executing an insertion command if the user confirms.
     */
    @FXML
    private void onAddTrackClick() {
        if (adapter == null || facade == null) return;

        List<Track> availableTracks = getAvailableTracksToAdd();
        if (availableTracks.isEmpty()) {
            DialogUtils.showInfo("Nessuna traccia disponibile", "Tutte le tracce sono già nella playlist.");
            return;
        }

        DialogDirector director = new DialogDirector();
        DialogBuilder<List<Track>> builder = new TrackSelectionDialogBuilder(availableTracks);
        Dialog<List<Track>> dialog = director.construct(builder);

        dialog.showAndWait().ifPresent(selectedTracks -> {
            Command<AdditionResult> addCommand = new AddTracksCommand(
                    facade.getPlaylistService(),
                    facade.getPlaybackService(),
                    currentPlaylist,
                    selectedTracks
            );

            CommandInvoker.execute(addCommand).ifPresent(result -> {
                if (result.hasAdded()) {
                    adapter.getItems().addAll(selectedTracks);
                    // US-018: keep queue in sync if this playlist is the active source
                    for (Track t : selectedTracks) {
                        facade.onTrackAddedToPlaylist(currentPlaylist, t);
                    }
                }
            });
        });
    }

    /**
     * Removes the currently selected track from the table view and playlist database model
     * after requiring explicit user confirmation.
     */
    @FXML
    private void onRemoveTrackClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            DialogUtils.showWarning("Traccia da eliminare non selezionata", "Per favore, seleziona una traccia dalla tabella prima di cliccare sul pulsante elimina");
            return;
        }

        String msg = "Sei sicuro di voler rimuovere '" + selectedTrack.getTitle() + "' dalla playlist?";
        boolean confirmed = DialogUtils.showConfirmation("Conferma eliminazione", "Eliminazione traccia", msg);

        if (confirmed) {
            Command<Void> removeCommand = new RemoveTrackCommand(
                    facade.getPlaylistService(),
                    facade.getPlaybackService(),
                    currentPlaylist, 
                    selectedTrack
            );
            CommandInvoker.execute(removeCommand);

            if (adapter != null) {
                adapter.getItems().remove(selectedTrack);
                playlistTrackTable.getSelectionModel().clearSelection();
                facade.onTrackRemovedFromPlaylist(currentPlaylist, selectedTrack);
            }

            showUndoToast("Traccia rimossa dalla playlist");
        }
    }

        @FXML
    private void onManageTagsClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack != null) {
            showTagDialog(selectedTrack);
        }
    }

    private void showTagDialog(Track track) {
        DialogTag.show(track).ifPresent(selectedTags -> {
            facade.updateTrackTags(track, selectedTags);
            playlistTrackTable.refresh();
        });
    }

    /**
     * Reorders a track within the playlist after a drag-and-drop gesture (US-027).
     * Persists the new order via {@link ReorderTrackCommand}, mirrors the move in the
     * bound UI list, keeps the live playback queue in sync when this playlist is the
     * active source, and restores the selection on the moved row.
     *
     * @param from The previous index of the moved track.
     * @param to   The new index of the moved track.
     */
    private void reorderTrack(int from, int to) {
        if (currentPlaylist == null || adapter == null || facade == null) return;

        CommandInvoker.execute(new ReorderTrackCommand(facade.getPlaylistService(), currentPlaylist, from, to));

        adapter.moveTrack(from, to);
        facade.onTrackReorderedInPlaylist(currentPlaylist, from, to);

        playlistTrackTable.getSelectionModel().select(to);
        playlistTrackTable.refresh();
    }

    /**
     * Deletes the entire playlist module permanently via structural command confirmation.
     * Fires the completion action callback upon success.
     */
    @FXML
    private void onDeletePlaylistClick() {
        String name = playlistNameLabel.getText();

        boolean confirmed = DialogUtils.showConfirmation(
                "Elimina playlist",
                "Eliminare \"" + name + "\"?",
                "L'operazione è irreversibile. Le tracce nella libreria non saranno toccate."
        );

        if (confirmed) {
            Window window = (playlistNameLabel.getScene() != null) ? playlistNameLabel.getScene().getWindow() : null;
            Command<Void> deleteCommand = new DeletePlaylistCommand(
                    facade.getPlaylistService(),
                    currentPlaylist
            );

            CommandInvoker.execute(deleteCommand);

            if (onDeleteAction != null) {
                Platform.runLater(() -> onDeleteAction.run());
            }

            if (window != null) {
                UndoToast.show(window, "Playlist eliminata", () -> {
                    if (facade.undoLastAction() && onPlaylistRestored != null) {
                        onPlaylistRestored.run();
                    }
                });
            }
        }
    }

    /**
     * Attempts to instantly play back the track currently selected within the table view.
     */
    @FXML
    private void onPlayTrackClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack != null) {
            CommandInvoker.execute(new PlayTrackCommand(facade, currentPlaylist, selectedTrack));
        }
    }

    /**
     * Starts the sequential playback of all tracks in the playlist.
     * Method called by pressing the "Play" button in the FXML interface.
     */
    public void onPlayPlaylistClick() {
        try{
            MusicPlayerFacade.getInstance().playFromPlaylist(currentPlaylist);
        } catch (IllegalArgumentException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Triggered when a track is permanently removed from the application.
     * Removes the track from the active playlist adapter and refreshes UI labels.
     *
     * @param track The track that was deleted globally.
     */
    @Override
    public void onTrackDeleted(Track track) {
        Platform.runLater(() -> {
            adapter.getItems().remove(track);
            playlistTrackTable.refresh();
            refreshLabels();
        });
    }

    /**
     * Triggered when a track's metadata (e.g., title, author) is modified.
     * Forces a refresh of the table view to display the most up-to-date information.
     *
     * @param track The track that was edited globally.
     */
    @Override
    public void onTrackEdit(Track track) {
        Platform.runLater(() -> {
            if (currentPlaylist == null || adapter == null) {
                return;
            }

            adapter.getItems().setAll(
                    currentPlaylist.getPlaylist()
            );

            playlistTrackTable.refresh();
            refreshLabels();
        });
    }

    /**
     * Triggered when the structural order or content of the playback queue changes.
     * Clears the current table selection and triggers a refresh to maintain visual consistency.
     */
    @Override
    public void onQueueChanged() {
        Platform.runLater(() -> {
            playlistTrackTable.getSelectionModel().clearSelection();
            playlistTrackTable.refresh();
        });
    }

    private void showUndoToast(String message) {
        if (playlistTrackTable.getScene() == null) return;
        UndoToast.show(playlistTrackTable.getScene().getWindow(), message, () -> {
            if (facade.undoLastAction()) {
                reload();
            }
        });
    }

    /**
     * Sets the callback run after an undone playlist deletion (to refresh the sidebar).
     *
     * @param onPlaylistRestored The action to execute.
     */
    public void setOnPlaylistRestored(Runnable onPlaylistRestored) {
        this.onPlaylistRestored = onPlaylistRestored;
    }

    /**
     * Re-syncs the table with the playlist model. Used after an undo (including a
     * global Ctrl/Cmd+Z) so the view reflects the restored state.
     */
    public void reload() {
        if (adapter == null || currentPlaylist == null) return;
        adapter.getItems().setAll(currentPlaylist.getTracks());
        playlistTrackTable.getSelectionModel().clearSelection();
        playlistTrackTable.refresh();
        refreshLabels();
    }

    /**
     * @return the playlist currently shown by this controller, or null.
     */
    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }
}