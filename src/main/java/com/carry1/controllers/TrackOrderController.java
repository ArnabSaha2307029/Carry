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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.List;

public class TrackOrderController {

    @FXML private ListView<Order> activeOrdersListView;
    @FXML private VBox orderDetailPane;
    @FXML private VBox travelerInfoPane;

    @FXML private Label detailOrderId, detailRoute, detailFee, detailStatus;
    @FXML private Label travelerNameLabel, travelerPhoneLabel;

    @FXML
    public void initialize() {
        orderDetailPane.setVisible(false);
        setupListView();
        loadUserOrders();

        activeOrdersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateDetailPane(newVal));
    }

    private void setupListView() {
        activeOrdersListView.setCellFactory(param -> new ListCell<Order>() {
            @Override
            protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setText(null);
                } else {
                    setText("Order: " + order.getOrderId() + " (" + order.getStatus().toString() + ")");
                    setStyle("-fx-font-weight: bold; -fx-padding: 10;");
                }
            }
        });
    }

    private void loadUserOrders() {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser == null) return;

        new Thread(() -> {
            List<Order> orders = LocalDatabaseManager.getOrdersByCustomerId(currentUser.getId());
            Platform.runLater(() -> {
                activeOrdersListView.setItems(FXCollections.observableArrayList(orders));
            });
        }).start();
    }

    private void updateDetailPane(Order order) {
        if (order == null) {
            orderDetailPane.setVisible(false);
            return;
        }

        orderDetailPane.setVisible(true);
        detailOrderId.setText("Order ID: " + order.getOrderId());
        detailRoute.setText("Route: " + order.getPickupLocation() + " -> " + order.getDropoffLocation());
        detailFee.setText("Fee: " + order.getRewardAmount() + " TK");
        detailStatus.setText(order.getStatus().name().replace("_", " "));

        if (order.getTravelerId() != null &&
                (order.getStatus() == OrderStatus.PICKED_UP ||
                        order.getStatus() == OrderStatus.AWAITING_CONFIRMATION ||
                        order.getStatus() == OrderStatus.DELIVERED)) {

            new Thread(() -> {
                String tName = LocalDatabaseManager.getUserNameById(order.getTravelerId());
                String tPhone = LocalDatabaseManager.getUserPhoneById(order.getTravelerId());

                Platform.runLater(() -> {
                    travelerNameLabel.setText("Name: " + tName);
                    travelerPhoneLabel.setText("Phone: " + tPhone);
                    travelerInfoPane.setVisible(true);
                });
            }).start();
        } else {
            travelerInfoPane.setVisible(false);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
    }
}
