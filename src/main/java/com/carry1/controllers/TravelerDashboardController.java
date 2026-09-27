package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Message;
import com.carry1.models.Complaint;
import com.carry1.models.ComplaintMessage;
import javafx.scene.control.Button;
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

    @FXML private StackPane supportOverlayPane;
    @FXML private ListView<String> supportChatListView;
    @FXML private TextField supportChatInputField;
    @FXML private Button reportIssueBtn;

    private Map<String, Complaint> activeSupportMap = new HashMap<>();
    private int currentSupportComplaintId = -1;


    @FXML
    public void initialize() {
        inboxOverlayPane.setVisible(false);
        supportOverlayPane.setVisible(false);
        inboxListPane.setVisible(true);
        inboxChatPane.setVisible(false);

                inboxListView.setOnMouseClicked(event -> {
            String selected = inboxListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (activeOrdersMap.containsKey(selected)) {
                    openInboxChat(activeOrdersMap.get(selected));
                } else if (activeSupportMap.containsKey(selected)) {
                    openSupportChat(activeSupportMap.get(selected));
                }
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
                Platform.runLater(() -> welcomeLabel.setText(String.format("Welcome, %s | Balance: %.2f TK", currentUser.getName(), currentUser.getBalance())));

                                if (inboxOverlayPane.isVisible()) {
                    if (inboxChatPane.isVisible() && currentInboxOrderId != null) {
                        refreshInboxChatMessages(currentUser.getId());
                    } else {
                        refreshInboxList(currentUser);
                    }
                }
                if (supportOverlayPane != null && supportOverlayPane.isVisible() && currentSupportComplaintId != -1) {
                    refreshSupportChatMessages(currentUser.getId());
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void stopPoller() {
        if(balancePoller != null && !balancePoller.isShutdown()) balancePoller.shutdownNow();
    }

    
    @FXML private void handleAddFunds() {
        stopPoller();
        SceneManager.switchScene("MockPaymentView.fxml", "Carry1 - Secure Payment");
    }

    @FXML private void handleWithdrawFunds() { stopPoller(); SceneManager.switchScene("MockWithdrawalView.fxml", "Carry1 - Withdraw Funds"); }

    @FXML private void handleLiveFeed() { stopPoller(); SceneManager.switchScene("LiveOrderFeedView.fxml", "Carry1 - Live Order Feed"); }
    @FXML private void handleMyDeliveries() { stopPoller(); SceneManager.switchScene("MyDeliveriesView.fxml", "Carry1 - My Deliveries"); }

    @FXML private void handleViewInbox() {
        inboxOverlayPane.setVisible(true);
        inboxListPane.setVisible(true);
        inboxChatPane.setVisible(false);
        refreshInboxList(LocalDatabaseManager.getCurrentUser());
    }

    @FXML private void closeInbox() {
        inboxOverlayPane.setVisible(false);
        supportOverlayPane.setVisible(false);
        currentInboxOrderId = null;
        currentInboxReceiverId = null;
    }

    @FXML private void backToInboxList() {
        currentInboxOrderId = null;
        currentInboxReceiverId = null;
        inboxChatPane.setVisible(false);
        inboxListPane.setVisible(true);
        refreshInboxList(LocalDatabaseManager.getCurrentUser());
    }

        private void refreshInboxList(User currentUser) {
        if (currentUser == null) return;
        List<Order> orders = LocalDatabaseManager.getOrdersByTravelerId(currentUser.getId());
        List<Complaint> complaints = LocalDatabaseManager.getOpenComplaintsForUser(currentUser.getId());
        activeOrdersMap.clear();
        activeSupportMap.clear();
        List<String> displayItems = new ArrayList<>();

        for(Order o : orders) {
            if(o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                String customerName = LocalDatabaseManager.getUserNameById(o.getCustomerId());
                String display = "Customer: " + customerName + " [ORD: " + o.getOrderId() + "]";
                activeOrdersMap.put(display, o);
                displayItems.add(display);
            }
        }
        
        for(Complaint c : complaints) {
            String display = "Carry Administrator [ORD-" + c.getOrderId() + "]";
            activeSupportMap.put(display, c);
            displayItems.add(display);
        }
        
        Platform.runLater(() -> inboxListView.setItems(FXCollections.observableArrayList(displayItems)));
    }

        private void openInboxChat(Order order) {
        currentInboxOrderId = order.getOrderId();
        currentInboxReceiverId = order.getCustomerId();
        String otherName = LocalDatabaseManager.getUserNameById(currentInboxReceiverId);

        Platform.runLater(() -> {
            inboxChatHeader.setText(otherName + " - " + order.getOrderId());
            if (reportIssueBtn != null) reportIssueBtn.setVisible(order.getStatus() == OrderStatus.PICKED_UP);
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
    private void sendInboxMessage() {
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


    @FXML private void handleReportIssue() {
        if (currentInboxOrderId != null && currentInboxReceiverId != null) {
            String currentUserId = LocalDatabaseManager.getCurrentUser().getId();
            new Thread(() -> {
                Complaint c = LocalDatabaseManager.createComplaint(currentInboxOrderId, currentUserId, currentInboxReceiverId);
                if (c != null) {
                    Platform.runLater(() -> openSupportChat(c));
                }
            }).start();
        }
    }

    private void openSupportChat(Complaint complaint) {
        currentSupportComplaintId = complaint.getId();
        Platform.runLater(() -> {
            inboxOverlayPane.setVisible(false);
            supportOverlayPane.setVisible(true);
        });
        refreshSupportChatMessages(LocalDatabaseManager.getCurrentUser().getId());
    }

    @FXML private void closeSupportOverlay() {
        supportOverlayPane.setVisible(false);
        currentSupportComplaintId = -1;
    }

    private void refreshSupportChatMessages(String currentUserId) {
        if(currentSupportComplaintId == -1) return;
        List<ComplaintMessage> msgs = LocalDatabaseManager.getComplaintMessages(currentSupportComplaintId);
        List<String> displayMsgs = new java.util.ArrayList<>();
        for (ComplaintMessage m : msgs) {
            String prefix = m.getSenderId().equals(currentUserId) ? "You: " : "Admin: ";
            displayMsgs.add(prefix + m.getMessageText());
        }
        Platform.runLater(() -> {
            supportChatListView.setItems(FXCollections.observableArrayList(displayMsgs));
            if (!displayMsgs.isEmpty()) supportChatListView.scrollTo(displayMsgs.size() - 1);
        });
    }

    @FXML
    private void sendSupportMessage() {
        String text = supportChatInputField.getText().trim();
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (text.isEmpty() || currentSupportComplaintId == -1 || currentUser == null) return;

        new Thread(() -> {
            LocalDatabaseManager.sendComplaintMessage(currentSupportComplaintId, currentUser.getId(), text);
            Platform.runLater(() -> {
                supportChatInputField.clear();
                refreshSupportChatMessages(currentUser.getId());
            });
        }).start();
    }

    @FXML
    private void handleLogout() {
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