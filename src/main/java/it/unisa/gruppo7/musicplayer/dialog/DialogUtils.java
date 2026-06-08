package it.unisa.gruppo7.musicplayer.dialog;

import javafx.scene.control.Alert;

/**
 * Utility helper class providing shorthand semantic wrappers to instantiate
 * and display standard JavaFX system notification alert dialog frames.
 *
 * @author Maxim Makhovskyy, Luigi Perone
 */
public final class DialogUtils {

    /**
     * Private constructor to enforce static utility container constraints.
     *
     * @throws UnsupportedOperationException If instantiation is attempted.
     */
    private DialogUtils() {
        throw new UnsupportedOperationException("Cannot be instantiated");
    }

    /**
     * Displays a blocking error alert window showing execution failure details.
     *
     * @param title   The text string showing on the window title strip.
     * @param message The detailed description text detailing the error context.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays a blocking info modal frame window outlining normal runtime parameters.
     *
     * @param title   The descriptive title label name string.
     * @param message The details notification string.
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays a blocking alert frame warning the user about anomalies.
     *
     * @param title   The system window title parameter label text.
     * @param message The primary textual details content.
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Renders an interactive binary option check confirmation dialog frame.
     * Returns true if the user pushes the explicit validation confirm button.
     *
     * @param title   The top frame validation title indicator.
     * @param header  The bold textual section explanation title layout.
     * @param message The inner informative context question description.
     * @return true if confirmed via OK click action, false otherwise.
     */
    public static boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        return alert.showAndWait().filter(result -> result == javafx.scene.control.ButtonType.OK).isPresent();
    }
}