package it.unisa.gruppo7.musicplayer;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry point for the JavaFX Music Player application.
 * Manages the primary stage lifecycle, view transitions, and safe shutdown sequences.
 *
 */
public class App extends Application {

    private static Scene scene;

    /**
     * Starts the JavaFX application by initializing the main scene and displaying the primary stage.
     *
     * @param stage The primary stage for this application.
     * @throws IOException If the initial FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("mainView"), 1300, 700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Handles application shutdown cleanup, ensuring background playback and timer processes
     * are safely terminated.
     *
     * @throws Exception If an error occurs during shutdown.
     */
    @Override
    public void stop() throws Exception {

        // Shutdown the timer process
        MusicPlayerFacade.getInstance().shutdownPlayback();

        super.stop();
    }

    /**
     * Replaces the root node of the current scene to navigate to a different view.
     *
     * @param fxml The name of the FXML file (without the extension) to load.
     * @throws IOException If the FXML file cannot be loaded.
     */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Loads an FXML layout file and returns its root hierarchy parent node.
     *
     * @param fxml The name of the FXML file (without the extension) to load.
     * @return The root Parent node of the loaded FXML view.
     * @throws IOException If the FXML file cannot be found or loaded.
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * The main execution method used to launch the application runtime.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        launch();
    }

}