package it.unisa.gruppo7.musicplayer.playlist;

import java.util.Optional;

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
 * 
 * @author Maxim Makhovskyy
 */
public class PlaylistSidebarController {
    @FXML private VBox listBox;
    @FXML private Button addBtn;

    private PlaylistService service;
    private HBox editRow;
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

    private void refreshList(){
        listBox.getChildren().clear();
        for(Playlist p: service.getPlaylists()){
            listBox.getChildren().add(playlistRow(p.getName()));
        }
    }

    private HBox playlistRow(String name){
        Label label = new Label(name);
        label.getStyleClass().add("row-label");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("row");
        return row;
    }

    @FXML
    private void onAddToggle(){
        // If and edit row is already open, the button canghes its state into a cancel button.
        // We close the edit module and reset the commitment flag to restore the normal focus behavior,
        // and exit the method to prevent opening a new input field.
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

        // Hides the error message dynamically as the user starts typing a valid name
        field.textProperty().addListener((obs, ov, nv) -> {
            if(tip.isVisible()){
                if(nv != null && !nv.trim().isEmpty()){
                    hideError(field, tip);
                }
            }
        });

        // Close the edit module if the user clicks away(not necessary on cancel button)
        // without typing anything.
        field.focusedProperty().addListener((obs, was, is) -> {
            if (!is && editRow != null && !committing && field.getText().isBlank()) {
                closeEdit();
            }
        });
    }

    private void confirm(TextField field, Label tip){
        String name = field.getText();
        Optional<String> error = service.createPlaylist(name);

        if(error.isPresent()){
            tip.setText(error.get());
            tip.setManaged(true);
            tip.setVisible(true);
            if(!field.getStyleClass().contains("error")){
                field.getStyleClass().add("error");
            }
            field.requestFocus();
            return;
        }

        committing = true;
        String created = name.trim();
        closeEdit();
        HBox row = playlistRow(created);
        listBox.getChildren().add(row);
        committing = false;
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
