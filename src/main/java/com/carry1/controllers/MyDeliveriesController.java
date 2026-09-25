package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Message;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyDeliveriesController {

    @FXML private ListView<Order> deliveriesListView;

    @FXML private VBox chatOverlayPane;
    @FXML private Label chatHeaderLabel, messageAlertLabel;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInputField;

    private ScheduledExecutorService pollingService;
    private String currentChatOrderId = null;
    private String currentChatReceiverId = null;

    @FXML
    public void initialize() {
        setupListView();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser != null) {
                List<Order> myDeliveries = LocalDatabaseManager.getOrdersByTravelerId(currentUser.getId());
                Platform.runLater(() -> {
                    if (deliveriesListView != null) {
                        deliveriesListView.setItems(FXCollections.observableArrayList(myDeliveries));
                    }
                });
                
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupListView() {
        deliveriesListView.setCellFactory(param -> new ListCell<Order>() {
            private final HBox root = new HBox(15);
            private final VBox detailsBox = new VBox(5);
            private final Label idLabel = new Label();
            private final Label itemLabel = new Label();
            private final Label routeLabel = new Label();
            private final Label rewardLabel = new Label();
            private final Label statusLabel = new Label();
            private final Button actionBtn = new Button();
            private final Button chatBtn = new Button("Chat");
            private final VBox actionBox = new VBox(5, actionBtn, chatBtn);

            {
                root.setAlignment(Pos.CENTER_LEFT);
                root.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
                
                idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                itemLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 14px;");
                routeLabel.setStyle("-fx-text-fill: #777; -fx-font-size: 12px;");
                rewardLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
                
                statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");

                actionBox.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(detailsBox, Priority.ALWAYS);

                detailsBox.getChildren().addAll(idLabel, itemLabel, routeLabel, rewardLabel);
                root.getChildren().addAll(detailsBox, statusLabel, actionBox);

                actionBtn.setOnAction(event -> {
                    Order order = getItem();
                    if (order != null) handleOrderAction(order, actionBtn);
                });
                
                chatBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
                chatBtn.setOnAction(event -> {
                    Order order = getItem();
                    if (order != null) openChat(order);
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
                    if (order.getStatus() == OrderStatus.DELIVERED) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #4CAF50;");
                    } else if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #FFC107;");
                    } else if (order.getStatus() == OrderStatus.CANCELLED) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #F44336;");
                    } else {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #2196F3;");
                    }

                    actionBox.getChildren().clear();
                    if (order.getStatus() == OrderStatus.PICKED_UP) {
                        actionBtn.setText("Req. Confirm"); actionBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;"); actionBtn.setDisable(false);
                        chatBtn.setVisible(true);
                        actionBox.getChildren().addAll(actionBtn, chatBtn);
                    } else if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        actionBtn.setText("Waiting..."); actionBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;"); actionBtn.setDisable(true);
                        actionBox.getChildren().add(actionBtn);
                    } else if (order.getStatus() == OrderStatus.DELIVERED) {
                        actionBtn.setText("Completed"); actionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); actionBtn.setDisable(true);
                        actionBox.getChildren().add(actionBtn);
                    }
                    
                    setGraphic(root);
                }
            }
        });
    }

    private void openChat(Order order) {
        currentChatOrderId = order.getOrderId();
        currentChatReceiverId = order.getCustomerId();
        chatHeaderLabel.setText("Chat - " + order.getOrderId());
        chatOverlayPane.setVisible(true);
        refreshChatMessagesInBackground(LocalDatabaseManager.getCurrentUser().getId());
    }

    @FXML
    private void closeChat() {
        chatOverlayPane.setVisible(false);
        currentChatOrderId = null;
        currentChatReceiverId = null;
    }

    @FXML
    private void sendChatMessage() {
        String text = chatInputField.getText().trim();
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (text.isEmpty() || currentChatOrderId == null || currentUser == null) return;

        new Thread(() -> {
            LocalDatabaseManager.sendMessage(currentChatOrderId, currentUser.getId(), currentChatReceiverId, text);
            Platform.runLater(() -> {
                chatInputField.clear();
                refreshChatMessagesInBackground(currentUser.getId());
            });
        }).start();
    }

    private void refreshChatMessagesInBackground(String currentUserId) {
        List<Message> msgs = LocalDatabaseManager.getOrderMessages(currentChatOrderId);
        List<String> displayMsgs = msgs.stream().map(m -> (m.getSenderId().equals(currentUserId) ? "You: " : "Customer: ") + m.getText()).toList();
        Platform.runLater(() -> {
            chatListView.setItems(FXCollections.observableArrayList(displayMsgs));
            chatListView.scrollTo(displayMsgs.size() - 1);
        });
        LocalDatabaseManager.markMessagesAsReadForOrder(currentChatOrderId, currentUserId);
    }

    private void handleOrderAction(Order order, Button btn) {
        if (order.getStatus() == OrderStatus.PICKED_UP) {
            btn.setText("Sending..."); btn.setDisable(true);
            new Thread(() -> {
                boolean success = LocalDatabaseManager.updateOrderStatus(order.getOrderId(), OrderStatus.AWAITING_CONFIRMATION);
                Platform.runLater(() -> { if (!success) btn.setDisable(false); });
            }).start();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (pollingService != null) pollingService.shutdownNow();
        SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
    }
}