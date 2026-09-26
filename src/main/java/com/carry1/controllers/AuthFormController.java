package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import com.carry1.viewmodels.LoginViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthFormController {

    @FXML private HBox mainFormBox;
    @FXML private VBox otpVerificationBox;
    @FXML private Label notificationToast;

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Button forgotPasswordBtn;
    @FXML private Canvas captchaCanvas;
    @FXML private TextField captchaInputField;

    @FXML private TextField rollField;
    @FXML private TextField studentEmailField;
    @FXML private TextField departmentField;
    @FXML private TextField hallField;

    @FXML private TextField otpInputField;
    @FXML private Label otpErrorLabel;
    @FXML private Button verifyOtpBtn;
    @FXML private Button cancelOtpBtn;

    private LoginViewModel viewModel;
    private String currentCaptchaText;
    private final Random random = new Random();
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final String PHONE_REGEX = "^01\\d{9}$";
    private static final String ROLL_REGEX = "^\\d{7}$";
    private static final String EMAIL_REGEX = "^[a-zA-Z]+(\\d{7})@stud\\.kuet\\.ac\\.bd$";

    private String currentOtp;
    private String capturedRoll;
    private String capturedDept;
    private String capturedHall;
    private int capturedGradYear;

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
            if (forgotPasswordBtn != null) {
                if (AuthFlowState.selectedRole == Role.ADMIN) {
                    forgotPasswordBtn.setVisible(false);
                    forgotPasswordBtn.setManaged(false);
                } else {
                    forgotPasswordBtn.setVisible(true);
                    forgotPasswordBtn.setManaged(true);
                }
            }
        }

        viewModel.isLoadingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) actionButton.setText("Processing...");
            else actionButton.setText(AuthFlowState.isSignUpMode ? "Create Account" : "Login");
        });

        if (AuthFlowState.selectedRole == Role.ADMIN) {
            phoneField.setPromptText("Admin ID (adm)");
        }

        if (AuthFlowState.isSignUpMode && AuthFlowState.selectedRole == Role.TRAVELER) {
            rollField.setVisible(true); rollField.setManaged(true);
            studentEmailField.setVisible(true); studentEmailField.setManaged(true);
            departmentField.setVisible(true); departmentField.setManaged(true);
            hallField.setVisible(true); hallField.setManaged(true);
        }

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

    private void showError(String message) {
        errorLabel.textProperty().unbind();
        errorLabel.setText(message);
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        String captchaInput = captchaInputField.getText().trim().toUpperCase();

        if (captchaInput.isEmpty()) {
            showError("Please enter the CAPTCHA code.");
            return;
        }

        if (!captchaInput.equals(currentCaptchaText)) {
            showError("Incorrect CAPTCHA. Please try again.");
            captchaInputField.clear();
            generateCaptcha();
            return;
        }

        String phoneText = phoneField.getText().trim();
        if (AuthFlowState.selectedRole != Role.ADMIN && !phoneText.isEmpty() && !phoneText.matches(PHONE_REGEX)) {
            showError("Invalid Phone No.");
            return;
        }

        if (AuthFlowState.isSignUpMode && AuthFlowState.selectedRole == Role.TRAVELER) {
            String roll = rollField.getText().trim();
            String email = studentEmailField.getText().trim();
            String dept = departmentField.getText().trim();
            String hall = hallField.getText().trim();

            if (roll.isEmpty() || email.isEmpty() || dept.isEmpty() || hall.isEmpty()) {
                showError("All student verification fields are required.");
                return;
            }

            if (!roll.matches(ROLL_REGEX)) {
                showError("Invalid Roll No.");
                return;
            }

            Matcher emailMatcher = Pattern.compile(EMAIL_REGEX).matcher(email);
            if (!emailMatcher.matches()) {
                showError("Invalid Email Address.");
                return;
            }

            String emailRollDigits = emailMatcher.group(1);
            if (!emailRollDigits.equals(roll)) {
                showError("Invalid Email Address.");
                return;
            }

            int admissionYear = 2000 + Integer.parseInt(roll.substring(0, 2));
            int gradYear = admissionYear + 5;
            LocalDate expiryDate = LocalDate.of(gradYear, 12, 31);

            if (LocalDate.now().isAfter(expiryDate)) {
                showError("Student Id validity expired.");
                return;
            }

            capturedRoll = roll;
            capturedDept = dept;
            capturedHall = hall;
            capturedGradYear = gradYear;

            mainFormBox.setVisible(false);
            otpVerificationBox.setVisible(true);
            triggerOtpSystem();
            return;
        }

        errorLabel.textProperty().bind(viewModel.errorMessageProperty());

        viewModel.processAction(() -> {
            Platform.runLater(() -> {
                try {
                    Role userRole = AuthFlowState.selectedRole;
                    AuthFlowState.clear();

                    if (userRole == Role.CUSTOMER) {
                        SceneManager.switchScene("CustomerDashboardView.fxml", "Carry1 - Customer Dashboard");
                    } else if (userRole == Role.TRAVELER) {
                        SceneManager.switchScene("TravelerDashboardView.fxml", "Carry1 - Traveler Dashboard");
                    } else if (userRole == Role.ADMIN) {
                        SceneManager.switchScene("AdminDashboardView.fxml", "Carry1 - Admin Dashboard");
                    }
                } catch (Throwable e) {
                    System.out.println("Scene Switch Error: " + e.getMessage()); e.printStackTrace(); viewModel.isLoadingProperty().set(false);
                }
            });
        });
    }

    private void triggerOtpSystem() {
        int number = random.nextInt(999999);
        currentOtp = String.format("%06d", number);

        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            Platform.runLater(() -> {
                notificationToast.setText("System Message: Your OTP is " + currentOtp);
                notificationToast.setVisible(true);

                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) {}
                    Platform.runLater(() -> notificationToast.setVisible(false));
                }).start();
            });
        }).start();
    }

    @FXML
    private void handleVerifyOtp(ActionEvent event) {
        String enteredOtp = otpInputField.getText().trim();

        if (enteredOtp.isEmpty()) {
            otpErrorLabel.setStyle("-fx-text-fill: red;");
            otpErrorLabel.setText("Please enter OTP.");
            return;
        }

        if (!enteredOtp.equals(currentOtp)) {
            otpErrorLabel.setStyle("-fx-text-fill: red;");
            otpErrorLabel.setText("Invalid OTP. Try again.");
            return;
        }
        
        verifyOtpBtn.setDisable(true);
        cancelOtpBtn.setDisable(true);
        otpErrorLabel.setStyle("-fx-text-fill: #2196F3;");
        otpErrorLabel.setText("Verification successful. Processing...");

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            Platform.runLater(() -> {
                otpVerificationBox.setVisible(false);
                mainFormBox.setVisible(true);
                errorLabel.textProperty().bind(viewModel.errorMessageProperty());

                viewModel.processAction(() -> {
                    LocalDatabaseManager.saveTravelerProfile(
                        LocalDatabaseManager.getCurrentUser().getId(),
                        capturedRoll, capturedDept, capturedHall, capturedGradYear
                    );
                    AuthFlowState.clear();
                    SceneManager.switchScene("StartView.fxml", "Carry1 - Start");
                });
            });
        }).start();
    }

    @FXML
    private void handleCancelOtp(ActionEvent event) {
        otpVerificationBox.setVisible(false);
        mainFormBox.setVisible(true);
        otpInputField.clear();
        otpErrorLabel.setText("");
        captchaInputField.clear();
        generateCaptcha();
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