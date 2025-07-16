package com.example.weatherdashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class WeatherApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(WeatherApp.class.getResource("weather-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Load the CSS stylesheet and apply it to the scene
        String css = Objects.requireNonNull(this.getClass().getResource("style.css")).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Weather Dashboard");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main (String[] args) {
        launch();
    }
}