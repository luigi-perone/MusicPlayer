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
import it.unisa.gruppo7.musicplayer.playlist.command.*;
import it.unisa.gruppo7.musicplayer.playlist.utils.AdditionResult;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Collection;
import java.util.List;

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
    private PlaylistTableConfigurator configurator;
    private RenameHandler        renameHandler;
    private Track playingTrack = null;

    @FXML
    public void initialize() {}

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
        facade.getPlaybackService().addObserver(this);
        configurator = new PlaylistTableConfigurator(
            playlistTrackTable, titleColumn, authorColumn,
            durationColumn, indexColumn, facade
        );
        configurator.configure(
            playingTrack,
            (i, track) -> CommandInvoker.execute(new PlayTrackCommand(facade, track))
        );

        renameHandler = new RenameHandler(
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

        refreshLabels();
        playlistTrackTable.refresh();
    }

    public void setOnBackAction(Runnable onBackAction) {
        this.onBackAction = onBackAction;
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
                    currentPlaylist,
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
        Track selectedTrack = playlistTrackTable.getSelectionModel().getSelectedItem();
        if (selectedTrack == null) {
            DialogUtils.showWarning("Traccia da eliminare non selezionata", "Per favore, seleziona una traccia dalla tabella prima di cliccare sul pulsante elimina");
            return;
        }

        String msg = "Sei sicuro di voler rimuovere '" + selectedTrack.getTitle() + "' dalla playlist?";
        boolean confirmed = DialogUtils.showConfirmation("Conferma eliminazione", "Eliminazione traccia", msg);

        if (confirmed) {
            Command<Void> removeCommand = new RemoveTrackCommand(currentPlaylist, selectedTrack);
            CommandInvoker.execute(removeCommand);

            if(adapter!=null){
                adapter.getItems().remove(selectedTrack);
                playlistTrackTable.getSelectionModel().clearSelection();
            }
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
                    currentPlaylist
            );

            CommandInvoker.execute(deleteCommand);

            if (onDeleteAction != null) {
                Platform.runLater(() -> onDeleteAction.run());
            }
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