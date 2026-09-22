package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class RoleSelectionController {

    @FXML private Label titleLabel;
    @FXML private Button adminButton;

    @FXML
    public void initialize() {
        if (AuthFlowState.isSignUpMode) {
            titleLabel.setText("Sign Up as:");
            // Sign Up এর সময় Admin বাটন গায়েব করে দেওয়া হলো
            adminButton.setVisible(false);
            adminButton.setManaged(false);
        } else {
            titleLabel.setText("Sign In as:");
        }
    }

    @FXML private void selectCustomer(ActionEvent event) { proceed(Role.CUSTOMER); }
    @FXML private void selectTraveler(ActionEvent event) { proceed(Role.TRAVELER); }
    @FXML private void selectAdmin(ActionEvent event) { proceed(Role.ADMIN); }

    private void proceed(Role role) {
        AuthFlowState.selectedRole = role;
        SceneManager.switchScene("AuthFormView.fxml", "Enter Credentials");
    }

    @FXML
    private void goBack(ActionEvent event) {
        AuthFlowState.clear();
        SceneManager.switchScene("StartView.fxml", "Carry1 - Start");
    }
}