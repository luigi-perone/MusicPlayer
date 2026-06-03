package it.unisa.gruppo7.musicplayer.playlist;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
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
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Control the communication between model and view of the playlist's detail page.
 *
 */
public class PlaylistDetailController {

    @FXML private Label playlistNameLabel;
    @FXML private Label trackCountLabel;
    @FXML private Label totalDurationLabel;
    @FXML private Button addTrack;
    @FXML private Button deletePlaylistBtn;
    @FXML private TableView<Track> playlistTrackTable;
    @FXML private TableColumn<Track, String> titleColumn;
    @FXML private TableColumn<Track, String> authorColumn;
    @FXML private TableColumn<Track, String> durationColumn;

    private Runnable onBackAction;
    private Runnable onDeleteAction;
    private PlaylistTableAdapter adapter;
    private MusicPlayerFacade facade;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            String formattedTime = MusicPlayerFacade.getInstance().formatDuration(track.getDuration());
            return new SimpleStringProperty(formattedTime);
        });
    }

    public void setPlaylist(Playlist playlist){
        adapter = new PlaylistTableAdapter(playlist);
        playlistTrackTable.setItems(adapter.getItems());
        playlistNameLabel.setText(playlist.getName());
        
        refreshLabels(playlist);

        adapter.getItems().addListener((ListChangeListener.Change<? extends Track> c) -> {
            refreshLabels(playlist);

            if (facade != null){
                facade.savePlaylists();
            }
        });
    }

    public void setMusicPlayer(MusicPlayerFacade facade){
        this.facade = facade;
    }

    public void setPlaylistName(String name) {
        playlistNameLabel.setText(name);
    }

    private void refreshLabels(Playlist playlist) {
        trackCountLabel.setText(playlist.getTrackCount() + " tracks");
        totalDurationLabel.setText(MusicPlayerFacade.getInstance().formatDuration(playlist.getTotalDuration()));
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

    @FXML
    private void onAddTrackClick(){
        if(adapter==null || facade==null){
            throw new IllegalStateException("Adapter or facade is null");
        }

        Collection<Track> allLibraryTracks = facade.getTracksFromLibrary(); 
        List<Track> alreadyInPlaylist = adapter.getItems();

        List<Track> availableTracks = allLibraryTracks.stream()
                    .filter(t -> !alreadyInPlaylist.contains(t))
                    .collect(Collectors.toList());
        
        if(availableTracks.isEmpty()){
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
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " - " + item.getAuthor());
                }
            }
        });

        listView.setPrefSize(350, 400);
        dialog.getDialogPane().setContent(new VBox(listView));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return new ArrayList<>(listView.getSelectionModel().getSelectedItems());
            }
            return null;
        });

        Optional<List<Track>> result = dialog.showAndWait();
        
        result.ifPresent(selectedTracks -> {
            for (Track t : selectedTracks) {
                adapter.trackAdded(t);
            }
        });
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
        alert.setContentText("Are you sure you want to remove '" + selectedTrack.getTitle() + "' from the playlist?");

        Optional<ButtonType> result = alert.showAndWait();

        if ((result.isPresent() && result.get() == ButtonType.OK) && adapter != null) {
            adapter.trackRemoved(selectedTrack);
        }
    }

    public void setOnDeleteAction(Runnable onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    @FXML
    private void onDeletePlaylistClick() {
        String name = playlistNameLabel.getText();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Elimina playlist");
        alert.setHeaderText("Eliminare \"" + name + "\"?");
        alert.setContentText("L'operazione è irreversibile. Le tracce nella libreria non saranno toccate.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (facade != null) {
                facade.getPlaylistService().deletePlaylist(name);
            }
            if (onDeleteAction != null) {
                onDeleteAction.run();
            }
        }
    }
}