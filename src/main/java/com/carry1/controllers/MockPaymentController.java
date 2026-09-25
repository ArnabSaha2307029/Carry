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
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.util.Random;

public class MockPaymentController {

    @FXML private VBox step1Box, step2Box, step3Box, step4Box;

    
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

    
    @FXML private TextField otpField;
    @FXML private Label statusLabel;
    @FXML private Button confirmBtn, cancelOtpBtn;

    
    @FXML private Label notificationToast;

    
    private double paymentAmount = 0.0;
    private String generatedOtp = "";

    @FXML
    public void initialize() {
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
            paymentAmount = Double.parseDouble(amountStr);
            if (paymentAmount <= 0) throw new NumberFormatException();
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
        triggerOtpSystem();
    }

    
    private void triggerOtpSystem() {
        
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        generatedOtp = String.format("%06d", number);

        
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            Platform.runLater(() -> {
                notificationToast.setText("System Message: Your OTP is " + generatedOtp);
                notificationToast.setVisible(true);

                
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
            statusLabel.setStyle("-fx-text-fill: #D32F2F;");
            statusLabel.setText("Please enter OTP.");
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            statusLabel.setStyle("-fx-text-fill: #D32F2F;");
            statusLabel.setText("Invalid OTP. Try again.");
            return;
        }

        
        confirmBtn.setDisable(true);
        cancelOtpBtn.setDisable(true);
        statusLabel.setStyle("-fx-text-fill: #D32F2F;");
        statusLabel.setText("Processing payment...");

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            User currentUser = LocalDatabaseManager.getCurrentUser();
            boolean success = LocalDatabaseManager.addFundsToUser(currentUser.getId(), paymentAmount);

            Platform.runLater(() -> {
                if (success) {
                    LocalDatabaseManager.refreshCurrentUser();
                    statusLabel.setStyle("-fx-text-fill: #D32F2F;");
                    statusLabel.setText("Payment Successful!");

                    new Thread(() -> {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        Platform.runLater(this::goBackToDashboard);
                    }).start();
                } else {
                    statusLabel.setStyle("-fx-text-fill: #D32F2F;");
                    statusLabel.setText("Payment Failed. DB Error.");
                    confirmBtn.setDisable(false);
                    cancelOtpBtn.setDisable(false);
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
        User currentUser = LocalDatabaseManager.getCurrentUser();
        if (currentUser.getRole() == Role.CUSTOMER) {
            SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
        } else {
            SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
        }
    }
}