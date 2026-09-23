package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import com.carry1.viewmodels.LoginViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.Random;

public class AuthFormController {

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Button forgotPasswordBtn;
    @FXML private Canvas captchaCanvas;
    @FXML private TextField captchaInputField;

    private LoginViewModel viewModel;
    private String currentCaptchaText;
    private final Random random = new Random();
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

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
            if (forgotPasswordBtn != null) forgotPasswordBtn.setVisible(false);
        } else {
            headerLabel.setText("Sign In - " + AuthFlowState.selectedRole.name());
            actionButton.setText("Login");
            nameField.setVisible(false);
            nameField.setManaged(false);
            if (forgotPasswordBtn != null) forgotPasswordBtn.setVisible(true);
        }

        viewModel.isLoadingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) actionButton.setText("Processing...");
            else actionButton.setText(AuthFlowState.isSignUpMode ? "Create Account" : "Login");
        });

        generateCaptcha();
    }

    private void generateCaptcha() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        currentCaptchaText = sb.toString();
        renderCaptcha();
    }

    private void renderCaptcha() {
        GraphicsContext gc = captchaCanvas.getGraphicsContext2D();
        double w = captchaCanvas.getWidth();
        double h = captchaCanvas.getHeight();

        gc.setFill(Color.rgb(240, 240, 245));
        gc.fillRect(0, 0, w, h);

        for (int i = 0; i < 8; i++) {
            gc.setStroke(Color.rgb(
                random.nextInt(100) + 100,
                random.nextInt(100) + 100,
                random.nextInt(100) + 100
            ));
            gc.setLineWidth(random.nextDouble() * 1.5 + 0.5);
            gc.strokeLine(
                random.nextDouble() * w,
                random.nextDouble() * h,
                random.nextDouble() * w,
                random.nextDouble() * h
            );
        }

        for (int i = 0; i < 15; i++) {
            gc.setFill(Color.rgb(
                random.nextInt(120) + 80,
                random.nextInt(120) + 80,
                random.nextInt(120) + 80
            ));
            double dotSize = random.nextDouble() * 3 + 1;
            gc.fillOval(random.nextDouble() * w, random.nextDouble() * h, dotSize, dotSize);
        }

        double charSlotWidth = w / currentCaptchaText.length();

        for (int i = 0; i < currentCaptchaText.length(); i++) {
            String ch = String.valueOf(currentCaptchaText.charAt(i));
            double fontSize = random.nextInt(9) + 24;
            FontPosture posture = (i % 2 == 0) ? FontPosture.ITALIC : FontPosture.REGULAR;
            gc.setFont(Font.font("Arial", FontWeight.BOLD, posture, fontSize));
            gc.setFill(Color.rgb(
                random.nextInt(60),
                random.nextInt(60),
                random.nextInt(60) + 20
            ));
            double angle = (random.nextDouble() * 40) - 20;
            double cx = charSlotWidth * i + charSlotWidth / 2;
            double cy = h / 2 + (random.nextDouble() * 10) - 5;
            gc.save();
            gc.translate(cx, cy);
            gc.rotate(angle);
            gc.fillText(ch, -fontSize / 4, fontSize / 3);
            gc.restore();
        }
    }

    @FXML
    private void handleRefreshCaptcha(ActionEvent event) {
        captchaInputField.clear();
        generateCaptcha();
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        String userInput = captchaInputField.getText().trim().toUpperCase();

        if (userInput.isEmpty()) {
            errorLabel.textProperty().unbind();
            errorLabel.setText("Please enter the CAPTCHA code.");
            return;
        }

        if (!userInput.equals(currentCaptchaText)) {
            errorLabel.textProperty().unbind();
            errorLabel.setText("Incorrect CAPTCHA. Please try again.");
            captchaInputField.clear();
            generateCaptcha();
            return;
        }

        errorLabel.textProperty().bind(viewModel.errorMessageProperty());

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