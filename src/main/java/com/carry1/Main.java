package com.carry1;

import com.carry1.database.LocalDatabaseManager;
import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        LocalDatabaseManager.initializeDatabase();

        SceneManager.setMainStage(stage);

        // অ্যাপ চালু হওয়ার সাথে সাথেই ডিফল্টভাবে ফুল স্ক্রিন (Maximized) করে দেবে
        stage.setMaximized(true);

        stage.show();

        SceneManager.switchScene("StartView.fxml", "Carry1 - Start");
    }

    public static void main(String[] args) {
        launch();
    }
}