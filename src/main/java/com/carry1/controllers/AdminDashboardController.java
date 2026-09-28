package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import com.carry1.models.Transaction;
import javafx.scene.control.ListCell;
import com.carry1.models.Complaint;
import com.carry1.models.ComplaintMessage;

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
    @FXML private javafx.scene.control.ListView<Transaction> transactionListView;
    @FXML private javafx.scene.control.Label transactionAlertLabel;

    @FXML private Label activeOrdersLabel, deliveredOrdersLabel, systemMoneyLabel, adminProfitLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> uColId, uColName, uColRole, uColStatus;
    @FXML private TableColumn<User, Double> uColBalance;
    @FXML private TableColumn<User, Void> uColAction;

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> oColId, oColCust, oColTrav, oColStatus;
    @FXML private TableColumn<Order, Void> oColAction;

    private ScheduledExecutorService adminPoller;

    @FXML private ListView<Complaint> complaintListView;
    @FXML private ListView<String> complaintChatListView;
    @FXML private Label complaintChatHeader;
    @FXML private TextField adminChatInput;
    @FXML private Button resolveTicketBtn;
    
    private Complaint currentAdminComplaint = null;


    @FXML
    public void initialize() {
        setupUserTable();
        setupOrderTable();
        setupTransactionList();
        setupComplaintList();
        startAdminPoller();
    }

    
    
    private void setupTransactionList() {
        if (transactionListView == null) return;
        transactionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                } else {
                    setText(String.format("[%d] | Sender: %s -> Receiver: %s | Amount: %.2f TK | Type: %s",
                            t.getId(), t.getSenderId(), t.getReceiverId(), t.getAmount(), t.getType()));
                }
            }
        });
    }

    @FXML
    private void handleReverseTransaction(javafx.event.ActionEvent event) {
        if (transactionListView == null) return;
        Transaction selected = transactionListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            transactionAlertLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            transactionAlertLabel.setText("Please select a transaction to reverse.");
            return;
        }
        new Thread(() -> {
            boolean success = LocalDatabaseManager.reverseTransaction(selected.getId());
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    transactionAlertLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    transactionAlertLabel.setText("Transaction reversed successfully!");
                } else {
                    transactionAlertLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    transactionAlertLabel.setText("Reversal failed: Insufficient receiver balance or already reversed.");
                }
            });
        }).start();
    }

    private void startAdminPoller() {
        adminPoller = Executors.newSingleThreadScheduledExecutor();
        adminPoller.scheduleAtFixedRate(() -> {
            Map<String, Double> stats = LocalDatabaseManager.getGlobalLedgerStats();
            List<User> users = LocalDatabaseManager.getAllUsers();
            List<Order> orders = LocalDatabaseManager.getAllOrders();

            
            List<Complaint> complaints = LocalDatabaseManager.getOpenComplaints();
            List<Transaction> transactions = LocalDatabaseManager.getAllTransactions();
            Platform.runLater(() -> {
                if (transactionListView != null) {
                    Transaction selected = transactionListView.getSelectionModel().getSelectedItem();
                    transactionListView.getItems().setAll(transactions);
                    if (selected != null) {
                        for (Transaction t : transactions) {
                            if (t.getId() == selected.getId()) {
                                transactionListView.getSelectionModel().select(t);
                                break;
                            }
                        }
                    }
                }
                activeOrdersLabel.setText(String.valueOf(stats.getOrDefault("activeOrders", 0.0).intValue()));
                deliveredOrdersLabel.setText(String.valueOf(stats.getOrDefault("deliveredOrders", 0.0).intValue()));
                double totalAmount = stats.getOrDefault("totalSystemMoney", 0.0);
                systemMoneyLabel.setText(String.format("%.2f TK", totalAmount));
                double adminProfit = stats.getOrDefault("adminProfit", 0.0);
                adminProfitLabel.setText(String.format("%.2f TK", adminProfit));

                userTable.setItems(FXCollections.observableArrayList(users));
                orderTable.setItems(FXCollections.observableArrayList(orders));
                
                Complaint selected = complaintListView.getSelectionModel().getSelectedItem();
                complaintListView.setItems(FXCollections.observableArrayList(complaints));
                if (selected != null) {
                    for (Complaint c : complaints) {
                        if (c.getId() == selected.getId()) {
                            complaintListView.getSelectionModel().select(c);
                            break;
                        }
                    }
                }
                
                if (currentAdminComplaint != null) {
                    refreshComplaintMessages();
                }
            });
        }, 0, 3, TimeUnit.SECONDS);
    }

    
    @FXML
    private void loadStats() {
        
        System.out.println("Stats auto-refreshing via poller...");
    }


    private void setupComplaintList() {
        complaintListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Complaint item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String creator = LocalDatabaseManager.getUserNameById(item.getCreatorId());
                    String against = LocalDatabaseManager.getUserNameById(item.getAgainstId());
                    setText(creator + " vs " + against + " [ORD-" + item.getOrderId() + "]");
                }
            }
        });
        
        complaintListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentAdminComplaint = newVal;
                String creator = LocalDatabaseManager.getUserNameById(newVal.getCreatorId());
                String against = LocalDatabaseManager.getUserNameById(newVal.getAgainstId());
                complaintChatHeader.setText(creator + " vs " + against + " [ORD-" + newVal.getOrderId() + "]");
                resolveTicketBtn.setVisible(true);
                refreshComplaintMessages();
            } else {
                currentAdminComplaint = null;
                complaintChatHeader.setText("Select a ticket");
                resolveTicketBtn.setVisible(false);
                complaintChatListView.getItems().clear();
            }
        });
    }

    private void refreshComplaintMessages() {
        if (currentAdminComplaint == null) return;
        List<ComplaintMessage> msgs = LocalDatabaseManager.getComplaintMessages(currentAdminComplaint.getId());
        List<String> displayMsgs = new java.util.ArrayList<>();
        for (ComplaintMessage msg : msgs) {
            String prefix = msg.getSenderId().equals("ADMIN-adm") ? "Admin: " : LocalDatabaseManager.getUserNameById(msg.getSenderId()) + ": ";
            displayMsgs.add(prefix + msg.getMessageText());
        }
        Platform.runLater(() -> {
            complaintChatListView.setItems(FXCollections.observableArrayList(displayMsgs));
            if (!displayMsgs.isEmpty()) complaintChatListView.scrollTo(displayMsgs.size() - 1);
        });
    }
    
    @FXML
    private void sendComplaintMessage(ActionEvent event) {
        String text = adminChatInput.getText().trim();
        if (text.isEmpty() || currentAdminComplaint == null) return;
        new Thread(() -> {
            LocalDatabaseManager.sendComplaintMessage(currentAdminComplaint.getId(), "ADMIN-adm", text);
            Platform.runLater(() -> {
                adminChatInput.clear();
                refreshComplaintMessages();
            });
        }).start();
    }
    
    @FXML
    private void handleResolveTicket(ActionEvent event) {
        if (currentAdminComplaint == null) return;
        new Thread(() -> {
            LocalDatabaseManager.resolveComplaint(currentAdminComplaint.getId());
            Platform.runLater(() -> {
                currentAdminComplaint = null;
                complaintChatHeader.setText("Select a ticket");
                resolveTicketBtn.setVisible(false);
                complaintChatListView.getItems().clear();
            });
        }).start();
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
            private final Button btnDel = new Button("Delete");
            private final HBox pane = new HBox(5, btnBan, btnBal, btnDel);
            {
                btnDel.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                btnDel.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    new Thread(() -> {
                        boolean success = LocalDatabaseManager.deleteUser(u.getId());
                        Platform.runLater(() -> {
                            if (success) {
                                getTableView().getItems().remove(u);
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot delete user. They likely have existing orders or transactions.");
                                alert.showAndWait();
                            }
                        });
                    }).start();
                });
                btnBan.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    String newStatus = u.getStatus().equals("BANNED") ? "ACTIVE" : "BANNED";
                    new Thread(() -> {
                        LocalDatabaseManager.setUserStatus(u.getId(), newStatus);
                        Platform.runLater(() -> {
                            u.setStatus(newStatus);
                            getTableView().refresh();
                        });
                    }).start();
                });
                btnBal.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(u.getBalance()));
                    dialog.setHeaderText("Adjust Balance for " + u.getId());
                    dialog.showAndWait().ifPresent(res -> {
                        try {
                            double newBal = Double.parseDouble(res);
                            new Thread(() -> {
                                LocalDatabaseManager.adjustUserBalance(u.getId(), newBal);
                                Platform.runLater(() -> {
                                    u.setBalance(newBal);
                                    getTableView().refresh();
                                });
                            }).start();
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