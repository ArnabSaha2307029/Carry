package com.carry1;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {
    private static Stage mainStage;

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void switchScene(String fxmlFileName, String title) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(SceneManager.class.getResource("views/" + fxmlFileName));
            Parent root = fxmlLoader.load();

            if (mainStage.getScene() == null) {
                mainStage.setScene(new Scene(root, 800, 600));
            } else {
                mainStage.getScene().setRoot(root);
            }

            mainStage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Failed to load scene: " + fxmlFileName);
            e.printStackTrace();
        }
    }
}