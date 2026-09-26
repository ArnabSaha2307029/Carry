package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Random;

public class ForgotPasswordController {

    @FXML private VBox step1Box, step2Box;
    @FXML private Label roleLabel, errorLabel1, errorLabel2, notificationToast;
    @FXML private TextField phoneField, otpField;
    @FXML private PasswordField newPasswordField;

    private String generatedOtp = null;
    private Role currentRole;

    @FXML
    public void initialize() {
        currentRole = AuthFlowState.selectedRole;
        if (currentRole != null) {
            roleLabel.setText("Role: " + currentRole.name());
        } else {
            currentRole = Role.CUSTOMER;
            roleLabel.setText("Role: CUSTOMER");
        }
        step1Box.setVisible(true);
        step2Box.setVisible(false);
    }

    @FXML
    private void handleSendOtp(ActionEvent event) {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            errorLabel1.setText("Please enter your phone number.");
            return;
        }

        if (!phone.matches("^01\\d{9}$")) {
            errorLabel1.setText("Invalid Phone No.");
            return;
        }

        boolean exists = LocalDatabaseManager.checkUserExists(phone, currentRole);
        if (!exists) {
            errorLabel1.setText("No account found with this phone number.");
            return;
        }

        errorLabel1.setText("");
        generatedOtp = String.format("%04d", new Random().nextInt(10000));

        
        step1Box.setVisible(false);
        step2Box.setVisible(true);

        showToast("Mock SMS: Your OTP is " + generatedOtp);
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String enteredOtp = otpField.getText().trim();
        String newPassword = newPasswordField.getText().trim();

        if (enteredOtp.isEmpty() || newPassword.isEmpty()) {
            errorLabel2.setText("OTP and New Password cannot be empty.");
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            errorLabel2.setText("Invalid OTP! Try again.");
            return;
        }

        boolean success = LocalDatabaseManager.updateUserPassword(phoneField.getText().trim(), currentRole, newPassword);

        if (success) {
            showToast("Password reset successfully! Redirecting...");

            
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
                Platform.runLater(() -> SceneManager.switchScene("AuthFormView.fxml", "Carry1 - Login"));
            }).start();
        } else {
            errorLabel2.setText("Database error. Failed to reset password.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        SceneManager.switchScene("AuthFormView.fxml", "Carry1 - Login");
    }

    private void showToast(String message) {
        notificationToast.setText(message);
        notificationToast.setVisible(true);
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (InterruptedException e) {}
            Platform.runLater(() -> notificationToast.setVisible(false));
        }).start();
    }
}