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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminDashboardController {

    @FXML private Label activeOrdersLabel, deliveredOrdersLabel, systemMoneyLabel, adminProfitLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> uColId, uColName, uColRole, uColStatus;
    @FXML private TableColumn<User, Double> uColBalance;
    @FXML private TableColumn<User, Void> uColAction;

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> oColId, oColCust, oColTrav, oColStatus;
    @FXML private TableColumn<Order, Void> oColAction;

    private ScheduledExecutorService adminPoller;

    @FXML
    public void initialize() {
        setupUserTable();
        setupOrderTable();
        startAdminPoller();
    }

    // ডেডিকেটেড অ্যাডমিন পোলিং (৩ সেকেন্ড পরপর সব ডাটা সিঙ্ক করবে)
    private void startAdminPoller() {
        adminPoller = Executors.newSingleThreadScheduledExecutor();
        adminPoller.scheduleAtFixedRate(() -> {
            Map<String, Double> stats = LocalDatabaseManager.getGlobalLedgerStats();
            List<User> users = LocalDatabaseManager.getAllUsers();
            List<Order> orders = LocalDatabaseManager.getAllOrders();

            Platform.runLater(() -> {
                activeOrdersLabel.setText(String.valueOf(stats.getOrDefault("activeOrders", 0.0).intValue()));
                deliveredOrdersLabel.setText(String.valueOf(stats.getOrDefault("deliveredOrders", 0.0).intValue()));
                systemMoneyLabel.setText(String.format("%.2f TK", stats.getOrDefault("totalSystemMoney", 0.0)));
                adminProfitLabel.setText(String.format("%.2f TK", stats.getOrDefault("adminProfit", 0.0)));

                userTable.setItems(FXCollections.observableArrayList(users));
                orderTable.setItems(FXCollections.observableArrayList(orders));
            });
        }, 0, 3, TimeUnit.SECONDS);
    }

    // ম্যানুয়াল রিফ্রেশ বাটন
    @FXML
    private void loadStats() {
        // পোলিং অটোমেটিক করছে, তাই এখানে ডামি কল রাখা হলো যাতে বাটনে চাপলে ক্র্যাশ না করে
        System.out.println("Stats auto-refreshing via poller...");
    }

    private void setupUserTable() {
        uColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        uColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        uColRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        uColBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        uColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        uColAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnBan = new Button();
            private final Button btnBal = new Button("Edit Balance");
            private final HBox pane = new HBox(5, btnBan, btnBal);
            {
                btnBan.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    String newStatus = u.getStatus().equals("BANNED") ? "ACTIVE" : "BANNED";
                    new Thread(() -> LocalDatabaseManager.setUserStatus(u.getId(), newStatus)).start();
                });
                btnBal.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(u.getBalance()));
                    dialog.setHeaderText("Adjust Balance for " + u.getId());
                    dialog.showAndWait().ifPresent(res -> {
                        try {
                            double newBal = Double.parseDouble(res);
                            new Thread(() -> LocalDatabaseManager.adjustUserBalance(u.getId(), newBal)).start();
                        } catch(Exception ignored){}
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    User u = getTableView().getItems().get(getIndex());
                    btnBan.setText(u.getStatus().equals("BANNED") ? "Unban" : "Ban");
                    btnBan.setStyle(u.getStatus().equals("BANNED") ? "-fx-background-color: #4CAF50; -fx-text-fill: white;" : "-fx-background-color: #F44336; -fx-text-fill: white;");
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupOrderTable() {
        oColId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        oColCust.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        oColTrav.setCellValueFactory(new PropertyValueFactory<>("travelerId"));
        oColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        oColAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPending = new Button("Force PENDING");
            private final Button btnRefund = new Button("Force Refund");
            private final HBox pane = new HBox(5, btnPending, btnRefund);
            {
                btnPending.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                btnRefund.setStyle("-fx-background-color: #E91E63; -fx-text-fill: white;");
                btnPending.setOnAction(e -> {
                    new Thread(() -> LocalDatabaseManager.forceOrderPending(getTableView().getItems().get(getIndex()).getOrderId())).start();
                });
                btnRefund.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    new Thread(() -> LocalDatabaseManager.forceRefundAdmin(o.getOrderId(), o.getCustomerId(), o.getRewardAmount())).start();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Order o = getTableView().getItems().get(getIndex());
                    btnPending.setVisible(o.getStatus() == OrderStatus.PICKED_UP || o.getStatus() == OrderStatus.DISPUTED);
                    btnRefund.setVisible(o.getStatus() == OrderStatus.DISPUTED);
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (adminPoller != null && !adminPoller.isShutdown()) adminPoller.shutdownNow();
        LocalDatabaseManager.clearSession();
        SceneManager.switchScene("StartView.fxml", "Carry1 - Login");
    }
}