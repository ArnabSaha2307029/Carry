package com.carry1.viewmodels;

import com.carry1.database.LocalDatabaseManager;
import com.carry1.models.AuthFlowState;
import com.carry1.models.Role;
import com.carry1.models.User;
import com.carry1.services.GlobalNotificationService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.application.Platform;

public class LoginViewModel {

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

    public StringProperty nameProperty() { return name; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty emailProperty() { return email; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public BooleanProperty isLoadingProperty() { return isLoading; }

    public void processAction(Runnable onSuccess) {
        if (phone.get().trim().isEmpty() || password.get().trim().isEmpty()) {
            errorMessage.set("Phone number and Password cannot be empty."); return;
        }

        isLoading.set(true); errorMessage.set("");

        new Thread(() -> {
            try {
                Thread.sleep(800);
                String phoneStr = phone.get().trim();
                String passStr = password.get().trim();
                Role role = AuthFlowState.selectedRole;
                String generatedId = role.name() + "-" + phoneStr;

                if (AuthFlowState.isSignUpMode) {
                    User newUser = new User(generatedId, name.get().trim(), role, 0.0, "token_" + generatedId, "ACTIVE");
                    boolean success = LocalDatabaseManager.registerUser(newUser, phoneStr, passStr, email.get().trim());
                    if (success) {
                        LocalDatabaseManager.saveSession(newUser);
                        GlobalNotificationService.start();
                        Platform.runLater(onSuccess);
                    } else {
                        Platform.runLater(() -> { errorMessage.set("Account already exists."); isLoading.set(false); });
                    }
                } else {
                    User user = LocalDatabaseManager.authenticateUser(phoneStr, passStr, role);
                    if (user != null) {
                        if ("BANNED".equals(user.getStatus())) {
                            Platform.runLater(() -> { errorMessage.set("Your account has been permanently BANNED."); isLoading.set(false); });
                            return;
                        }
                        LocalDatabaseManager.saveSession(user);
                        GlobalNotificationService.start();
                        Platform.runLater(onSuccess);
                    } else {
                        Platform.runLater(() -> { errorMessage.set("Invalid credentials."); isLoading.set(false); });
                    }
                }
            } catch (Throwable e) {
                Platform.runLater(() -> { System.out.println("LoginThread Error: " + e.getMessage()); e.printStackTrace(); errorMessage.set("An error occurred during authentication."); isLoading.set(false); });
            }
        }).start();
    }
}