package it.unisa.gruppo7.musicplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import it.unisa.gruppo7.musicplayer.playlist.PlaylistService;
import it.unisa.gruppo7.musicplayer.playlist.PlaylistSidebarController;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        java.net.URL url = App.class.getResource("/it/unisa/gruppo7/musicplayer/playlist/PlaylistSidebar.fxml");
        System.out.println("FXML URL: " + url);
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        PlaylistSidebarController controller = loader.getController();
        controller.setPlaylistService(new PlaylistService());

        scene = new Scene(root, 234, 600);
        scene.getStylesheets().add(
            App.class.getResource("/it/unisa/gruppo7/musicplayer/playlist/Playlist.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}