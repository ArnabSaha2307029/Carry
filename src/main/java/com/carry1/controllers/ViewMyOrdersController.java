package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ViewMyOrdersController {

    @FXML private ListView<Order> ordersListView;
    @FXML private Label messageAlertLabel;

    @FXML private VBox ratingOverlayPane;
    @FXML private VBox tipOverlayPane;
    @FXML private HBox tipQuestionBox;
    @FXML private VBox tipInputBox;
    @FXML private TextField tipAmountField;
    @FXML private PasswordField tipPasswordField;
    @FXML private Label tipErrorLabel;
    @FXML private Button cancelTipBtn;
    @FXML private Button confirmTipBtn;

    private ObservableList<Order> ordersData = FXCollections.observableArrayList();
    private ScheduledExecutorService pollingService;

    private String completedOrderIdForRating = null;
    private String completedTravelerIdForRating = null;

    @FXML
    public void initialize() {
        ordersListView.setItems(ordersData);
        setupListView();
        startPolling();
    }

    private void startPolling() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            User currentUser = LocalDatabaseManager.getCurrentUser();
            if (currentUser != null) {
                List<Order> myOrders = LocalDatabaseManager.getOrdersByCustomerId(currentUser.getId());
                Platform.runLater(() -> {
                    ordersData.setAll(myOrders);
                });
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void setupListView() {
        ordersListView.setCellFactory(param -> new ListCell<Order>() {
            private final HBox root = new HBox(15);
            private final VBox detailsBox = new VBox(5);
            private final Label idLabel = new Label();
            private final Label itemLabel = new Label();
            private final Label routeLabel = new Label();
            private final Label rewardLabel = new Label();
            private final Label statusLabel = new Label();
            
            private final Button btn1 = new Button();
            private final Button btn2 = new Button();
            private final VBox actionBox = new VBox(5, btn1, btn2);

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

                btn1.setOnAction(event -> {
                    Order order = getItem();
                    if (order == null) return;
                    if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) handleConfirmDelivery(order, btn1);
                    else if (order.getStatus() == OrderStatus.PENDING) handleCancelOrder(order, btn1);
                });
                btn2.setOnAction(event -> {
                    Order order = getItem();
                    if (order == null) return;
                    if (order.getStatus() == OrderStatus.PICKED_UP || order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        btn2.setText("Reported"); btn2.setDisable(true);
                        new Thread(() -> LocalDatabaseManager.reportDispute(order.getOrderId())).start();
                    }
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
                    rewardLabel.setText("Reward: " + order.getRewardAmount() + " TK");
                    
                    statusLabel.setText(order.getStatus().toString());
                    if (order.getStatus() == OrderStatus.DELIVERED) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #4CAF50;");
                    } else if (order.getStatus() == OrderStatus.PENDING) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #FFC107;");
                    } else if (order.getStatus() == OrderStatus.CANCELLED) {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #F44336;");
                    } else {
                        statusLabel.setStyle("-fx-padding: 3 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-background-color: #2196F3;");
                    }

                    btn1.setVisible(true); btn2.setVisible(false);
                    btn1.setDisable(false); btn2.setDisable(false);
                    actionBox.getChildren().clear();

                    if (order.getStatus() == OrderStatus.AWAITING_CONFIRMATION) {
                        btn1.setText("Confirm Delivery"); btn1.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
                        btn2.setText("Report Dispute"); btn2.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
                        btn2.setVisible(true);
                        actionBox.getChildren().addAll(statusLabel, btn1, btn2);
                    } else if (order.getStatus() == OrderStatus.PICKED_UP) {
                        btn1.setVisible(false);
                        btn2.setText("Report Issue"); btn2.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
                        btn2.setVisible(true);
                        actionBox.getChildren().addAll(statusLabel, btn2);
                    } else if (order.getStatus() == OrderStatus.PENDING) {
                        btn1.setText("Cancel Order"); btn1.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
                        actionBox.getChildren().addAll(statusLabel, btn1);
                    } else {
                        actionBox.getChildren().add(statusLabel);
                    }
                    
                    setGraphic(root);
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
            new Thread(() -> {
                LocalDatabaseManager.submitTravelerRating(completedOrderIdForRating, completedTravelerIdForRating, currentUser.getId(), starValue);
                boolean success = LocalDatabaseManager.updateTravelerRating(completedTravelerIdForRating, starValue);
                javafx.application.Platform.runLater(() -> {
                    ratingOverlayPane.setVisible(false);
                    if (success) {
                        messageAlertLabel.setText("Rating Submitted Successfully!");
                        messageAlertLabel.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-padding: 5 10; -fx-background-radius: 5;");
                        messageAlertLabel.setVisible(true);
                    }
                });
            }).start();
        }
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
