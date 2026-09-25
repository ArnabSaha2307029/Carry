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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class CreateOrderController {

    private static final String MAPBOX_API_KEY = "pk.eyJ1IjoiYXJuYWIyOSIsImEiOiJjbXVrMzQ3N3cxNWM0MnhzOWZhOGowYmNjIn0.2hy9zzYNgJZ0cQ6p5vwIBQ";

    @FXML private VBox step1Box, step2Box, step3Box, step4Box;

    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private TextField itemNameField, itemWeightField;
    @FXML private Label errorLabel1;

    @FXML private TextField senderNameField, senderPhoneField, pickupInfoField;
    @FXML private ComboBox<String> pickupLocationCombo;
    @FXML private Label errorLabel2;

    @FXML private TextField receiverNameField, receiverPhoneField, dropoffInfoField;
    @FXML private ComboBox<String> dropoffLocationCombo;
    @FXML private Label errorLabel3;

    @FXML private VBox loadingBox, summaryBox;
    @FXML private Label distanceLabel, weightLabel, feeLabel, balanceWarningLabel, errorLabel4;
    @FXML private Button backBtn4, confirmBtn;

    private double finalWeight = 0;
    private double finalDistanceKm = 0;
    private double finalFee = 0;
    private double finalCharge = 0;
    private double finalTotalCost = 0;

    private Map<String, double[]> locationMap = new HashMap<>();

    @FXML
    public void initialize() {
        itemTypeCombo.getItems().addAll("Documents", "Food", "Other");
        itemTypeCombo.getSelectionModel().selectFirst();

        try (InputStream is = getClass().getResourceAsStream("/khulna_locations.json")) {
            if (is != null) {
                String raw = new Scanner(is).useDelimiter("\\A").next();
                JSONObject root = new JSONObject(raw);
                JSONArray locations = root.getJSONArray("khulna_locations");
                for (int i = 0; i < locations.length(); i++) {
                    JSONObject loc = locations.getJSONObject(i);
                    String name = loc.getString("name");
                    double lon = loc.getDouble("lon");
                    double lat = loc.getDouble("lat");
                    locationMap.put(name, new double[]{lon, lat});
                    pickupLocationCombo.getItems().add(name);
                    dropoffLocationCombo.getItems().add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        if (senderNameField.getText().trim().isEmpty() || senderPhoneField.getText().trim().isEmpty() || pickupLocationCombo.getValue() == null) {
            errorLabel2.setText("Name, Phone, and Pickup Location are required."); return;
        }
        if (!senderPhoneField.getText().trim().matches("^01\\d{9}$")) {
            errorLabel2.setText("Invalid Phone No."); return;
        }
        errorLabel2.setText(""); showBox(step3Box);
    }

    @FXML private void calculateAndGoToStep4(ActionEvent event) {
        if (receiverNameField.getText().trim().isEmpty() || receiverPhoneField.getText().trim().isEmpty() || dropoffLocationCombo.getValue() == null) {
            errorLabel3.setText("Name, Phone, and Delivery Location are required."); return;
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

        String origin = pickupLocationCombo.getValue();
        String destination = dropoffLocationCombo.getValue();

        new Thread(() -> {
            try {
                double[] originCoords = locationMap.get(origin);
                double[] destCoords = locationMap.get(destination);

                if (originCoords == null || destCoords == null) {
                    Platform.runLater(() -> {
                        loadingBox.setVisible(false); loadingBox.setManaged(false);
                        backBtn4.setDisable(false);
                        errorLabel4.setText("Invalid location selected.");
                    });
                    return;
                }

                double originLon = originCoords[0];
                double originLat = originCoords[1];
                double destLon = destCoords[0];
                double destLat = destCoords[1];

                String directionsUrl = "https://api.mapbox.com/directions/v5/mapbox/driving/"
                        + originLon + "," + originLat + ";"
                        + destLon + "," + destLat
                        + "?access_token=" + MAPBOX_API_KEY;

                System.out.println("Requesting Mapbox URL: " + directionsUrl);

                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest dirReq = HttpRequest.newBuilder().uri(URI.create(directionsUrl)).GET().build();
                HttpResponse<String> dirResp = client.send(dirReq, HttpResponse.BodyHandlers.ofString());
                System.out.println("Mapbox Response: " + dirResp.body());
                JSONObject dirJson = new JSONObject(dirResp.body());
                JSONArray routes = dirJson.getJSONArray("routes");

                if (routes.isEmpty()) {
                    Platform.runLater(() -> {
                        loadingBox.setVisible(false); loadingBox.setManaged(false);
                        backBtn4.setDisable(false);
                        errorLabel4.setText("Invalid Location. Maps could not find a route.");
                    });
                    return;
                }

                double distanceMeters = routes.getJSONObject(0).getDouble("distance");
                finalDistanceKm = distanceMeters / 1000.0;

                Platform.runLater(() -> {
                    loadingBox.setVisible(false); loadingBox.setManaged(false);
                    backBtn4.setDisable(false);
                    calculateDynamicFee();
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("CRASH REASON: " + e.getMessage());
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
                senderNameField.getText().trim(), senderPhoneField.getText().trim(), pickupLocationCombo.getValue(), pickupInfoField.getText().trim(),
                receiverNameField.getText().trim(), receiverPhoneField.getText().trim(), dropoffLocationCombo.getValue(), dropoffInfoField.getText().trim(),
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