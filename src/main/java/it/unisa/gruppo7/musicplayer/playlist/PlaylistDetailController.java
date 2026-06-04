package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.dialog.DialogBuilder;
import it.unisa.gruppo7.musicplayer.dialog.DialogDirector;
import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;
import it.unisa.gruppo7.musicplayer.dialog.TrackSelectionDialogBuilder;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackObserver;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.util.Collection;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

public class PlaylistDetailController implements PlaybackObserver {

    @FXML private Label     playlistNameLabel;
    @FXML private TextField playlistNameField;
    @FXML private Label     trackCountLabel;
    @FXML private Label     totalDurationLabel;
    @FXML private Button    addTrack;
    @FXML private Button    deletePlaylistBtn;
    @FXML private TableView<Track>           playlistTrackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;
    @FXML private TableColumn<Track, Void>   indexColumn;

    private Runnable             onBackAction;
    private Runnable             onDeleteAction;
    private Runnable             onRenameAction;
    private PlaylistTableAdapter adapter;
    private MusicPlayerFacade    facade;
    private Playlist             currentPlaylist;
    private Track playingTrack = null;

    @FXML
    public void initialize() {

        indexColumn.setCellFactory(col -> new TableCell<Track, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || getTableRow() == null) ? null
                        : String.valueOf(getIndex() + 1));
            }
        });

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formatted = (this.facade != null) ? this.facade.formatDuration(track.getDuration()) : "";
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

        playlistTrackTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        this.facade.setSelectedTrack(newSelection);
                    }
                }
        );



        playlistTrackTable.setRowFactory(tv -> {
            TableRow<Track> row = new TableRow<Track>() {
                @Override
                protected void updateItem(Track item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if (item.equals(playingTrack)) {
                        setStyle("-fx-background-color: #d4edda; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setOnMouseClicked(event -> {
                // Intercetta il doppio clic sulla riga non vuota
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Track selectedTrack = row.getItem();
                    CommandInvoker.execute(new PlayTrackCommand(facade, selectedTrack));
                }
            });
            return row;
        });

        MusicPlayerFacade.getInstance().getPlaybackService().addObserver(this);
    }

    @Override
    public void onTrackChanged(Track newTrack) {
        this.playingTrack = newTrack;
        Platform.runLater(() -> playlistTrackTable.refresh());
    }

    @Override
    public void onStateChanged(PlaybackState newState) {
        if (newState == PlaybackState.STOPPED) {
            this.playingTrack = null;
            Platform.runLater(() -> playlistTrackTable.refresh());
        }
    }

    @Override
    public void onTimeTick(int simulatedSeconds) {
    }

    public void setOnDeleteAction(Runnable onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    public void setOnRenameAction(Runnable onRenameAction) {
        this.onRenameAction = onRenameAction;
    }

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

    public void setMusicPlayer(MusicPlayerFacade facade) {
        this.facade = facade;
        refreshLabels();
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    private void enterRenameMode() {
        playlistNameField.setText(playlistNameLabel.getText());
        playlistNameField.selectAll();
        playlistNameLabel.setVisible(false);
        playlistNameLabel.setManaged(false);
        playlistNameField.setVisible(true);
        playlistNameField.setManaged(true);
        playlistNameField.requestFocus();
    }

    private void applyRename() {
        if (currentPlaylist == null || facade == null) return;
        String oldName = currentPlaylist.getName();
        String newName = playlistNameField.getText().trim();

        Command<Void> renameCommand = new RenamePlaylistCommand(facade.getPlaylistService(), oldName, newName);
        CommandInvoker.execute(renameCommand).ifPresent(v -> {
            playlistNameLabel.setText(newName);
            exitRenameMode();
            if (onRenameAction != null) onRenameAction.run();
        });
    }

    private void exitRenameMode() {
        playlistNameField.setVisible(false);
        playlistNameField.setManaged(false);
        playlistNameLabel.setVisible(true);
        playlistNameLabel.setManaged(true);
    }

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

    @FXML private void onBackClick() { if (onBackAction != null) onBackAction.run(); }

    private List<Track> getAvailableTracksToAdd() {
        List<Track> alreadyInPlaylist = adapter.getItems();
        Collection<Track> allLibraryTracks = facade.getTracksFromLibrary();
        return allLibraryTracks.stream()
                .filter(t -> !alreadyInPlaylist.contains(t))
                .collect(java.util.stream.Collectors.toList());
    }

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
                    currentPlaylist.getName(),
                    selectedTracks
            );

            CommandInvoker.execute(addCommand).ifPresent(result -> {
                if (result.hasAdded()) {
                    adapter.getItems().addAll(selectedTracks);
                }
            });

        });
    }

    @FXML
    private void onRemoveTrackClick() {
        Track selectedTrack =
                playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            DialogUtils.showWarning("Track to be deleted not selected", "Please, select a track on the table before clicking on delete button");
            return;
        }

        String msg = "Are you sure you want to remove '" + selectedTrack.getTitle() + "' from the playlist?";
        boolean confirmed = DialogUtils.showConfirmation("Confirm elimination", "Elimination track", msg);

        if (confirmed) {
            Command<Void> removeCommand = new RemoveTrackCommand(currentPlaylist, selectedTrack);
            CommandInvoker.execute(removeCommand).ifPresent(v -> {
                if (adapter != null) adapter.getItems().remove(selectedTrack);
            });
        }
    }

    @FXML
    private void onDeletePlaylistClick() {
        String name = playlistNameLabel.getText();

        boolean confirmed = DialogUtils.showConfirmation(
            "Elimina playlist",
            "Eliminare \"" + name + "\"?",
            "L'operazione è irreversibile. Le tracce nella libreria non saranno toccate."
        );

        if (confirmed) {
            Command<Void> deleteCommand = new DeletePlaylistCommand(
                facade.getPlaylistService(),
                name
            );

            CommandInvoker.execute(deleteCommand).ifPresent(v -> {
                if (onDeleteAction != null) onDeleteAction.run();
            });
        }
    }

    @FXML
    private void onPlayTrackClick() {
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack != null) {
            CommandInvoker.execute(new PlayTrackCommand(facade, selectedTrack));
        }
    }
}