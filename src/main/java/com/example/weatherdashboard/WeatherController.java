package com.example.weatherdashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WeatherController {

    @FXML private TextField cityTextField;
    @FXML private Label errorLabel;
    @FXML private VBox weatherDetailsPane;
    @FXML private Label locationLabel;
    @FXML private ImageView weatherIcon;
    @FXML private Label temperatureLabel;
    @FXML private Label conditionLabel;
    @FXML private Label humidityLabel;
    @FXML private VBox forecastPane;
    @FXML private HBox forecastContainer;

    private final WeatherService weatherService = new WeatherService();
    private static final String ICON_URL_PREFIX = "https://openweathermap.org/img/wn/";
    private static final String ICON_URL_SUFFIX = "@2x.png";

    @FXML
    public void initialize() {
        // Hide details until a successful search
        clearUI();
    }

    @FXML
    protected void handleSearchAction() {
        String city = cityTextField.getText();
        if (city == null || city.trim().isEmpty()) {
            showError("City name cannot be empty.");
            return;
        }
        clearUI();

        // Asynchronously fetch both current weather and forecast data
        weatherService.getWeatherData(city).thenAccept(this::updateCurrentWeatherUI)
                .exceptionally(this::handleApiError);

        weatherService.getForecastData(city).thenAccept(this::updateForecastUI)
                .exceptionally(this::handleApiError);
    }

    private void updateCurrentWeatherUI(JsonObject data) {
        Platform.runLater(() -> {
            weatherDetailsPane.setVisible(true);
            weatherDetailsPane.setManaged(true);

            String cityName = data.get("name").getAsString();
            String country = data.getAsJsonObject("sys").get("country").getAsString();
            locationLabel.setText(cityName + ", " + country);

            JsonObject main = data.getAsJsonObject("main");
            temperatureLabel.setText(String.format("%.0f°C", main.get("temp").getAsDouble()));
            humidityLabel.setText("Humidity: " + main.get("humidity").getAsInt() + "%");

            JsonObject weather = data.getAsJsonArray("weather").get(0).getAsJsonObject();
            String description = weather.get("description").getAsString();
            conditionLabel.setText(capitalize(description));

            String iconCode = weather.get("icon").getAsString();
            weatherIcon.setImage(new Image(ICON_URL_PREFIX + iconCode + ICON_URL_SUFFIX));
        });
    }

    private void updateForecastUI(JsonObject data) {
        Platform.runLater(() -> {
            forecastPane.setVisible(true);
            forecastPane.setManaged(true);
            forecastContainer.getChildren().clear();

            JsonArray forecastList = data.getAsJsonArray("list");
            List<JsonObject> dailyForecasts = new ArrayList<>();
            for (int i = 0; i < forecastList.size(); i += 8) { // API provides data every 3 hours, so 8 intervals = 24 hours
                dailyForecasts.add(forecastList.get(i).getAsJsonObject());
            }

            for (JsonObject forecastItem : dailyForecasts) {
                VBox forecastBox = createForecastBox(forecastItem);
                forecastContainer.getChildren().add(forecastBox);
            }
        });
    }

    private VBox createForecastBox(JsonObject forecastData) {
        long dt = forecastData.get("dt").getAsLong();
        String day = Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE"));

        JsonObject main = forecastData.getAsJsonObject("main");
        String temp = String.format("%.0f°C", main.get("temp").getAsDouble());

        JsonObject weather = forecastData.getAsJsonArray("weather").get(0).getAsJsonObject();
        String iconCode = weather.get("icon").getAsString();

        Label dayLabel = new Label(day);
        dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        ImageView iconView = new ImageView(new Image(ICON_URL_PREFIX + iconCode + ICON_URL_SUFFIX));
        iconView.setFitHeight(40);
        iconView.setFitWidth(40);

        Label tempLabel = new Label(temp);
        tempLabel.setStyle("-fx-text-fill: white;");

        VBox box = new VBox(5, dayLabel, iconView, tempLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("forecast-box");
        return box;
    }

    private Void handleApiError(Throwable error) {
        showError(error.getCause().getMessage());
        return null; // Required for exceptionally()
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            weatherDetailsPane.setVisible(false);
            weatherDetailsPane.setManaged(false);
            forecastPane.setVisible(false);
            forecastPane.setManaged(false);
        });
    }

    private void clearUI() {
        errorLabel.setText("");
        weatherDetailsPane.setVisible(false);
        forecastPane.setVisible(false);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}