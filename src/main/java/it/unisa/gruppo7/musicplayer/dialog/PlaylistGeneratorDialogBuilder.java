package it.unisa.gruppo7.musicplayer.dialog;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unisa.gruppo7.musicplayer.playlist.strategy.TagCombinationMode;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
/**
 * Builder for the "smart playlist" generator dialog.
 * Assembles the dialog that lets the user choose a generation criterion
 * (genre, year or tags) and returns the choice as a {@link GenerationRequest}.
 *
 * @author francescoLemmo
 */
public class PlaylistGeneratorDialogBuilder implements DialogBuilder<PlaylistGeneratorDialogBuilder.GenerationRequest> {

    private Dialog<GenerationRequest> dialog;
    private ComboBox<String> criterionBox;
    private TextField targetField;
    private ButtonType generateButtonType;
    private TextField playlistNameField;
    private VBox tagOptions;
    private ToggleGroup combinationGroup;
    private final Map<TrackTag, CheckBox> tagCheckBoxes =
        new EnumMap<>(TrackTag.class);

    /**
     * Immutable value object carrying the user's playlist-generation choices
     * collected from the dialog.
     */
    public static class GenerationRequest {
        /** The chosen generation criterion (genre, year or tag). */
        public final String criterion;
        /** The target value for genre/year criteria. */
        public final String target;
        /** The name to give the generated playlist. */
        public final String playlistName;
        /** The tags selected for a tag-based criterion. */
        public final Set<TrackTag> selectedTags;
        /** How the selected tags must be combined. */
        public final TagCombinationMode combinationMode;

        /**
         * Creates a generation request.
         *
         * @param criterion       the chosen criterion (genre, year or tag)
         * @param target          the target value for genre/year criteria
         * @param playlistName    the name to give the generated playlist
         * @param selectedTags    the tags selected for a tag-based criterion
         * @param combinationMode how the selected tags must be combined
         */
        public GenerationRequest(String criterion, String target, String playlistName,
                                 Set<TrackTag> selectedTags, TagCombinationMode combinationMode) {
            this.criterion = criterion;
            this.target = target;
            this.playlistName = playlistName;
            this.selectedTags = selectedTags;
            this.combinationMode = combinationMode;
        }
    }

