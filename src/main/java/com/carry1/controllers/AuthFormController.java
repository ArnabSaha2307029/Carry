package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import com.carry1.viewmodels.LoginViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthFormController {

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Button forgotPasswordBtn; // New Button Reference

    private LoginViewModel viewModel;

    @FXML
    public void initialize() {
        viewModel = new LoginViewModel();

        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        phoneField.textProperty().bindBidirectional(viewModel.phoneProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        actionButton.disableProperty().bind(viewModel.isLoadingProperty());

        if (AuthFlowState.isSignUpMode) {
            headerLabel.setText("Sign Up - " + AuthFlowState.selectedRole.name());
            actionButton.setText("Create Account");
            if(forgotPasswordBtn != null) forgotPasswordBtn.setVisible(false); // সাইন আপে ফরগট পাসওয়ার্ড দেখাবে না
        } else {
            headerLabel.setText("Sign In - " + AuthFlowState.selectedRole.name());
            actionButton.setText("Login");
            nameField.setVisible(false);
            nameField.setManaged(false);
            if(forgotPasswordBtn != null) forgotPasswordBtn.setVisible(true);
        }

        viewModel.isLoadingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) actionButton.setText("Processing...");
            else actionButton.setText(AuthFlowState.isSignUpMode ? "Create Account" : "Login");
        });
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        viewModel.processAction(() -> {
            Role userRole = AuthFlowState.selectedRole;
            AuthFlowState.clear();

            if (userRole == Role.CUSTOMER) {
                SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
            } else if (userRole == Role.TRAVELER) {
                SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
            } else if (userRole == Role.ADMIN) {
                SceneManager.switchScene("AdminDashboardView.fxml", "Carry1 - Admin Control Panel");
            }
        });
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) {
        SceneManager.switchScene("ForgotPasswordView.fxml", "Carry1 - Reset Password");
    }

    @FXML
    private void goBack(ActionEvent event) {
        SceneManager.switchScene("RoleSelectionView.fxml", "Select Role");
    }
}