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
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackOrderController {

    @FXML private ListView<String> orderListView;
    @FXML private VBox detailsPane, travelerInfoPane;

    @FXML private Label orderIdLabel, itemNameLabel, routeLabel, statusLabel;
    @FXML private Label travelerNameLabel, travelerPhoneLabel, travelerIdLabel;

    private Map<String, Order> orderMap = new HashMap<>();

    @FXML
    public void initialize() {
        detailsPane.setVisible(false);
        loadUserOrders();

        orderListView.setOnMouseClicked(event -> {
            String selectedId = orderListView.getSelectionModel().getSelectedItem();
            if (selectedId != null && orderMap.containsKey(selectedId)) {
                showOrderDetails(orderMap.get(selectedId));
            }
        });
    }

    private void loadUserOrders() {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser == null) return;

        new Thread(() -> {
            List<Order> orders = LocalDatabaseManager.getOrdersByCustomerId(currentUser.getId());
            List<String> orderIds = orders.stream().map(Order::getOrderId).toList();

            for (Order o : orders) {
                orderMap.put(o.getOrderId(), o);
            }

            Platform.runLater(() -> {
                orderListView.setItems(FXCollections.observableArrayList(orderIds));
            });
        }).start();
    }

    private void showOrderDetails(Order order) {
        detailsPane.setVisible(true);
        orderIdLabel.setText("Order ID: " + order.getOrderId());
        itemNameLabel.setText("Item: " + order.getItemName());
        routeLabel.setText("Route: " + order.getPickupLocation() + " ➔ " + order.getDropoffLocation());
        statusLabel.setText(order.getStatus().name().replace("_", " "));

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
                    travelerIdLabel.setText("Traveler ID: " + order.getTravelerId());
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