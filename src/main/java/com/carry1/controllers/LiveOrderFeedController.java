package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiveOrderFeedController {

    @FXML private Label warningLabel;
    @FXML private TableView<Order> feedTable;
    @FXML private TableColumn<Order, String> colOrderId, colItemName, colPickup, colDropoff;
    @FXML private TableColumn<Order, String> colDeliveryFee, colCharge, colTotalAmount;
    @FXML private TableColumn<Order, Void> colAction;

    private ScheduledExecutorService pollingService;

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colPickup.setCellValueFactory(new PropertyValueFactory<>("pickupLocation"));
        colDropoff.setCellValueFactory(new PropertyValueFactory<>("dropoffLocation"));

        colDeliveryFee.setCellValueFactory(param -> new SimpleStringProperty(String.format("%.2f", param.getValue().getRewardAmount())));
        colCharge.setCellValueFactory(param -> new SimpleStringProperty(String.format("%.2f", param.getValue().getRewardAmount() * 0.05)));
        colTotalAmount.setCellValueFactory(param -> new SimpleStringProperty(String.format("%.2f", param.getValue().getRewardAmount() * 0.95)));

        setupActionColumn();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            List<Order> pendingOrders = LocalDatabaseManager.getPendingOrders();
            Platform.runLater(() -> feedTable.setItems(FXCollections.observableArrayList(pendingOrders)));
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accept");
            {
                acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                acceptBtn.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    handleAcceptOrder(order, acceptBtn);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(acceptBtn);
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
                    feedTable.getItems().remove(order);
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