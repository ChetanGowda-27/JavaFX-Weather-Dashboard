package com.example.weatherdashboard;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WeatherService {

    // !!! IMPORTANT: PASTE YOUR API KEY HERE !!!
    private static final String API_KEY = "f54232d3fbb69478c381b212562fffa0";

    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=%s";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=%s";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public CompletableFuture<JsonObject> getWeatherData(String city, String units) {
        return fetchData(String.format(WEATHER_URL, city, API_KEY, units));
    }

    public CompletableFuture<JsonObject> getForecastData(String city, String units) {
        return fetchData(String.format(FORECAST_URL, city, API_KEY, units));
    }

    private CompletableFuture<JsonObject> fetchData(String url) {
        try {
            String encodedUrl = url.replace(" ", "%20");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(encodedUrl))
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            return gson.fromJson(response.body(), JsonObject.class);
                        } else if (response.statusCode() == 401) {
                            throw new RuntimeException("API Error: Invalid API Key. Status: 401");
                        } else if (response.statusCode() == 404) {
                            throw new RuntimeException("API Error: City not found. Status: 404");
                        } else {
                            throw new RuntimeException("API Error: Status " + response.statusCode());
                        }
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}