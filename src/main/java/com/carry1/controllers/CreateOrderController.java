package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Order;
import com.carry1.models.OrderStatus;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateOrderController {

    private static final String GOOGLE_API_KEY = "YOUR_GOOGLE_API_KEY_HERE"; 

    @FXML private VBox step1Box, step2Box, step3Box, step4Box;

    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private TextField itemNameField, itemWeightField;
    @FXML private Label errorLabel1;

    @FXML private TextField senderNameField, senderPhoneField, pickupLocationField, pickupInfoField;
    @FXML private Label errorLabel2;

    @FXML private TextField receiverNameField, receiverPhoneField, dropoffLocationField, dropoffInfoField;
    @FXML private Label errorLabel3;

    @FXML private VBox loadingBox, summaryBox;
    @FXML private Label distanceLabel, weightLabel, feeLabel, balanceWarningLabel, errorLabel4;
    @FXML private Button backBtn4, confirmBtn;

    private double finalWeight = 0;
    private double finalDistanceKm = 0;
    private double finalFee = 0;
    private double finalCharge = 0;
    private double finalTotalCost = 0;

    @FXML
    public void initialize() {
        itemTypeCombo.getItems().addAll("Documents", "Food", "Other");
        itemTypeCombo.getSelectionModel().selectFirst();
        showBox(step1Box);
    }

    private void showBox(VBox box) {
        step1Box.setVisible(false); step2Box.setVisible(false);
        step3Box.setVisible(false); step4Box.setVisible(false);
        box.setVisible(true);
    }

    @FXML private void goToStep1(ActionEvent event) { showBox(step1Box); }

    @FXML private void goToStep2(ActionEvent event) {
        if (itemNameField.getText().trim().isEmpty() || itemWeightField.getText().trim().isEmpty()) {
            errorLabel1.setText("All fields are required."); return;
        }
        try {
            finalWeight = Double.parseDouble(itemWeightField.getText().trim());
            if (finalWeight <= 0) throw new NumberFormatException();
            errorLabel1.setText(""); showBox(step2Box);
        } catch (NumberFormatException e) { errorLabel1.setText("Invalid weight. Enter numeric value."); }
    }

    @FXML private void goToStep3(ActionEvent event) {
        if (senderNameField.getText().trim().isEmpty() || senderPhoneField.getText().trim().isEmpty() || pickupLocationField.getText().trim().isEmpty()) {
            errorLabel2.setText("Name, Phone, and Pickup Map Location are required."); return;
        }
        if (!senderPhoneField.getText().trim().matches("^01\\d{9}$")) {
            errorLabel2.setText("Invalid Phone No."); return;
        }
        errorLabel2.setText(""); showBox(step3Box);
    }

    @FXML private void calculateAndGoToStep4(ActionEvent event) {
        if (receiverNameField.getText().trim().isEmpty() || receiverPhoneField.getText().trim().isEmpty() || dropoffLocationField.getText().trim().isEmpty()) {
            errorLabel3.setText("Name, Phone, and Delivery Map Location are required."); return;
        }
        if (!receiverPhoneField.getText().trim().matches("^01\\d{9}$")) {
            errorLabel3.setText("Invalid Phone No."); return;
        }
        errorLabel3.setText("");
        showBox(step4Box);

        loadingBox.setVisible(true); loadingBox.setManaged(true);
        summaryBox.setVisible(false); summaryBox.setManaged(false);
        confirmBtn.setDisable(true); backBtn4.setDisable(true);
        errorLabel4.setText("");

        String origin = pickupLocationField.getText().trim();
        String destination = dropoffLocationField.getText().trim();

        new Thread(() -> {
            try {
                String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
                String encodedDest = URLEncoder.encode(destination, StandardCharsets.UTF_8);
                String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + encodedOrigin + "&destinations=" + encodedDest + "&key=" + GOOGLE_API_KEY;

                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();

                Platform.runLater(() -> {
                    loadingBox.setVisible(false); loadingBox.setManaged(false);
                    backBtn4.setDisable(false);

                    if (json.contains("\"status\" : \"REQUEST_DENIED\"")) {
                        errorLabel4.setText("API Key Error: Billing not enabled or invalid key.");
                    } else if (!json.contains("\"status\" : \"OK\"") || json.contains("ZERO_RESULTS") || json.contains("NOT_FOUND")) {
                        errorLabel4.setText("Invalid Location. Maps could not find a route.");
                    } else {
                        Matcher m = Pattern.compile("\"distance\"\\s*:\\s*\\{\\s*\"text\"\\s*:\\s*\"[^\"]+\",\\s*\"value\"\\s*:\\s*(\\d+)").matcher(json);
                        if (m.find()) {
                            long distanceMeters = Long.parseLong(m.group(1));
                            finalDistanceKm = distanceMeters / 1000.0;
                            calculateDynamicFee();
                        } else {
                            errorLabel4.setText("Failed to parse distance from Map API.");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingBox.setVisible(false); loadingBox.setManaged(false);
                    backBtn4.setDisable(false);
                    errorLabel4.setText("Network error during API call.");
                });
            }
        }).start();
    }

    private void calculateDynamicFee() {
        double extraDistance = Math.max(0, finalDistanceKm - 1.0);
        double extraWeight = Math.max(0, finalWeight - 2.0);

        finalFee = Math.round((30.0 + (extraDistance * 10.0) + (extraWeight * 5.0)) * 100.0) / 100.0;
        finalCharge = Math.round((finalFee * 0.05) * 100.0) / 100.0;
        finalTotalCost = finalFee + finalCharge;

        distanceLabel.setText(String.format("Route Distance: %.2f km", finalDistanceKm));
        weightLabel.setText(String.format("Product Weight: %.2f kg", finalWeight));

        feeLabel.setText(String.format("Delivery Fee %.2f TK\nCharge %.2f TK\n\nTotal Deduction: %.2f TK", finalFee, finalCharge, finalTotalCost));

        LocalDatabaseManager.refreshCurrentUser();
        User currentUser = LocalDatabaseManager.getCurrentUser();

        if (currentUser.getBalance() < finalTotalCost) {
            balanceWarningLabel.setText("Insufficient balance! You need " + finalTotalCost + " TK. Add funds.");
            confirmBtn.setDisable(true);
        } else {
            balanceWarningLabel.setText(""); confirmBtn.setDisable(false);
        }
        summaryBox.setVisible(true); summaryBox.setManaged(true);
    }

    @FXML
    private void handleConfirmOrder(ActionEvent event) {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser.getBalance() < finalTotalCost) return;

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order newOrder = new Order(
                orderId, currentUser.getId(), null,
                itemTypeCombo.getValue(), itemNameField.getText().trim(), finalWeight,
                senderNameField.getText().trim(), senderPhoneField.getText().trim(), pickupLocationField.getText().trim(), pickupInfoField.getText().trim(),
                receiverNameField.getText().trim(), receiverPhoneField.getText().trim(), dropoffLocationField.getText().trim(), dropoffInfoField.getText().trim(),
                finalDistanceKm, finalFee, OrderStatus.PENDING
        );

        LocalDatabaseManager.createOrderWithEscrow(newOrder, finalTotalCost);

        errorLabel4.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        errorLabel4.setText("Order created & Funds held in Escrow!\nTrack NO: " + orderId);
        confirmBtn.setDisable(true);
        backBtn4.setDisable(true);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            Platform.runLater(() -> SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard"));
        }).start();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
    }
}