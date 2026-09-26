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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyDeliveriesController {

    @FXML private TableView<Order> deliveriesTable;
    @FXML private TableColumn<Order, String> colOrderId, colItemName, colPickup, colDropoff, colStatus;
    @FXML private TableColumn<Order, Double> colReward;
    @FXML private TableColumn<Order, Void> colAction, colChat;

    @FXML private VBox chatOverlayPane;
    @FXML private Label chatHeaderLabel, messageAlertLabel;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInputField;

    private ScheduledExecutorService pollingService;
    private String currentChatOrderId = null;
    private String currentChatReceiverId = null;

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colPickup.setCellValueFactory(new PropertyValueFactory<>("pickupLocation"));
        colDropoff.setCellValueFactory(new PropertyValueFactory<>("dropoffLocation"));
        colReward.setCellValueFactory(new PropertyValueFactory<>("rewardAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupActionColumn();
        setupChatColumn();
        startAutoRefresh();
        chatOverlayPane.setVisible(false);
    }

    private void startAutoRefresh() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser != null) {
                List<Order> myDeliveries = LocalDatabaseManager.getOrdersByTravelerId(currentUser.getId());
                boolean hasNewMessage = LocalDatabaseManager.hasUnreadMessages(currentUser.getId());

                Platform.runLater(() -> {
                    deliveriesTable.setItems(FXCollections.observableArrayList(myDeliveries));
                    messageAlertLabel.setVisible(hasNewMessage);
                });

                if (chatOverlayPane.isVisible() && currentChatOrderId != null) {
                    refreshChatMessagesInBackground(currentUser.getId());
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button actionBtn = new Button();
            {
                actionBtn.setOnAction(event -> handleOrderAction(getTableView().getItems().get(getIndex()), actionBtn));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == OrderStatus.PICKED_UP) {
                        actionBtn.setText("Req. Confirm"); actionBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;"); actionBtn.setDisable(false); setGraphic(actionBtn);
                    } else if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        actionBtn.setText("Waiting..."); actionBtn.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;"); actionBtn.setDisable(true); setGraphic(actionBtn);
                    } else if (order.getStatus() == OrderStatus.DELIVERED) {
                        actionBtn.setText("Completed"); actionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); actionBtn.setDisable(true); setGraphic(actionBtn);
                    } else setGraphic(null);
                }
            }
        });
    }

    private void setupChatColumn() {
        colChat.setCellFactory(param -> new TableCell<>() {
            private final Button chatBtn = new Button("Chat");
            {
                chatBtn.setOnAction(event -> openChat(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == OrderStatus.PICKED_UP) {
                        chatBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
                        setGraphic(chatBtn);
                    } else setGraphic(null);
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