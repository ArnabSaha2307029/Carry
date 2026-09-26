package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Message;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import com.carry1.services.AiSupportService;
import com.carry1.services.GlobalNotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TravelerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private VBox chatBox, inboxListPane, inboxChatPane;
    @FXML private StackPane inboxOverlayPane;
    @FXML private ListView<String> chatListView, inboxListView, inboxChatListView;
    @FXML private TextField chatInputField, inboxChatInputField;
    @FXML private Label inboxChatHeader;

    private ScheduledExecutorService balancePoller;

    private Map<String, Order> activeOrdersMap = new HashMap<>();
    private String currentInboxOrderId = null;
    private String currentInboxReceiverId = null;

    @FXML
    public void initialize() {
        inboxOverlayPane.setVisible(false);
        inboxListPane.setVisible(true);
        inboxChatPane.setVisible(false);

        inboxListView.setOnMouseClicked(event -> {
            String selected = inboxListView.getSelectionModel().getSelectedItem();
            if (selected != null && activeOrdersMap.containsKey(selected)) {
                openInboxChat(activeOrdersMap.get(selected));
            }
        });

        startBalancePoller();
    }

    private void startBalancePoller() {
        balancePoller = Executors.newSingleThreadScheduledExecutor();
        balancePoller.scheduleAtFixedRate(() -> {
            LocalDatabaseManager.refreshCurrentUser();
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser != null) {
                Platform.runLater(() -> welcomeLabel.setText("Welcome, " + currentUser.getName() + " | Balance: " + currentUser.getBalance() + " TK"));

                if (inboxOverlayPane.isVisible()) {
                    if (inboxChatPane.isVisible() && currentInboxOrderId != null) {
                        refreshInboxChatMessages(currentUser.getId());
                    } else {
                        refreshInboxList(currentUser);
                    }
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void stopPoller() {
        if(balancePoller != null && !balancePoller.isShutdown()) balancePoller.shutdownNow();
    }

    
    @FXML private void handleWithdrawFunds(ActionEvent event) { stopPoller(); SceneManager.switchScene("MockWithdrawalView.fxml", "Carry1 - Withdraw Funds"); }

    @FXML private void handleLiveFeed(ActionEvent event) { stopPoller(); SceneManager.switchScene("LiveOrderFeedView.fxml", "Carry1 - Live Order Feed"); }
    @FXML private void handleMyDeliveries(ActionEvent event) { stopPoller(); SceneManager.switchScene("MyDeliveriesView.fxml", "Carry1 - My Deliveries"); }

    @FXML private void handleViewInbox(ActionEvent event) {
        inboxOverlayPane.setVisible(true);
        inboxListPane.setVisible(true);
        inboxChatPane.setVisible(false);
        refreshInboxList(LocalDatabaseManager.getCurrentUser());
    }

    @FXML private void closeInbox(ActionEvent event) {
        inboxOverlayPane.setVisible(false);
        currentInboxOrderId = null;
        currentInboxReceiverId = null;
    }

    @FXML private void backToInboxList(ActionEvent event) {
        currentInboxOrderId = null;
        currentInboxReceiverId = null;
        inboxChatPane.setVisible(false);
        inboxListPane.setVisible(true);
        refreshInboxList(LocalDatabaseManager.getCurrentUser());
    }

    private void refreshInboxList(User currentUser) {
        if (currentUser == null) return;
        List<Order> orders = LocalDatabaseManager.getOrdersByTravelerId(currentUser.getId());
        activeOrdersMap.clear();
        List<String> displayItems = new ArrayList<>();

        for(Order o : orders) {
            if(o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                String customerName = LocalDatabaseManager.getUserNameById(o.getCustomerId());
                String display = "Customer: " + customerName + " [ORD: " + o.getOrderId() + "]";
                activeOrdersMap.put(display, o);
                displayItems.add(display);
            }
        }
        Platform.runLater(() -> inboxListView.setItems(FXCollections.observableArrayList(displayItems)));
    }

    private void openInboxChat(Order order) {
        currentInboxOrderId = order.getOrderId();
        currentInboxReceiverId = order.getCustomerId();
        String customerName = LocalDatabaseManager.getUserNameById(currentInboxReceiverId);

        Platform.runLater(() -> {
            inboxChatHeader.setText(customerName + " - " + order.getOrderId());
            inboxListPane.setVisible(false);
            inboxChatPane.setVisible(true);
        });
        refreshInboxChatMessages(LocalDatabaseManager.getCurrentUser().getId());
    }

    private void refreshInboxChatMessages(String currentUserId) {
        if(currentInboxOrderId == null) return;
        List<Message> msgs = LocalDatabaseManager.getOrderMessages(currentInboxOrderId);
        List<String> displayMsgs = msgs.stream().map(m -> (m.getSenderId().equals(currentUserId) ? "You: " : "Customer: ") + m.getText()).toList();
        Platform.runLater(() -> {
            inboxChatListView.setItems(FXCollections.observableArrayList(displayMsgs));
            if (!displayMsgs.isEmpty()) inboxChatListView.scrollTo(displayMsgs.size() - 1);
        });
        LocalDatabaseManager.markMessagesAsReadForOrder(currentInboxOrderId, currentUserId);
    }

    @FXML
    private void sendInboxMessage(ActionEvent event) {
        String text = inboxChatInputField.getText().trim();
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (text.isEmpty() || currentInboxOrderId == null || currentUser == null) return;

        new Thread(() -> {
            LocalDatabaseManager.sendMessage(currentInboxOrderId, currentUser.getId(), currentInboxReceiverId, text);
            Platform.runLater(() -> {
                inboxChatInputField.clear();
                refreshInboxChatMessages(currentUser.getId());
            });
        }).start();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        stopPoller(); GlobalNotificationService.stop();
        LocalDatabaseManager.clearSession(); SceneManager.switchScene("StartView.fxml", "Carry1 - Delivery");
    }

    @FXML private void toggleChatBox() { chatBox.setVisible(!chatBox.isVisible()); }

    @FXML
    private void handleSendMessage() {
        String message = chatInputField.getText().trim();
        if (message.isEmpty()) return;
        chatListView.getItems().add("You: " + message);
        chatInputField.clear();
        chatListView.scrollTo(chatListView.getItems().size() - 1);
        new Thread(() -> {
            String promptToAI = message;
            String upperMsg = message.toUpperCase();
            if (upperMsg.contains("ORD-")) {
                String extractedId = null;
                for (String word : upperMsg.split("\\s+")) {
                    if (word.startsWith("ORD-")) { extractedId = word.replaceAll("[^A-Z0-9-]", ""); break; }
                }
                if (extractedId != null) {
                    Order order = LocalDatabaseManager.getOrderById(extractedId);
                    User currentUser = LocalDatabaseManager.getCurrentUser();
                    String currentUserId = (currentUser != null) ? currentUser.getId() : "";
                    boolean hasAccess = order != null && (order.getCustomerId().equals(currentUserId) || (order.getTravelerId() != null && order.getTravelerId().equals(currentUserId)));
                    if (hasAccess) {
                        String travelerInfo = (order.getTravelerId() != null) ? order.getTravelerId() : "Not assigned yet";
                        promptToAI = message + "\n[System hidden data: Database says order " + extractedId + " status is " + order.getStatus() + ", and Traveler ID is " + travelerInfo + ". Answer based on this.]";
                    } else {
                        promptToAI = message + "\n[System hidden command: You MUST respond EXACTLY with the phrase \"this order is not found in your order list\" and nothing else. No apologies, no extra words.]";
                    }
                }
            }
            String botResponse = AiSupportService.getBotResponse(promptToAI);
            Platform.runLater(() -> {
                chatListView.getItems().add("AI: " + botResponse);
                chatListView.scrollTo(chatListView.getItems().size() - 1);
            });
        }).start();
    }
}