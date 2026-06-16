package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
/**
 * @author francescoLemmo
 */
public class PlaylistGeneratorDialogBuilder implements DialogBuilder<PlaylistGeneratorDialogBuilder.GenerationRequest> {

    private Dialog<GenerationRequest> dialog;
    private ComboBox<String> criterionBox;
    private TextField targetField;
    private ButtonType generateButtonType;

    public static class GenerationRequest {
        public final String criterion;
        public final String target;

        public GenerationRequest(String criterion, String target) {
            this.criterion = criterion;
            this.target = target;
        }
    }

    public PlaylistGeneratorDialogBuilder() {
        this.dialog = new Dialog<>();

        // Connect to the CSS file
        try {
            String cssPath = getClass().getResource("/it/unisa/gruppo7/musicplayer/playlist/Playlist.css").toExternalForm();
            this.dialog.getDialogPane().getStylesheets().add(cssPath);

            // Opzionale: puoi dare una classe generale a tutto il pannello del Dialog
            //this.dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");
        } catch (NullPointerException e) {
            System.err.println("Attenzione: File CSS non trovato per il Dialog.");
        }
    }

    @Override
    public void buildHeader() {
        dialog.setTitle("Genera Playlist Intelligente");
        dialog.setHeaderText("Crea automaticamente una playlist filtrando la libreria.");
    }

    @Override
    public void buildButtons() {
        generateButtonType = new ButtonType("Genera", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);
    }

    @Override
    public void buildContent() {
        VBox content = new VBox(15);

        criterionBox = new ComboBox<>();
        criterionBox.getItems().addAll("Genere", "Anno", "Tag");
        criterionBox.getSelectionModel().selectFirst();
        criterionBox.setMaxWidth(Double.MAX_VALUE);
        criterionBox.getStyleClass().add("custom-combo-box");

        targetField = new TextField();
        targetField.setPromptText("Es. Rock, Pop, 2023...");
        targetField.getStyleClass().add("playlist-input");

        Label criterionLabel = new Label("Scegli il criterio di generazione:");
        Label targetLabel = new Label("Valore da cercare:");

        content.getChildren().addAll(criterionLabel, criterionBox, targetLabel, targetField);

        dialog.getDialogPane().setContent(content);
    }

    @Override
    public void buildResultConverter() {
        dialog.setResultConverter(button -> {
            if (button == generateButtonType) {
                return new GenerationRequest(criterionBox.getValue(), targetField.getText().trim());
            }
            return null;
        });
    }

    @Override
    public Dialog<GenerationRequest> getResult() {
        return dialog;
    }

}
