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
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

public class MockWithdrawalController {

    @FXML private VBox step1Box, step2Box, step3Box, step4Box, confirmBox;

    
    @FXML private Label currentBalanceLabel;
    @FXML private TextField amountField;
    @FXML private Label errorLabel1;

    
    @FXML private Label step3Header;
            private boolean isCardMode = false;
    @FXML private ImageView method1Image, method2Image, method3Image, method4Image;

    private void safeLoadImage(ImageView imageView, String imagePath) {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                imageView.setImage(new Image(stream));
            } else {
                System.err.println("Warning: Image not found at " + imagePath);
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + imagePath);
        }
    }

    private String selectedMethod = null;
    @FXML private VBox method1Box, method2Box, method3Box, method4Box;

    @FXML private void selectMethod1() { setSelection(isCardMode ? "Visa" : "bKash", method1Box, method2Box, method3Box, method4Box); }
    @FXML private void selectMethod2() { setSelection(isCardMode ? "MasterCard" : "Nagad", method2Box, method1Box, method3Box, method4Box); }
    @FXML private void selectMethod3() { setSelection(isCardMode ? "NexusPay" : "Rocket", method3Box, method1Box, method2Box, method4Box); }
    @FXML private void selectMethod4() { setSelection(isCardMode ? "Amex" : "Upay", method4Box, method1Box, method2Box, method3Box); }

    private void setSelection(String method, VBox selected, VBox... others) {
        selectedMethod = method;
        selected.setStyle("-fx-border-color: #D32F2F; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-background-color: #FFCDD2; -fx-cursor: hand;");
        for (VBox box : others) {
            box.setStyle("-fx-border-color: transparent; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-cursor: hand;");
        }
    }

    private void resetSelection() {
        selectedMethod = null;
        if (method1Box != null) {
            method1Box.setStyle("-fx-border-color: transparent; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-cursor: hand;");
            method2Box.setStyle("-fx-border-color: transparent; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-cursor: hand;");
            method3Box.setStyle("-fx-border-color: transparent; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-cursor: hand;");
            method4Box.setStyle("-fx-border-color: transparent; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5; -fx-cursor: hand;");
        }
    }
    @FXML private TextField accountNoField;
    @FXML private Label errorLabel3;

    
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

    
    @FXML
    private void selectMobileBanking(ActionEvent event) {
        isCardMode = false;
        safeLoadImage(method1Image, "/bkash.png");
        safeLoadImage(method2Image, "/nagad.png");
        safeLoadImage(method3Image, "/rocket.png");
        safeLoadImage(method4Image, "/upay.png");

        step3Header.setText("Mobile Banking Details");
        resetSelection();
        accountNoField.setPromptText("Mobile Number");
        showBox(step3Box);
    }

    @FXML
    private void selectCard(ActionEvent event) {
        isCardMode = true;
        safeLoadImage(method1Image, "/visa.png");
        safeLoadImage(method2Image, "/master.png");
        safeLoadImage(method3Image, "/nexuspay.png");
        safeLoadImage(method4Image, "/amex.png");

        step3Header.setText("Card Details");
        resetSelection();
        accountNoField.setPromptText("Card Number");
        showBox(step3Box);
    }

    @FXML private void goBackToStep2(ActionEvent event) { showBox(step2Box); }

    
    @FXML
    private void goToStep4(ActionEvent event) {
        if (selectedMethod == null || accountNoField.getText().trim().isEmpty()) {
            errorLabel3.setText("Please select a provider and enter number.");
            return;
        }
        errorLabel3.setText("");
        showBox(step4Box);
    }

    @FXML private void goBackToStep3(ActionEvent event) { showBox(step3Box); }

    
    @FXML
    private void verifyPassword(ActionEvent event) {
        String pass = passwordField.getText();
        if (pass.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #D32F2F;");
            statusLabel.setText("Password is required.");
            return;
        }

        
        if (!LocalDatabaseManager.verifyUserPassword(currentUser.getId(), pass)) {
            statusLabel.setStyle("-fx-text-fill: #D32F2F;");
            statusLabel.setText("Incorrect password. Please try again.");
            return;
        }

        
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
        confirmBox.setVisible(false); 
        statusLabel.setStyle("-fx-text-fill: #D32F2F;");
        statusLabel.setText("Processing transfer...");

        
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            boolean success = LocalDatabaseManager.withdrawFundsFromUser(currentUser.getId(), withdrawalAmount);

            Platform.runLater(() -> {
                if (success) {
                    LocalDatabaseManager.refreshCurrentUser();
                    statusLabel.setStyle("-fx-text-fill: #D32F2F;");
                    statusLabel.setText("Fund transferred successfully!");

                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException e) {}
                        Platform.runLater(this::goBackToDashboard);
                    }).start();
                } else {
                    statusLabel.setStyle("-fx-text-fill: #D32F2F;");
                    statusLabel.setText("Transfer Failed. DB Error.");
                    verifyBtn.setDisable(false);
                }
            });
        }).start();
    }

    
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