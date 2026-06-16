package it.unisa.gruppo7.musicplayer.playlist;

import java.util.function.BiConsumer;
import java.util.function.Supplier; // Aggiunto import

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

    /**
     * Configures the columns, selection mode, and row factories of the table.
     *
     * @param playingTrackSupplier A supplier providing the track currently playing in real-time.
     * @param onDoubleClick        A callback executed when a row is double-clicked.
     */
    public void configure(Supplier<Track> playingTrackSupplier, BiConsumer<Integer, Track> onDoubleClick) {
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
        // Passa il supplier a buildRow
        table.setRowFactory(tv -> buildRow(playingTrackSupplier, onDoubleClick));
    }

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
     * Builds a custom TableRow to handle specific styling and mouse events.
     *
     * @param playingTrackSupplier The supplier for the track currently playing.
     * @param onDoubleClick        The callback to execute when the row is double-clicked.
     * @return A configured TableRow for the track.
     */
    private TableRow<Track> buildRow(Supplier<Track> playingTrackSupplier,
                                     BiConsumer<Integer, Track> onDoubleClick) {
        TableRow<Track> row = new TableRow<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);

                Track currentPlaying = playingTrackSupplier.get();

                setStyle((empty || item == null) ? "" :
                        item.equals(currentPlaying)
                        ? "-fx-background-color: #6498CCFF; -fx-font-weight: bold;"
                        : "");
            }
        };
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !row.isEmpty())
                onDoubleClick.accept(0, row.getItem());
        });
        return row;
    }
}