    /** Creates the builder and initializes the underlying dialog with its stylesheet. */
    public PlaylistGeneratorDialogBuilder() {
        this.dialog = new Dialog<>();

        // Connect to the CSS file
        try {
            String cssPath = getClass().getResource("/it/unisa/gruppo7/musicplayer/playlist/Playlist.css").toExternalForm();
            this.dialog.getDialogPane().getStylesheets().add(cssPath);

            // Optional: a general style class could be applied to the whole Dialog pane
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

VBox tagCheckBoxesContainer = new VBox(8);

for (TrackTag tag : TrackTag.values()) {
    CheckBox checkBox = new CheckBox(tag.getDisplayName());
    checkBox.setStyle("-fx-text-fill: #1c1c1e;");

    tagCheckBoxes.put(tag, checkBox);
    tagCheckBoxesContainer.getChildren().add(checkBox);
}

ScrollPane tagScrollPane = new ScrollPane(tagCheckBoxesContainer);
tagScrollPane.setFitToWidth(true);
tagScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
tagScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
tagScrollPane.setPrefViewportHeight(160);
tagScrollPane.setMaxHeight(160);

combinationGroup = new ToggleGroup();

RadioButton allTagsButton = new RadioButton("Tutti i tag");
allTagsButton.setUserData(TagCombinationMode.ALL);
allTagsButton.setToggleGroup(combinationGroup);
allTagsButton.setSelected(true);
allTagsButton.getStyleClass().add("tag-combination-radio");

RadioButton anyTagButton = new RadioButton("Almeno un tag");
anyTagButton.setUserData(TagCombinationMode.ANY);
anyTagButton.setToggleGroup(combinationGroup);
anyTagButton.getStyleClass().add("tag-combination-radio");

Label combinationLabel =
        new Label("Combina i tag selezionati:");

tagOptions = new VBox(
        8,
        tagScrollPane,
        combinationLabel,
        allTagsButton,
        anyTagButton
);
        playlistNameField = new TextField("Playlist Genere");
        playlistNameField.setPromptText("Nome playlist");
        playlistNameField.getStyleClass().add("playlist-input");
        Label playlistNameLabel = new Label("Nome playlist:");

        content.getChildren().addAll(
            criterionLabel,
            criterionBox,
            targetLabel,
            targetField,
            tagOptions,
            playlistNameLabel,
            playlistNameField
        );

Runnable updateTagControls = () -> {
    boolean tagSelected = "Tag".equals(criterionBox.getValue());

    targetLabel.setVisible(!tagSelected);
    targetLabel.setManaged(!tagSelected);
    targetField.setVisible(!tagSelected);
    targetField.setManaged(!tagSelected);
    tagOptions.setVisible(tagSelected);
    tagOptions.setManaged(tagSelected);
    playlistNameLabel.setVisible(tagSelected);
    playlistNameLabel.setManaged(tagSelected);
    playlistNameField.setVisible(tagSelected);
    playlistNameField.setManaged(tagSelected);

    if (tagSelected) {
        updateDefaultTagPlaylistName();
    } else {
        updateDefaultPlaylistName();
    }

    Platform.runLater(() -> {
        if (dialog.getDialogPane().getScene() != null
                && dialog.getDialogPane().getScene().getWindow() != null) {
            dialog.getDialogPane()
                    .getScene()
                    .getWindow()
                    .sizeToScene();
        }
    });
};

        Runnable updateCombinationControls = () -> {
        boolean hasSelectedTag = tagCheckBoxes.values().stream().anyMatch(CheckBox::isSelected);
            allTagsButton.setDisable(!hasSelectedTag);
            anyTagButton.setDisable(!hasSelectedTag);
            combinationLabel.setDisable(!hasSelectedTag);
        };

        criterionBox.valueProperty().addListener((obs, oldValue, newValue) -> updateTagControls.run());
        targetField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!"Tag".equals(criterionBox.getValue())) {
                updateDefaultPlaylistName();
            }
        });
        // The suggested name must follow the tag selection: a fixed name would make every
        // tag-based generation collide on the same playlist.
        tagCheckBoxes.values().forEach(checkBox ->checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            updateCombinationControls.run();
            updateDefaultTagPlaylistName();
        }));

        updateTagControls.run();
        updateCombinationControls.run();


        dialog.getDialogPane().setContent(content);
    }

    @Override
    public void buildResultConverter() {
        dialog.setResultConverter(button -> {
            if (button == generateButtonType) {
                Set<TrackTag> selectedTags = EnumSet.noneOf(TrackTag.class);

    tagCheckBoxes.forEach((tag, checkBox) -> {
    if (checkBox.isSelected()) {
        selectedTags.add(tag);
    }
    });

                Toggle selectedMode = combinationGroup.getSelectedToggle();
                TagCombinationMode combinationMode = selectedMode == null
                        ? TagCombinationMode.ALL
                        : (TagCombinationMode) selectedMode.getUserData();

                return new GenerationRequest(criterionBox.getValue(), targetField.getText().trim(),
                        playlistNameField.getText().trim(), selectedTags, combinationMode);

            }
            return null;
        });
    }

    @Override
    public Dialog<GenerationRequest> getResult() {
        return dialog;
    }

    /**
     * Updates the playlist name field with a default name derived from the
     * current criterion and target value.
     */
        private void updateDefaultPlaylistName() {
    String target = targetField.getText().trim();

    if (target.isEmpty()) {
        playlistNameField.setText(
                "Playlist " + criterionBox.getValue()
        );
        return;
    }

    String formattedTarget = target.substring(0, 1).toUpperCase()
            + target.substring(1).toLowerCase();

    playlistNameField.setText("Playlist " + formattedTarget);
    }

    /**
     * Updates the playlist name field with a default name listing the tags currently
     * selected, so that two different tag selections never propose the same name.
     * Tags are listed in enum declaration order, making the name independent of the
     * order in which the user ticked the checkboxes.
     */
    private void updateDefaultTagPlaylistName() {
        if (!"Tag".equals(criterionBox.getValue())) {
            return;
        }

        String tags = tagCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(entry -> entry.getKey().getDisplayName())
                .collect(Collectors.joining(", "));

        playlistNameField.setText(tags.isEmpty() ? "Playlist da tag" : "Playlist " + tags);
    }

}
