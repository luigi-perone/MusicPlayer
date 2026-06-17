package it.unisa.gruppo7.musicplayer.playlist;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;
import it.unisa.gruppo7.musicplayer.track.TrackTag;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

/**
 * Configures the JavaFX TableView used to display a playlist of tracks.
 * It handles setting up column cell value factories, custom formatting (like row indexes and duration),
 * and row-level interactions and styling.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public class PlaylistTableConfigurator {

    private final TableView<Track>           table;
    private final TableColumn<Track, String> titleColumn;
    private final TableColumn<Track, String> authorColumn;
    private final TableColumn<Track, String> durationColumn;
    private final TableColumn<Track, String> tagColumn;
    private final TableColumn<Track, Void>   indexColumn;
    private final MusicPlayerFacade          facade;

    /**
     * Creates a configurator bound to the given table and its columns.
     *
     * @param table          the table view displaying the playlist tracks
     * @param titleColumn    the column showing the track title
     * @param authorColumn   the column showing the track author
     * @param durationColumn the column showing the formatted track duration
     * @param tagColumn      the column showing the track tags
     * @param indexColumn    the column showing the row index
     * @param facade         the facade used to format durations
     */
    public PlaylistTableConfigurator(TableView<Track> table,
                                     TableColumn<Track, String> titleColumn,
                                     TableColumn<Track, String> authorColumn,
                                     TableColumn<Track, String> durationColumn,
                                     TableColumn<Track, String> tagColumn,
                                     TableColumn<Track, Void>   indexColumn,
                                     MusicPlayerFacade          facade) {
        this.table          = table;
        this.titleColumn    = titleColumn;
        this.authorColumn   = authorColumn;
        this.durationColumn = durationColumn;
        this.tagColumn      = tagColumn;
        this.indexColumn    = indexColumn;
        this.facade         = facade;
    }

    /** Identifier for the drag-and-drop gesture used to reorder rows (US-027). */
    private static final String REORDER_DRAG_TOKEN = "playlist-reorder:";

    /**
     * Configures the columns, selection mode, and row factories of the table.
     *
     * @param playingTrackSupplier A supplier providing the track currently playing in real-time.
     * @param onDoubleClick        A callback executed when a row is double-clicked.
     * @param onReorder            A callback executed when a row is dragged to a new position,
     *                             receiving the source and target indices (from, to).
     */
    public void configure(Supplier<Track> playingTrackSupplier,
                          BiConsumer<Integer, Track> onDoubleClick,
                          BiConsumer<Integer, Integer> onReorder) {
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
        tagColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatTags(cellData.getValue())));
        durationColumn.setCellValueFactory(cellData -> {
            Track track = cellData.getValue();
            try {
                String formatted = facade.formatDuration(track.getDuration());
                return new SimpleStringProperty(formatted);
            } catch (IllegalStateException e) {
                return new SimpleStringProperty("");
            }
        });

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // Pass the supplier to buildRow
        table.setRowFactory(tv -> buildRow(playingTrackSupplier, onDoubleClick, onReorder));
    }

    /**
     * Builds the comma-separated short labels of the tags assigned to a track.
     *
     * @param track the track whose tags are formatted (may be {@code null})
     * @return the joined short labels, or an empty string when there are none
     */
    private String formatTags(Track track) {
        if (track == null || track.getTags().isEmpty()) {
            return "";
        }

        java.util.List<String> labels = new java.util.ArrayList<>();
        for (TrackTag tag : TrackTag.values()) {
            if (track.hasTag(tag)) {
                labels.add(tag.getShortLabel());
            }
        }
        return String.join(", ", labels);
    }    

    /**
     * Builds a custom TableRow to handle specific styling, mouse events and
     * drag-and-drop reordering.
     *
     * @param playingTrackSupplier The supplier for the track currently playing.
     * @param onDoubleClick        The callback to execute when the row is double-clicked.
     * @param onReorder            The callback to execute when the row is dropped at a new position.
     * @return A configured TableRow for the track.
     */
    private TableRow<Track> buildRow(Supplier<Track> playingTrackSupplier,
                                     BiConsumer<Integer, Track> onDoubleClick,
                                     BiConsumer<Integer, Integer> onReorder) {
        TableRow<Track> row = new TableRow<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                applyPlayingStyle(this, item, empty, playingTrackSupplier);
            }
        };
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !row.isEmpty())
                onDoubleClick.accept(0, row.getItem());
        });

        configureDragAndDrop(row, playingTrackSupplier, onReorder);
        return row;
    }

    /**
     * Highlights the currently playing track via the {@code playing-row} CSS class,
     * keeping the styling centralized in the stylesheet. Also clears any leftover
     * inline style (e.g. the drag-over border) so rows return to their resting look.
     *
     * @param row                  The row to style.
     * @param item                 The track displayed in the row (may be null).
     * @param empty                Whether the row is empty.
     * @param playingTrackSupplier Supplier of the track currently playing.
     */
    private void applyPlayingStyle(TableRow<Track> row, Track item, boolean empty,
                                   Supplier<Track> playingTrackSupplier) {
        row.setStyle(""); // drop any inline drag-over border
        row.getStyleClass().remove("playing-row");
        if (!empty && item != null && item.equals(playingTrackSupplier.get())) {
            row.getStyleClass().add("playing-row");
        }
    }

    /**
     * Wires drag-and-drop reordering handlers on a table row (US-027).
     * The drag carries the source row index; on drop the source and target indices
     * are forwarded to {@code onReorder}, which performs the actual move (model,
     * UI list and playback-queue sync) in a single controlled path.
     *
     * @param row                  The row to make draggable.
     * @param playingTrackSupplier Supplier used to restore row styling after a drag.
     * @param onReorder            The callback receiving (from, to) indices on a successful drop.
     */
    private void configureDragAndDrop(TableRow<Track> row,
                                      Supplier<Track> playingTrackSupplier,
                                      BiConsumer<Integer, Integer> onReorder) {
        row.setOnDragDetected(event -> {
            if (row.isEmpty()) return;
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(REORDER_DRAG_TOKEN + row.getIndex());
            db.setContent(content);
            event.consume();
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (isReorderDrag(db)) {
                event.acceptTransferModes(TransferMode.MOVE);
                row.setStyle("-fx-border-color: #e0592b; -fx-border-width: 2 0 0 0;");
                event.consume();
            }
        });

        row.setOnDragExited(event -> {
            // Restore styling (e.g. playing-track highlight) after the drag-over border.
            applyPlayingStyle(row, row.getItem(), row.isEmpty(), playingTrackSupplier);
        });

        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (!isReorderDrag(db)) return;

            int from = Integer.parseInt(db.getString().substring(REORDER_DRAG_TOKEN.length()));
            int size = table.getItems().size();
            // Dropping on an empty row (below the last item) means "move to the end".
            int to = row.isEmpty() ? size - 1 : row.getIndex();

            if (from != to) {
                onReorder.accept(from, to);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    /**
     * @return true if the dragboard carries a playlist-reorder gesture.
     */
    private boolean isReorderDrag(Dragboard db) {
        return db.hasString() && db.getString().startsWith(REORDER_DRAG_TOKEN);
    }
}