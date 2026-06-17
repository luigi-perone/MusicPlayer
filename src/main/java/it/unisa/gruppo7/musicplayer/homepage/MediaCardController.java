package it.unisa.gruppo7.musicplayer.homepage;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Controller for a single media card shown on the home page.
 * Displays a title and subtitle and assigns a deterministic cover colour
 * derived from the title.
 *
 * @author francescoLemmo
 */

public class MediaCardController {

    @FXML private StackPane coverPane;
    @FXML private Label titleLabel;
    @FXML private Label subTitleLabel;

    /**
     * Populates the card with the given title and subtitle and assigns a
     * cover colour derived from the title.
     *
     * @param title    the main label of the card
     * @param subtitle the secondary label of the card
     */
    public void setData(String title, String subtitle) {
        titleLabel.setText(title);
        titleLabel.setTextFill(Color.WHITE);

        subTitleLabel.setText(subtitle);
        subTitleLabel.setTextFill(Color.web("#E0E0E0"));


        String hexColor = generateColorHash(title);


        coverPane.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 8;");
    }

    /**
     * Picks a stable cover colour for the given string from a fixed palette,
     * using the string's hash code so the same title always maps to the same colour.
     *
     * @param str the string used to select the colour (typically the card title)
     * @return the chosen colour as a hex string
     */
    private String generateColorHash(String str) {
        String[] colors = {
                "#3E9960", // vivid forest green
                "#3482B5", // ocean blue
                "#C24A66", // raspberry / cherry pink
                "#8954A8", // amethyst purple
                "#D45D33", // bright rust orange
                "#1A939E", // bright teal / dark turquoise
                "#CC8814", // dark mustard / gold
                "#4B619C"  // indigo blue
        };
        int index = Math.abs(str.hashCode()) % colors.length;
        return colors[index];
    }
}