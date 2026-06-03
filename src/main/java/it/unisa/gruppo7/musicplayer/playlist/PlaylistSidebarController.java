package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;
import java.util.function.Consumer;
import it.unisa.gruppo7.musicplayer.playlist.Playlist;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

/**
 * Manages the communication between the model and the view of the playlist's sidebar.
 * * @author Maxim Makhovskyy
 */
public class PlaylistSidebarController {
    @FXML private VBox listBox;
    @FXML private Button addBtn;

    private PlaylistService service;
    private HBox editRow;
    private Consumer<Playlist> onPlaylistSelected;

    /**
     * Flag used to avoid the premature close of the edit module. It is set true when the user
     * clicks a UI button so that the TextField's focus-lost listener ignores the focuse change.
     */
    private boolean committing;

    @FXML
    private void initialize() {
        addBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> committing = true);
    }

    public void setPlaylistService(PlaylistService service){
        this.service = service;
        refreshList();
    }

    public void setOnPlaylistSelected(Consumer<Playlist> listener) {
        this.onPlaylistSelected = listener;
    }

    private void refreshList(){
        listBox.getChildren().clear();
        for(Playlist p: service.getPlaylists()){
            listBox.getChildren().add(playlistRow(p));
        }
    }

    private HBox playlistRow(Playlist playlist){
        Label label = new Label(playlist.getName());
        label.getStyleClass().add("row-label");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("row");

        row.setOnMouseClicked(e -> {
            if (onPlaylistSelected != null) {
                onPlaylistSelected.accept(playlist);
            }
        });

        return row;
    }

    @FXML
    private void onAddToggle(){
        if(editRow != null){
            closeEdit();
            committing = false;
            return;
        }

        committing = false;
        addBtn.setText("×");
        addBtn.getStyleClass().add("cancel-btn");

        TextField field = new TextField();
        field.setPromptText("Playlist name...");
        field.getStyleClass().add("playlist-input");
        HBox.setHgrow(field, Priority.ALWAYS);

        Label tip = new Label();
        tip.getStyleClass().add("field-error");
        tip.setManaged(false);
        tip.setVisible(false);

        editRow = new HBox(8, dot(field), field, confirmBtn(field, tip));
        editRow.setAlignment(Pos.CENTER_LEFT);
        editRow.getStyleClass().add("edit-row");

        VBox wrap = new VBox(editRow, tip);
        listBox.getChildren().add(0, wrap);
        Platform.runLater(field::requestFocus);

        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)  confirm(field, tip);
            if (e.getCode() == KeyCode.ESCAPE) closeEdit();
        });

        field.textProperty().addListener((obs, ov, nv) -> {
            if(tip.isVisible()){
                if(nv != null && !nv.trim().isEmpty()){
                    hideError(field, tip);
                }
            }
        });

        field.focusedProperty().addListener((obs, was, is) -> {
            if (!is && editRow != null && !committing && field.getText().trim().isEmpty()) {
                closeEdit();
            }
        });
    }

    private void confirm(TextField field, Label tip){
        String name = field.getText();
        try{
            Playlist p = service.createPlaylist(name);
            committing = true;
            closeEdit();

            HBox row = playlistRow(p);
            listBox.getChildren().add(row);
            if (onPlaylistSelected != null) {
                onPlaylistSelected.accept(p);
            }
            committing = false;
        }catch(IllegalArgumentException e){
            tip.setText(e.getMessage());
            tip.setManaged(true);
            tip.setVisible(true);
            
            if(!field.getStyleClass().contains("error")){
                field.getStyleClass().add("error");
            }
            field.requestFocus();
        }
    }

    private void hideError(TextField field, Label tip) {
        tip.setVisible(false);
        tip.setManaged(false);
        field.getStyleClass().remove("error");
    }

    private void closeEdit(){
        if(editRow == null) return;
        Node wrap = editRow.getParent();
        listBox.getChildren().remove(wrap);
        editRow = null;

        addBtn.setText("+");
        addBtn.getStyleClass().remove("cancel-btn");
    }

    private StackPane dot(TextField field) {
        Label initial = new Label("?");
        initial.getStyleClass().add("dot-label");
        StackPane tile = new StackPane(initial);
        tile.setMinSize(26, 26);
        tile.setPrefSize(26, 26);
        tile.setMaxSize(26, 26);
        tile.getStyleClass().add("dot");
        field.textProperty().addListener((obs, ov, nv) -> {
            String t = nv == null ? "" : nv.trim();
            initial.setText(t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase());
        });
        return tile;
    }

    private Button confirmBtn(TextField field, Label tip) {
        Button b = new Button("\u2713");
        b.getStyleClass().add("confirm-btn");
        b.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> committing = true);
        b.setOnAction(e -> { confirm(field, tip); committing = false; });
        return b;
    }
}