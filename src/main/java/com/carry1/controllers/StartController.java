package com.carry1.controllers;

import com.carry1.SceneManager;
import com.carry1.models.AuthFlowState;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class StartController {

    @FXML
    private void handleSignIn(ActionEvent event) {
        AuthFlowState.isSignUpMode = false;
        SceneManager.switchScene("RoleSelectionView.fxml", "Select Role");
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        AuthFlowState.isSignUpMode = true;
        SceneManager.switchScene("RoleSelectionView.fxml", "Select Role");
    }
}