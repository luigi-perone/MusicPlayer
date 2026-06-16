package it.unisa.gruppo7.musicplayer.homepage;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * @author francescoLemmo
 */

public class MediaCardController {

    @FXML private StackPane coverPane;
    @FXML private Label titleLabel;
    @FXML private Label subTitleLabel;

    public void setData(String title, String subtitle) {
        titleLabel.setText(title);
        titleLabel.setTextFill(Color.WHITE);

        subTitleLabel.setText(subtitle);
        subTitleLabel.setTextFill(Color.web("#E0E0E0"));


        String hexColor = generateColorHash(title);


        coverPane.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 8;");
    }

    private String generateColorHash(String str) {
        String[] colors = {
                "#3E9960", // Verde bosco vivace
                "#3482B5", // Blu oceano
                "#C24A66", // Rosa lampone / Ciliegia
                "#8954A8", // Viola ametista
                "#D45D33", // Arancio ruggine acceso
                "#1A939E", // Ottanio brillante / Turchese scuro
                "#CC8814", // Senape scuro / Oro
                "#4B619C"  // Blu indaco
        };
        int index = Math.abs(str.hashCode()) % colors.length;
        return colors[index];
    }
}