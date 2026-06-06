package it.unisa.gruppo7.musicplayer.playlist;

import java.util.function.BiConsumer;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import it.unisa.gruppo7.musicplayer.track.Track;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class PlaylistTableConfigurator {

    private final TableView<Track>           table;
    private final TableColumn<Track, String> titleColumn;
    private final TableColumn<Track, String> authorColumn;
    private final TableColumn<Track, String> durationColumn;
    private final TableColumn<Track, Void>   indexColumn;
    private final MusicPlayerFacade          facade;

    public PlaylistTableConfigurator(TableView<Track> table,
                                     TableColumn<Track, String> titleColumn,
                                     TableColumn<Track, String> authorColumn,
                                     TableColumn<Track, String> durationColumn,
                                     TableColumn<Track, Void>   indexColumn,
                                     MusicPlayerFacade          facade) {
        this.table          = table;
        this.titleColumn    = titleColumn;
        this.authorColumn   = authorColumn;
        this.durationColumn = durationColumn;
        this.indexColumn    = indexColumn;
        this.facade         = facade;
    }

    public void configure(Track playingTrack, BiConsumer<Integer, Track> onDoubleClick) {
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
            try {
                String formatted = facade.formatDuration(track.getDuration());
                return new SimpleStringProperty(formatted);
            } catch (IllegalStateException e) {
                return new SimpleStringProperty("");
            }
        });

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setRowFactory(tv -> buildRow(playingTrack, onDoubleClick));
    }

    private TableRow<Track> buildRow(Track playingTrack,
                                     BiConsumer<Integer, Track> onDoubleClick) {
        TableRow<Track> row = new TableRow<Track>() {
            @Override
            protected void updateItem(Track item, boolean empty) {
                super.updateItem(item, empty);
                setStyle((empty || item == null) ? "" :
                    item.equals(playingTrack)
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
