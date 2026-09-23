package com.carry1.services;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalNotificationService {

    private static ScheduledExecutorService pollingService;
    private static int lastUnreadCount = 0;

    public static void start() {
        if (pollingService != null && !pollingService.isShutdown()) return;

        lastUnreadCount = 0; 
        pollingService = Executors.newSingleThreadScheduledExecutor();

        pollingService.scheduleAtFixedRate(() -> {
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser == null) return;

            
            LocalDatabaseManager.refreshCurrentUser();
            User updatedUser = LocalDatabaseManager.getCurrentUser();

            if (updatedUser != null && "BANNED".equals(updatedUser.getStatus())) {
                stop();
                LocalDatabaseManager.clearSession();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Account Suspended");
                    alert.setHeaderText("Session Terminated");
                    alert.setContentText("Your account has been BANNED by the Administrator. You have been logged out.");
                    alert.showAndWait();

                    SceneManager.switchScene("StartView.fxml", "Carry1 - Login");
                });
                return; 
            }

            
            if (updatedUser != null) {
                int currentUnreadCount = LocalDatabaseManager.getUnreadMessageCount(updatedUser.getId());

                if (currentUnreadCount > lastUnreadCount) {
                    showToastNotification("📩 You have a new message!");
                }
                lastUnreadCount = currentUnreadCount;
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (pollingService != null && !pollingService.isShutdown()) {
            pollingService.shutdownNow();
        }
    }

    private static void showToastNotification(String message) {
        Platform.runLater(() -> {
            Stage stage = SceneManager.getMainStage();
            if (stage == null) return;

            Popup popup = new Popup();
            Label label = new Label(message);
            label.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-padding: 15px 25px; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");

            popup.getContent().add(label);
            popup.setAutoHide(true);

            
            popup.show(stage, stage.getX() + stage.getWidth() - 350, stage.getY() + 80);

            
            new Thread(() -> {
                try { Thread.sleep(4000); } catch (InterruptedException e) {}
                Platform.runLater(popup::hide);
            }).start();
        });
    }
}