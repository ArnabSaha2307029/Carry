package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ViewMyOrdersController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colOrderId, colItemName, colPickup, colDropoff, colStatus;
    @FXML private TableColumn<Order, Double> colReward;
    @FXML private TableColumn<Order, Void> colAction;

    @FXML private VBox ratingOverlayPane;
    @FXML private Label messageAlertLabel;

    // Tip Overlay Elements
    @FXML private VBox tipOverlayPane, tipInputBox;
    @FXML private HBox tipQuestionBox;
    @FXML private TextField tipAmountField;
    @FXML private PasswordField tipPasswordField;
    @FXML private Label tipErrorLabel;
    @FXML private Button confirmTipBtn, cancelTipBtn;

    private ScheduledExecutorService pollingService;
    private String completedOrderIdForRating = null;
    private String completedTravelerIdForRating = null;

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colPickup.setCellValueFactory(new PropertyValueFactory<>("pickupLocation"));
        colDropoff.setCellValueFactory(new PropertyValueFactory<>("dropoffLocation"));
        colReward.setCellValueFactory(new PropertyValueFactory<>("rewardAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupActionColumn();
        startAutoRefresh();

        ratingOverlayPane.setVisible(false);
        tipOverlayPane.setVisible(false);
        messageAlertLabel.setVisible(false);
    }

    private void startAutoRefresh() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser != null) {
                List<Order> myOrders = LocalDatabaseManager.getOrdersByCustomerId(currentUser.getId());
                boolean hasNewMessage = LocalDatabaseManager.hasUnreadMessages(currentUser.getId());

                Platform.runLater(() -> {
                    ordersTable.setItems(FXCollections.observableArrayList(myOrders));
                    messageAlertLabel.setVisible(hasNewMessage);
                    if (hasNewMessage) messageAlertLabel.setText("You have unread messages in your active orders!");
                });
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn1 = new Button();
            private final Button btn2 = new Button();
            private final HBox actionBox = new HBox(5, btn1, btn2);
            {
                btn1.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) handleConfirmDelivery(order, btn1);
                    else if (order.getStatus() == OrderStatus.PENDING) handleCancelOrder(order, btn1);
                });
                btn2.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == OrderStatus.PICKED_UP || order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        btn2.setText("Reported"); btn2.setDisable(true);
                        new Thread(() -> LocalDatabaseManager.reportDispute(order.getOrderId())).start();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Order order = getTableView().getItems().get(getIndex());
                    btn1.setVisible(true); btn2.setVisible(false);
                    btn1.setDisable(false); btn2.setDisable(false);

                    if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        btn1.setText("Confirm Delivery"); btn1.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                        btn2.setText("Report Dispute"); btn2.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        btn2.setVisible(true);
                        setGraphic(actionBox);
                    } else if (order.getStatus() == OrderStatus.PICKED_UP) {
                        btn1.setVisible(false);
                        btn2.setText("Report Issue"); btn2.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        btn2.setVisible(true);
                        setGraphic(actionBox);
                    } else if (order.getStatus() == OrderStatus.PENDING) {
                        btn1.setText("Cancel Order"); btn1.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        setGraphic(actionBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void handleCancelOrder(Order order, Button btn) {
        btn.setText("Cancelling..."); btn.setDisable(true);
        new Thread(() -> LocalDatabaseManager.cancelOrderAndRefundEscrow(order.getOrderId(), order.getCustomerId(), order.getRewardAmount())).start();
    }

    private void handleConfirmDelivery(Order order, Button btn) {
        if (order.getCustomerId() == null || order.getTravelerId() == null) return;
        btn.setText("Processing..."); btn.setDisable(true);
        new Thread(() -> {
            boolean success = LocalDatabaseManager.completeDeliveryWithCommission(order.getOrderId(), order.getTravelerId(), order.getRewardAmount());
            Platform.runLater(() -> {
                if (success) {
                    completedOrderIdForRating = order.getOrderId();
                    completedTravelerIdForRating = order.getTravelerId();
                    ratingOverlayPane.setVisible(true);
                }
                else { btn.setDisable(false); }
            });
        }).start();
    }

    @FXML private void submitRating1() { submitRating(1); }
    @FXML private void submitRating2() { submitRating(2); }
    @FXML private void submitRating3() { submitRating(3); }
    @FXML private void submitRating4() { submitRating(4); }
    @FXML private void submitRating5() { submitRating(5); }

    private void submitRating(int starValue) {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (completedOrderIdForRating != null && completedTravelerIdForRating != null && currentUser != null) {
            new Thread(() -> LocalDatabaseManager.submitTravelerRating(completedOrderIdForRating, completedTravelerIdForRating, currentUser.getId(), starValue)).start();
        }
        showTipOverlay();
    }

    @FXML
    private void handleSkipRating(ActionEvent event) {
        showTipOverlay();
    }

    private void showTipOverlay() {
        ratingOverlayPane.setVisible(false);
        tipOverlayPane.setVisible(true);
        tipQuestionBox.setVisible(true);
        tipQuestionBox.setManaged(true);
        tipInputBox.setVisible(false);
        tipInputBox.setManaged(false);
    }

    @FXML
    private void handleTipYes(ActionEvent event) {
        tipQuestionBox.setVisible(false);
        tipQuestionBox.setManaged(false);
        tipInputBox.setVisible(true);
        tipInputBox.setManaged(true);
    }

    @FXML
    private void handleTipNo(ActionEvent event) {
        if (pollingService != null) pollingService.shutdownNow();
        SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
    }

    @FXML
    private void confirmTip(ActionEvent event) {
        String amountStr = tipAmountField.getText().trim();
        String passStr = tipPasswordField.getText();

        try {
            double tipAmount = Double.parseDouble(amountStr);
            if (tipAmount <= 0) throw new NumberFormatException();

            LocalDatabaseManager.refreshCurrentUser();
            User currentUser = LocalDatabaseManager.getCurrentUser();

            if (tipAmount > currentUser.getBalance()) {
                tipErrorLabel.setStyle("-fx-text-fill: red;");
                tipErrorLabel.setText("Insufficient balance for this tip.");
                return;
            }

            if (passStr.isEmpty()) {
                tipErrorLabel.setStyle("-fx-text-fill: red;");
                tipErrorLabel.setText("Password is required.");
                return;
            }

            if (!LocalDatabaseManager.verifyUserPassword(currentUser.getId(), passStr)) {
                tipErrorLabel.setStyle("-fx-text-fill: red;");
                tipErrorLabel.setText("Incorrect password.");
                return;
            }

            confirmTipBtn.setDisable(true);
            cancelTipBtn.setDisable(true);
            tipErrorLabel.setStyle("-fx-text-fill: #2196F3;");
            tipErrorLabel.setText("Processing Tip...");

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
                boolean success = LocalDatabaseManager.sendTipTransaction(currentUser.getId(), completedTravelerIdForRating, tipAmount);
                Platform.runLater(() -> {
                    if(success) {
                        handleTipNo(null);
                    } else {
                        confirmTipBtn.setDisable(false);
                        cancelTipBtn.setDisable(false);
                        tipErrorLabel.setStyle("-fx-text-fill: red;");
                        tipErrorLabel.setText("Failed to process tip. Try again.");
                    }
                });
            }).start();
        } catch (NumberFormatException e) {
            tipErrorLabel.setStyle("-fx-text-fill: red;");
            tipErrorLabel.setText("Invalid tip amount.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (pollingService != null) pollingService.shutdownNow();
        SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
    }
}