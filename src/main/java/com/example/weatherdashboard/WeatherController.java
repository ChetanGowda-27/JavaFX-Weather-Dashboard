package com.example.weatherdashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class WeatherController {

    // FXML elements from the view
    @FXML private TextField cityTextField;
    @FXML private Label errorLabel;
    @FXML private VBox weatherDetailsPane;
    @FXML private Label locationLabel;
    @FXML private ImageView weatherIcon;
    @FXML private Label temperatureLabel;
    @FXML private Label conditionLabel;
    @FXML private Label humidityLabel;
    @FXML private VBox forecastPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label feelsLikeLabel;
    @FXML private ToggleGroup unitsToggleGroup;
    @FXML private ToggleButton celsiusButton;
    @FXML private ToggleButton fahrenheitButton;
    @FXML private LineChart<String, Number> forecastChart;
    @FXML private CategoryAxis forecastXAxis;
    @FXML private NumberAxis forecastYAxis;

    private final WeatherService weatherService = new WeatherService();
    private static final String ICON_URL_PREFIX = "https://openweathermap.org/img/wn/";
    private static final String ICON_URL_SUFFIX = "@2x.png";

    private Preferences prefs;
    private String currentUnits = "metric";
    private static ScheduledExecutorService autoRefreshExecutor;

    @FXML
    public void initialize() {
        prefs = Preferences.userNodeForPackage(WeatherController.class);

        celsiusButton.setUserData("metric");
        fahrenheitButton.setUserData("imperial");
        celsiusButton.setSelected(true);
        unitsToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                this.currentUnits = (String) newToggle.getUserData();
                handleSearchAction();
            } else {
                // Prevent deselection
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
            }
        });

        startAutoRefresh();

        String lastCity = prefs.get("lastSearchedCity", "");
        if (!lastCity.isEmpty()) {
            cityTextField.setText(lastCity);
            handleSearchAction();
        } else {
            clearUI();
        }
    }

    @FXML
    protected void handleSearchAction() {
        String city = cityTextField.getText();
        if (city == null || city.trim().isEmpty()) {
            showError("City name cannot be empty.");
            return;
        }

        clearUI();
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);

        weatherService.getWeatherData(city, currentUnits)
                .thenAccept(this::updateCurrentWeatherUI)
                .exceptionally(this::handleApiError)
                .whenComplete((v, e) -> hideLoadingSpinner());

        weatherService.getForecastData(city, currentUnits)
                .thenAccept(this::updateForecastUI)
                .exceptionally(this::handleApiError);
    }

    private void updateCurrentWeatherUI(JsonObject data) {
        Platform.runLater(() -> {
            weatherDetailsPane.setVisible(true);
            weatherDetailsPane.setManaged(true);

            String cityName = data.get("name").getAsString();
            locationLabel.setText(cityName + ", " + data.getAsJsonObject("sys").get("country").getAsString());

            JsonObject main = data.getAsJsonObject("main");
            String unitSymbol = currentUnits.equals("metric") ? "°C" : "°F";
            temperatureLabel.setText(String.format("%.0f%s", main.get("temp").getAsDouble(), unitSymbol));
            feelsLikeLabel.setText("Feels like: " + String.format("%.0f%s", main.get("feels_like").getAsDouble(), unitSymbol));
            humidityLabel.setText("Humidity: " + main.get("humidity").getAsInt() + "%");

            JsonObject weather = data.getAsJsonArray("weather").get(0).getAsJsonObject();
            conditionLabel.setText(capitalize(weather.get("description").getAsString()));
            weatherIcon.setImage(new Image(ICON_URL_PREFIX + weather.get("icon").getAsString() + ICON_URL_SUFFIX));

            prefs.put("lastSearchedCity", cityName);
        });
    }

    private void updateForecastUI(JsonObject data) {
        Platform.runLater(() -> {
            forecastPane.setVisible(true);
            forecastPane.setManaged(true);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            JsonArray forecastList = data.getAsJsonArray("list");

            for (int i = 0; i < forecastList.size(); i += 8) {
                JsonObject forecastItem = forecastList.get(i).getAsJsonObject();
                long dt = forecastItem.get("dt").getAsLong();
                String day = Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE"));
                double temp = forecastItem.getAsJsonObject("main").get("temp").getAsDouble();
                series.getData().add(new XYChart.Data<>(day, temp));
            }

            forecastChart.getData().clear();
            forecastChart.getData().add(series);
            forecastYAxis.setLabel("Temp (" + (currentUnits.equals("metric") ? "°C" : "°F") + ")");
        });
    }

    private void startAutoRefresh() {
        autoRefreshExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "Auto-Refresh-Thread");
            t.setDaemon(true);
            return t;
        });

        Runnable refreshTask = () -> Platform.runLater(this::handleSearchAction);

        autoRefreshExecutor.scheduleAtFixedRate(refreshTask, 30, 30, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        if (autoRefreshExecutor != null && !autoRefreshExecutor.isShutdown()) {
            autoRefreshExecutor.shutdownNow();
        }
    }

    private void hideLoadingSpinner() { Platform.runLater(() -> { loadingIndicator.setVisible(false); loadingIndicator.setManaged(false); }); }
    private Void handleApiError(Throwable error) { showError(error.getCause().getMessage()); return null; }
    private void showError(String message) { Platform.runLater(() -> { clearUI(); errorLabel.setText(message); hideLoadingSpinner(); }); }
    private void clearUI() {
        errorLabel.setText("");
        weatherDetailsPane.setVisible(false);
        weatherDetailsPane.setManaged(false);
        forecastPane.setVisible(false);
        forecastPane.setManaged(false);
    }
    private String capitalize(String text) { return text == null || text.isEmpty() ? text : text.substring(0, 1).toUpperCase() + text.substring(1); }
}