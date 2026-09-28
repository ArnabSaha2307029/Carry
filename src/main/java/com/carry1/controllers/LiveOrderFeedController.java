package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.User;
import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;



import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiveOrderFeedController {

    @FXML private Label warningLabel;
    @FXML private ListView<Order> feedListView;

    private ScheduledExecutorService pollingService;

    @FXML
    public void initialize() {
        setupListView();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            List<Order> pendingOrders = LocalDatabaseManager.getPendingOrders();
            Platform.runLater(() -> {
                if (feedListView != null) {
                    feedListView.setItems(FXCollections.observableArrayList(pendingOrders));
                }
            });
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupListView() {
        feedListView.setCellFactory(param -> new ListCell<Order>() {
            private final HBox root = new HBox(15);
            private final VBox detailsBox = new VBox(5);
            private final Label idLabel = new Label();
            private final Label itemLabel = new Label();
            private final Label routeLabel = new Label();
            private final Label rewardLabel = new Label();
            private final Label statusLabel = new Label();
            private final Button acceptBtn = new Button("Accept");
            private final VBox actionBox = new VBox(5, acceptBtn);

            {
                root.setAlignment(Pos.CENTER_LEFT);
                root.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
                
                idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                itemLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                routeLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");
                rewardLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                
                statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #FFC107;");

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(detailsBox, Priority.ALWAYS);

                detailsBox.getChildren().addAll(idLabel, itemLabel, routeLabel, rewardLabel);
                root.getChildren().addAll(detailsBox, statusLabel, actionBox);

                acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                acceptBtn.setOnAction(event -> {
                    Order order = getItem();
                    if (order != null) handleAcceptOrder(order, acceptBtn);
                });
            }

            @Override
            protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    idLabel.setText("Order: " + order.getOrderId());
                    itemLabel.setText("Item: " + order.getItemName());
                    routeLabel.setText(order.getPickupLocation() + " -> " + order.getDropoffLocation());
                    rewardLabel.setText("Reward: " + String.format("%.2f TK", order.getRewardAmount() * 0.95));
                    statusLabel.setText(order.getStatus().toString());
                    setGraphic(root);
                }
            }
        });
    }

    private void handleAcceptOrder(Order order, Button btn) {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser == null) return;

        btn.setText("Accepting...");
        btn.setDisable(true);

        new Thread(() -> {
            
            LocalDatabaseManager.refreshCurrentUser();
            User refreshedUser = LocalDatabaseManager.getCurrentUser();

            
            if (refreshedUser != null && refreshedUser.getBalance() < 50.0) {
                Platform.runLater(() -> {
                    btn.setText("Accept");
                    btn.setDisable(false);
                    warningLabel.setText("Security Deposit Required: You must have at least 50 TK in your account to accept an order.");
                    warningLabel.setVisible(true);
                    warningLabel.setManaged(true);
                });
                return;
            }

            boolean success = LocalDatabaseManager.acceptOrder(order.getOrderId(), refreshedUser.getId());
            Platform.runLater(() -> {
                if (success) {
                    warningLabel.setVisible(false);
                    warningLabel.setManaged(false);
                    feedListView.getItems().remove(order);
                } else {
                    btn.setText("Accept");
                    btn.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (pollingService != null) pollingService.shutdownNow();
        SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
    }
}