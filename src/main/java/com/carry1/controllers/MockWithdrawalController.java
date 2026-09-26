package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class MockWithdrawalController {

    @FXML private VBox step1Box, step2Box, step3Box, step4Box, confirmBox;

    // Step 1
    @FXML private Label currentBalanceLabel;
    @FXML private TextField amountField;
    @FXML private Label errorLabel1;

    // Step 3
    @FXML private Label step3Header;
    @FXML private ComboBox<String> providerCombo;
    @FXML private TextField accountNoField;
    @FXML private Label errorLabel3;

    // Step 4
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button verifyBtn;

    private double withdrawalAmount = 0.0;
    private User currentUser;

    @FXML
    public void initialize() {
        LocalDatabaseManager.refreshCurrentUser();
        currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser != null) {
            currentBalanceLabel.setText("Available Balance: " + currentUser.getBalance() + " TK");
        }
        confirmBox.setVisible(false);
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
            withdrawalAmount = Double.parseDouble(amountStr);
            if (withdrawalAmount <= 0) throw new NumberFormatException();

            if (withdrawalAmount > currentUser.getBalance()) {
                errorLabel1.setText("Insufficient balance for withdrawal.");
                return;
            }

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
    }

    @FXML private void goBackToStep3(ActionEvent event) { showBox(step3Box); }

    // --- STEP 4: Password Verification ---
    @FXML
    private void verifyPassword(ActionEvent event) {
        String pass = passwordField.getText();
        if (pass.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Password is required.");
            return;
        }

        // ডাটাবেস থেকে পাসওয়ার্ড ভেরিফাই
        if (!LocalDatabaseManager.verifyUserPassword(currentUser.getId(), pass)) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Incorrect password. Please try again.");
            return;
        }

        // ওভারলে পপ-আপ দেখানো হচ্ছে
        verifyBtn.setDisable(true);
        confirmBox.setVisible(true);
    }

    @FXML
    private void cancelConfirmation(ActionEvent event) {
        confirmBox.setVisible(false);
        verifyBtn.setDisable(false);
    }

    @FXML
    private void processFinalWithdrawal(ActionEvent event) {
        confirmBox.setVisible(false); // পপ-আপ লুকানো হচ্ছে
        statusLabel.setStyle("-fx-text-fill: #2196F3;");
        statusLabel.setText("Processing transfer...");

        // ২ সেকেন্ডের API Delay
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            boolean success = LocalDatabaseManager.withdrawFundsFromUser(currentUser.getId(), withdrawalAmount);

            Platform.runLater(() -> {
                if (success) {
                    LocalDatabaseManager.refreshCurrentUser();
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    statusLabel.setText("Fund transferred successfully!");

                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException e) {}
                        Platform.runLater(this::goBackToDashboard);
                    }).start();
                } else {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Transfer Failed. DB Error.");
                    verifyBtn.setDisable(false);
                }
            });
        }).start();
    }

    // --- Utility Method ---
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
        SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
    }
}