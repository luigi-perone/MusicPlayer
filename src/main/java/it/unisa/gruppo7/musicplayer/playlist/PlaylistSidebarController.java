package it.unisa.gruppo7.musicplayer.playlist;

import java.util.function.Consumer;

import it.unisa.gruppo7.musicplayer.MainController;
import it.unisa.gruppo7.musicplayer.command.Command;
import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import it.unisa.gruppo7.musicplayer.dialog.DialogDirector;
import it.unisa.gruppo7.musicplayer.dialog.DialogUtils;
import it.unisa.gruppo7.musicplayer.dialog.PlaylistGeneratorDialogBuilder;
import it.unisa.gruppo7.musicplayer.errorHandling.ErrorHandlingStrategy;
import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.playback.PlaybackState;
import it.unisa.gruppo7.musicplayer.playlist.command.CreatePlaylistCommand;
import it.unisa.gruppo7.musicplayer.playlist.strategy.GenreGenerationStrategy;
import it.unisa.gruppo7.musicplayer.playlist.strategy.PlaylistGenerationStrategy;
import it.unisa.gruppo7.musicplayer.playlist.strategy.TagGenerationStrategy;
import it.unisa.gruppo7.musicplayer.playlist.strategy.YearGenerationStrategy;
import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

/**
 * Manages the communication between the model and the view of the playlist's sidebar.
 * Handles displaying the list of available playlists and providing an inline interface
 * to create new playlists.
 * * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlaylistSidebarController {
    @FXML private VBox listBox;
    @FXML private Button addBtn;

    private PlaylistService service;
    private HBox editRow;
    private Consumer<Playlist> onPlaylistSelected;

    /// reference to the mainController, in order to use its methods (UI refresh)
    private MainController mainController;

    /**
     * Flag used to avoid the premature close of the edit module. It is set true when the user
     * clicks a UI button so that the TextField's focus-lost listener ignores the focus change.
     */
    private boolean committing;

    /**
     * Initializes the controller. Sets up event filters to manage focus state transitions
     * when the add button is pressed.
     */
    @FXML
    private void initialize() {
        addBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> committing = true);
    }

    @FXML
    private void onHomeClick() {
        if (mainController != null) {
            mainController.showHomePage();
        }
    }

    @FXML
    private void onLibraryClick() {
        if (mainController != null) {
            mainController.showLibrary();
        }
    }

    @FXML
    private void onPlaylistGeneratorToggle() {
        DialogDirector director = new DialogDirector();

        PlaylistGeneratorDialogBuilder builder = new PlaylistGeneratorDialogBuilder();
        Dialog<PlaylistGeneratorDialogBuilder.GenerationRequest> dialog = director.construct(builder);

        dialog.showAndWait().ifPresent(request -> {
            String criterion = request.criterion;
            String target = request.target;
            String defaultPlaylistName = request.playlistName;

            if (!"Tag".equals(criterion) && target.isEmpty()) {
                DialogUtils.showWarning("Dati mancanti", "Inserisci un valore per procedere.");
                return;
            }

            // Default playlist name

            if (defaultPlaylistName.isEmpty()) {
                DialogUtils.showWarning("Dati mancanti", "Il nome della playlist non può essere vuoto");
                return;
            }

            if ("Tag".equals(criterion) && request.selectedTags.isEmpty()) {
                DialogUtils.showWarning("Dati mancanti", "Seleziona almeno un tag");
                return;
            }

            // Selecting strategy
            PlaylistGenerationStrategy strategy = null;
            AutomaticPlaylistRule rule = new AutomaticPlaylistRule();

            try {
                switch (criterion) {
                    case "Genere":
                        strategy = new GenreGenerationStrategy(target);
                        rule.criterion = "GENRE";
                        rule.target = target;
                        break;
                    case "Anno":
                        strategy = new YearGenerationStrategy(Integer.parseInt(target));
                        rule.criterion = "YEAR";
                        rule.target = target;
                        break;
                    case "Tag":
                        strategy = new TagGenerationStrategy(request.selectedTags, request.combinationMode);
                        rule.criterion = "TAG";
                        rule.tags = request.selectedTags;
                        rule.combinationMode =
                        request.combinationMode;
                        break;
                    default: 
                        throw new IllegalArgumentException("Criterio non supportato");
                }

                // Execution from facade
                MusicPlayerFacade.getInstance().createAutoPlaylist(defaultPlaylistName, strategy, rule);

                DialogUtils.showInfo("Completato", "La '" + defaultPlaylistName + "' è stata creata con successo!");
                refreshList();

            } catch (NumberFormatException e) {
                DialogUtils.showWarning("Errore di formato", "L'anno deve essere un numero valido.");
            } catch (IllegalArgumentException e) {
                String errorMessage = e.getMessage();

                if (errorMessage != null && errorMessage.equals("Nessuna traccia trovata")) {
                    DialogUtils.showWarning(
                            "Creazione Annullata",
                            "Tag".equals(criterion)
                                    ? "Nessun brano corrisponde alla combinazione di tag selezionata"
                                    : "Nessun brano trovato per questo " + criterion.toLowerCase() + " nella tua libreria."
                    );
                }else if (errorMessage != null && errorMessage.equals("Esiste già una playlist con questo nome")) {
                    DialogUtils.showWarning(
                            "Creazione Annullata",
                            "La playlist '" + defaultPlaylistName + "' esiste già."
                    );
                } else {
                    DialogUtils.showWarning(
                            "Impossibile creare la playlist",
                            errorMessage
                    );
                }
            }
        });
    }


    /**
     * Sets the playlist service injection and refreshes the sidebar list view.
     *
     * @param service The playlist service containing backend data logic.
     */
    public void setPlaylistService(PlaylistService service){
        this.service = service;
        refreshList();
    }

    /**
     * Sets the callback listener that triggers when a playlist is selected from the sidebar.
     *
     * @param listener The consumer callback action accepting the selected Playlist.
     */
    public void setOnPlaylistSelected(Consumer<Playlist> listener) {
        this.onPlaylistSelected = listener;
    }

    /**
     * Clears the current list display and repopulates it with playlists fetched from the service.
     */
    public void refreshList(){
        listBox.getChildren().clear();
        for(Playlist p: service.getPlaylists()){
            listBox.getChildren().add(playlistRow(p));
        }
    }

    /**
     * Creates a graphical HBox row representing a single playlist in the sidebar.
     *
     * @param playlist The playlist data model to bind to this row.
     * @return A configured HBox container acting as the visual row.
     */
    private HBox playlistRow(Playlist playlist){
        Label label = new Label(playlist.getName());
        label.getStyleClass().add("row-label");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("row");

        ContextMenu contextMenu = new ContextMenu();

        MenuItem playItem = new MenuItem("Riproduci");
        playItem.setOnAction(e -> {
            try {
                MusicPlayerFacade.getInstance().playFromPlaylist(playlist);
            } catch (IllegalArgumentException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText(null);
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });

        MenuItem appendItem = new MenuItem("Aggiungi a coda");
        appendItem.setOnAction(e -> {
            // Add the playlist to the queue
            if (playlist.getTracks().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText(null);
                alert.setContentText("La playlist non contiene brani.");
                alert.showAndWait();
                return;
            }
            MusicPlayerFacade.getInstance().appendPlaylistToQueue(playlist);

            // Update the queue UI
            if (mainController != null) {
                mainController.refreshQueueView();
            }

            PlaybackState playbackState = MusicPlayerFacade.getInstance().getPlaybackState();
            if (playbackState == PlaybackState.STOPPED || playbackState == PlaybackState.START_UP) {
                MusicPlayerFacade.getInstance().playFromQueue(playlist.getPlaylist().get(0));
            }

        });

        contextMenu.getItems().addAll(playItem, appendItem);

        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                contextMenu.hide();
                if (onPlaylistSelected != null) {
                    onPlaylistSelected.accept(playlist);
                }
            }
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });

        return row;
    }

    /**
     * Handles clicking the add/cancel button to toggle the inline creation text field form.
     */
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

    /**
     * Confirms the playlist creation process by executing the corresponding command.
     * Handles inline error rendering if creation validation fails.
     *
     * @param field The text input field containing the playlist name.
     * @param tip   The feedback label used to display errors.
     */
    private void confirm(TextField field, Label tip){
        String name = field.getText();
        Command<Playlist> createCommand = new CreatePlaylistCommand(service, name);
        ErrorHandlingStrategy inlineStrategy = e -> {
            tip.setText(e.getMessage());
            tip.setManaged(true);
            tip.setVisible(true);
            if(!field.getStyleClass().contains("error")){
                field.getStyleClass().add("error");
            }
            field.requestFocus();
        };

        CommandInvoker.execute(createCommand, inlineStrategy).ifPresent(p -> {
            committing = true;
            closeEdit();

            HBox row = playlistRow(p);
            listBox.getChildren().add(row);
            if (onPlaylistSelected != null) {
                onPlaylistSelected.accept(p);
            }
            committing = false;
        });
    }

    /**
     * Clears validation error messages and restores the text field style.
     *
     * @param field The target text field.
     * @param tip   The target error feedback label.
     */
    private void hideError(TextField field, Label tip) {
        tip.setVisible(false);
        tip.setManaged(false);
        field.getStyleClass().remove("error");
    }

    /**
     * Closes the inline creation form and resets the add button visual appearance.
     */
    private void closeEdit(){
        if(editRow == null) return;
        Node wrap = editRow.getParent();
        listBox.getChildren().remove(wrap);
        editRow = null;

        addBtn.setText("+");
        addBtn.getStyleClass().remove("cancel-btn");
    }

    /**
     * Generates a structural placeholder circle indicator showing the first letter of
     * the entered text, dynamically responding to text changes.
     *
     * @param field The text field to bind for letter extraction.
     * @return A StackPane displaying the placeholder dot.
     */
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

    /**
     * Instantiates the validation button for the creation row form.
     *
     * @param field The text input field to bind.
     * @param tip   The error display label to bind.
     * @return A configured checkmark validation Button.
     */
    private Button confirmBtn(TextField field, Label tip) {
        Button b = new Button("\u2713");
        b.getStyleClass().add("confirm-btn");
        b.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> committing = true);
        b.setOnAction(e -> { confirm(field, tip); committing = false; });
        return b;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

}