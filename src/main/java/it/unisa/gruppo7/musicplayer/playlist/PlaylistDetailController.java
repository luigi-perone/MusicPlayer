package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistTableAdapter;
import javafx.scene.control.TableCell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controls the detail view of a single playlist.
 * Supports inline rename: double-click the title to edit,
 * Enter to confirm, Escape or focus-lost to cancel.
 */
public class PlaylistDetailController {

    @FXML private Label     playlistNameLabel;
    @FXML private TextField playlistNameField;
    @FXML private Label     trackCountLabel;
    @FXML private Label     totalDurationLabel;
    @FXML private Button    addTrack;
    @FXML private Button    deletePlaylistBtn;
    @FXML private TableView<Track>        playlistTrackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;
    @FXML private TableColumn<Track, Void> indexColumn;
    private Runnable            onBackAction;
    private Runnable            onDeleteAction;
    private Runnable            onRenameAction;
    private PlaylistTableAdapter adapter;
    private MusicPlayerFacade   facade;
    private Playlist            currentPlaylist;

    @FXML
    public void initialize() {

        indexColumn.setCellFactory(col -> new TableCell<Track, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formatted = MusicPlayerFacade.getInstance().formatDuration(track.getDuration());
            return new SimpleStringProperty(formatted);
        });

        playlistTrackTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        playlistNameLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                enterRenameMode();
            }
        });

        playlistNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)  applyRename();
            if (e.getCode() == KeyCode.ESCAPE) exitRenameMode();
        });

        playlistNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) exitRenameMode();
        });

        // --- Notify Facade when a track is selected (for Play Button)  ---
        playlistTrackTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        MusicPlayerFacade.getInstance().setSelectedTrack(newSelection);
                    }
                }
        );

        // --- Double click logic for Play Track ---

        playlistTrackTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Track> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                // Intercetta il doppio clic sulla riga non vuota
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Track selectedTrack = row.getItem();
                    MusicPlayerFacade.getInstance().playTrack(selectedTrack);
                }
            });
            return row;
        });

    }

    public void setPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;

        adapter = new PlaylistTableAdapter(playlist);
        playlistTrackTable.setItems(adapter.getItems());
        playlistNameLabel.setText(playlist.getName());

        refreshLabels(playlist);

        adapter.getItems().addListener((ListChangeListener.Change<? extends Track> c) -> {
            refreshLabels(playlist);
            if (facade != null) facade.savePlaylists();
        });
    }

    public void setMusicPlayer(MusicPlayerFacade facade) {
        this.facade = facade;
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    public void setOnDeleteAction(Runnable onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    /** Called by the parent controller to sync the sidebar after a rename. */
    public void setOnRenameAction(Runnable onRenameAction) {
        this.onRenameAction = onRenameAction;
    }

    /** Switches the title area from Label to TextField, pre-filled with the current name. */
    private void enterRenameMode() {
        playlistNameField.setText(playlistNameLabel.getText());
        playlistNameField.selectAll();

        playlistNameLabel.setVisible(false);
        playlistNameLabel.setManaged(false);
        playlistNameField.setVisible(true);
        playlistNameField.setManaged(true);
        playlistNameField.requestFocus();
    }

    /**
     * Validates the new name and delegates to {@link PlaylistService#renamePlaylist}.
     * Shows an Alert on error; on success updates the label and notifies the sidebar.
     */
    private void applyRename() {
        if (currentPlaylist == null || facade == null) return;

        String oldName = currentPlaylist.getName();
        String newName = playlistNameField.getText().trim();

        Optional<String> error = facade.getPlaylistService().renamePlaylist(oldName, newName);

        if (error.isPresent()) {
            showRenameError(error.get());
            playlistNameField.requestFocus();
            return;
        }

        playlistNameLabel.setText(currentPlaylist.getName());
        exitRenameMode();
        if (onRenameAction != null) onRenameAction.run();
    }

    /** Restores the Label without saving any change. */
    private void exitRenameMode() {
        playlistNameField.setVisible(false);
        playlistNameField.setManaged(false);
        playlistNameLabel.setVisible(true);
        playlistNameLabel.setManaged(true);
    }

    private void showRenameError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Rename failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshLabels(Playlist playlist) {
        trackCountLabel.setText(playlist.getTrackCount() + " tracks");
        totalDurationLabel.setText(
                MusicPlayerFacade.getInstance().formatDuration(playlist.getTotalDuration()));
    }

    @FXML
    private void onBackClick() {
        if (onBackAction != null) onBackAction.run();
    }

    @FXML
    private void onAddTrackClick() {
        if (adapter == null || facade == null) {
            throw new IllegalStateException("Adapter or facade is null");
        }

        Collection<Track> allLibraryTracks = facade.getTracksFromLibrary();
        List<Track> alreadyInPlaylist = adapter.getItems();

        List<Track> availableTracks = allLibraryTracks.stream()
                .filter(t -> !alreadyInPlaylist.contains(t))
                .collect(Collectors.toList());

        if (availableTracks.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No track available");
            alert.setHeaderText(null);
            alert.setContentText("All tracks are already in this playlist");
            alert.showAndWait();
            return;
        }

        Dialog<List<Track>> dialog = new Dialog<>();
        dialog.setTitle("Add tracks");
        dialog.setHeaderText("Select tracks");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        ListView<Track> listView = new ListView<>();
        listView.getItems().addAll(availableTracks);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(param -> new ListCell<Track>() {
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getTitle() + " - " + item.getAuthor());
            }
        });
        listView.setPrefSize(350, 400);
        dialog.getDialogPane().setContent(new VBox(listView));

        dialog.setResultConverter(dialogButton ->
                dialogButton == addButtonType
                        ? new ArrayList<>(listView.getSelectionModel().getSelectedItems())
                        : null);

        dialog.showAndWait().ifPresent(selected -> selected.forEach(adapter::trackAdded));
    }

    @FXML
    private void onRemoveTrackClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Track to be deleted not selected");
            alert.setHeaderText(null);
            alert.setContentText("Please, select a track on the table before clicking on delete button");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm elimination");
        alert.setHeaderText("Elimination track");
        alert.setContentText("Are you sure you want to remove '"
                + selectedTrack.getTitle() + "' from the playlist?");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && adapter != null) adapter.trackRemoved(selectedTrack);
        });
    }

    @FXML
    private void onDeletePlaylistClick() {
        String name = playlistNameLabel.getText();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Elimina playlist");
        alert.setHeaderText("Eliminare \"" + name + "\"?");
        alert.setContentText("L'operazione è irreversibile. Le tracce nella libreria non saranno toccate.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (facade != null) facade.getPlaylistService().deletePlaylist(name);
                if (onDeleteAction != null) onDeleteAction.run();
            }
        });
    }

    @FXML
    private void onPlayTrackClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        MusicPlayerFacade.getInstance().playTrack(selectedTrack);
    }
}