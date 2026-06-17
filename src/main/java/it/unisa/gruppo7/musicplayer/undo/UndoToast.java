package it.unisa.gruppo7.musicplayer.undo;

import it.unisa.gruppo7.musicplayer.command.CommandInvoker;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public final class UndoToast {
    private static final String ACCENT = "#1474ce";

    private static Popup currentPopup;
    private static Timeline currentTimeline;
    private static FadeTransition currentFade;
    private static Runnable currentOnUndo;
    private static Node bottomBar;

    private UndoToast(){}

    public static void show(Window owner, String message, Runnable onUndo) {
        if (owner == null) return;
        Platform.runLater(() -> display(owner, message, onUndo));
    }

    public static void hide() {
        if (currentTimeline != null) { currentTimeline.stop(); currentTimeline = null; }
        if (currentFade != null)     { currentFade.stop();     currentFade = null; }
        if (currentPopup != null)    { currentPopup.hide();    currentPopup = null; }
        currentOnUndo = null;
    }

    public static void triggerUndo() {
        if (currentPopup != null && currentOnUndo != null) {
            currentOnUndo.run();
            hide();
        }
    }

    private static void display(Window owner, String message, Runnable onUndo) {
        hide();

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #1c1c1c; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button undoButton = new Button("Annulla");
        undoButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT
                + "; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 8;");

        HBox row = new HBox(12, label, spacer, undoButton);
        row.setAlignment(Pos.CENTER_LEFT);

        Region barFill = new Region();
        barFill.setStyle("-fx-background-color: " + ACCENT + "; -fx-background-radius: 2;");
        barFill.setPrefHeight(4);
        barFill.setMaxWidth(Double.MAX_VALUE);
        Scale barScale = new Scale(1, 1, 0, 0); // pivot on the left edge so it shrinks leftward
        barFill.getTransforms().add(barScale);

        StackPane bar = new StackPane(barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        bar.setPrefHeight(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: rgba(0,0,0,0.10); -fx-background-radius: 2;");

        VBox card = new VBox(10, row, bar);
        card.setPadding(new Insets(14, 16, 12, 16));
        card.setPrefWidth(420);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: rgba(0,0,0,0.12); -fx-border-radius: 12;");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(18);
        shadow.setColor(Color.rgb(0, 0, 0, 0.25));
        card.setEffect(shadow);

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.getContent().add(card);
        popup.show(owner);

        reposition(owner, card, popup);
        Platform.runLater(() -> reposition(owner, card, popup));

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(barScale.xProperty(), 1.0)),
                new KeyFrame(ttl(), new KeyValue(barScale.xProperty(), 0.0))
        );
        timeline.setOnFinished(e -> hide());

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), card);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        undoButton.setOnAction(e -> {
            if (onUndo != null) onUndo.run();
            hide();
        });

        currentPopup    = popup;
        currentTimeline = timeline;
        currentFade     = fadeIn;
        currentOnUndo   = onUndo;

        fadeIn.play();
        timeline.play();
    }

    private static void reposition(Window owner, Region card, Popup popup) {
        double cardWidth  = card.getWidth()  > 0 ? card.getWidth()  : card.getPrefWidth();
        double cardHeight = card.getHeight() > 0 ? card.getHeight() : 64;

        double bottomInset = 48;
        if (bottomBar != null && bottomBar.getScene() != null) {
            double barHeight = bottomBar.getLayoutBounds().getHeight();
            if (barHeight > 0) {
                bottomInset = barHeight + 12; // sit just above the playback bar
            }
        }
        popup.setX(owner.getX() + (owner.getWidth() - cardWidth) / 2.0);
        popup.setY(owner.getY() + owner.getHeight() - cardHeight - bottomInset);
    }

    private static Duration ttl() {
        try {
            return Duration.millis(CommandInvoker.getUndoManager().getTtl().toMillis());
        } catch (Exception e) {
            return Duration.seconds(10);
        }
    }

    public static boolean isShowing() {
        return currentPopup != null;
    }

    public static void setBottomBar(Node bar) {
        bottomBar = bar;
    }
}
