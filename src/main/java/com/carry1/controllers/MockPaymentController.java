package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.Role;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Random;

public class MockPaymentController {

    @FXML private VBox step1Box, step2Box, step3Box, step4Box;

    // Step 1
    @FXML private TextField amountField;
    @FXML private Label errorLabel1;

    // Step 3
    @FXML private Label step3Header;
    @FXML private ComboBox<String> providerCombo;
    @FXML private TextField accountNoField;
    @FXML private Label errorLabel3;

    // Step 4
    @FXML private TextField otpField;
    @FXML private Label statusLabel;
    @FXML private Button confirmBtn, cancelOtpBtn;

    // Notification
    @FXML private Label notificationToast;

    // State Variables
    private double paymentAmount = 0.0;
    private String generatedOtp = "";

    @FXML
    public void initialize() {
        showBox(step1Box);
    }

    // --- STEP 1: Amount ---
    @FXML
    private void goToStep2(ActionEvent event) {
        String amountStr = amountField.getText().trim();
        if (amountStr.isEmpty()) {
            errorLabel1.setText("Please enter an amount.");
            return;
        }
        try {
            paymentAmount = Double.parseDouble(amountStr);
            if (paymentAmount <= 0) throw new NumberFormatException();
            errorLabel1.setText("");
            showBox(step2Box);
        } catch (NumberFormatException e) {
            errorLabel1.setText("Invalid amount.");
        }
    }

    @FXML private void goBackToStep1(ActionEvent event) { showBox(step1Box); }

    // --- STEP 2: Category ---
    @FXML
    private void selectMobileBanking(ActionEvent event) {
        step3Header.setText("Mobile Banking Details");
        providerCombo.getItems().clear();
        providerCombo.getItems().addAll("bKash", "Nagad", "Upay", "Rocket");
        accountNoField.setPromptText("Mobile Number");
        showBox(step3Box);
    }

    @FXML
    private void selectCard(ActionEvent event) {
        step3Header.setText("Card Details");
        providerCombo.getItems().clear();
        providerCombo.getItems().addAll("Mastercard", "Visa Card", "NexusPay", "Amex");
        accountNoField.setPromptText("Card Number");
        showBox(step3Box);
    }

    @FXML private void goBackToStep2(ActionEvent event) { showBox(step2Box); }

    // --- STEP 3: Details ---
    @FXML
    private void goToStep4(ActionEvent event) {
        if (providerCombo.getValue() == null || accountNoField.getText().trim().isEmpty()) {
            errorLabel3.setText("Please select a provider and enter number.");
            return;
        }
        errorLabel3.setText("");
        showBox(step4Box);
        triggerOtpSystem();
    }

    // --- STEP 4: OTP & Payment ---
    private void triggerOtpSystem() {
        // ৬ ডিজিটের র‍্যান্ডম OTP জেনারেট
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        generatedOtp = String.format("%06d", number);

        // ০.৫ সেকেন্ড পর নোটিফিকেশন দেখাবে
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            Platform.runLater(() -> {
                notificationToast.setText("System Message: Your OTP is " + generatedOtp);
                notificationToast.setVisible(true);

                // ৫ সেকেন্ড পর নোটিফিকেশন গায়েব হয়ে যাবে
                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) {}
                    Platform.runLater(() -> notificationToast.setVisible(false));
                }).start();
            });
        }).start();
    }

    @FXML
    private void verifyOtpAndPay(ActionEvent event) {
        String enteredOtp = otpField.getText().trim();

        if (enteredOtp.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please enter OTP.");
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Invalid OTP. Try again.");
            return;
        }

        // OTP ঠিক থাকলে ২ সেকেন্ডের API Delay শুরু
        confirmBtn.setDisable(true);
        cancelOtpBtn.setDisable(true);
        statusLabel.setStyle("-fx-text-fill: #2196F3;");
        statusLabel.setText("Processing payment...");

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            User currentUser = LocalDatabaseManager.getCurrentUser();
            boolean success = LocalDatabaseManager.addFundsToUser(currentUser.getId(), paymentAmount);

            Platform.runLater(() -> {
                if (success) {
                    LocalDatabaseManager.refreshCurrentUser();
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    statusLabel.setText("Payment Successful!");

                    new Thread(() -> {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        Platform.runLater(this::goBackToDashboard);
                    }).start();
                } else {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Payment Failed. DB Error.");
                    confirmBtn.setDisable(false);
                    cancelOtpBtn.setDisable(false);
                }
            });
        }).start();
    }

    // --- Utility ---
    private void showBox(VBox boxToShow) {
        step1Box.setVisible(false);
        step2Box.setVisible(false);
        step3Box.setVisible(false);
        step4Box.setVisible(false);
        boxToShow.setVisible(true);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        goBackToDashboard();
    }

    private void goBackToDashboard() {
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser.getRole() == Role.CUSTOMER) {
            SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
        } else {
            SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
        }
    }
}