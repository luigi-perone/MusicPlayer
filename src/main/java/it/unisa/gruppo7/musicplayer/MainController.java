package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.library.LibraryController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistSidebarController;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistService; // Importa il servizio
import javafx.fxml.FXML;

public class MainController {

    @FXML private PlaylistSidebarController playlistSidebarController;
    @FXML private LibraryController libraryController; // Corretto il nome in base a fx:id="library"

    @FXML
    public void initialize() {
        // 1. Inizializza il servizio (o passalo tramite costruttore/dipendenze se lo crei altrove)
        PlaylistService playlistService = new PlaylistService();

        // 2. Passa il servizio alla sidebar
        if (playlistSidebarController != null) {
            playlistSidebarController.setPlaylistService(playlistService);
        }
    }
}