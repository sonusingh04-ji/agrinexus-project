package com.agrinexus.service;

import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveWeatherService {

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast";

    private final FarmRepository farmRepository;

    private final WeatherRecordRepository weatherRecordRepository;

    private final WeatherRiskAnalysisService weatherRiskAnalysisService;

    private final CombinedRiskAnalysisService combinedRiskAnalysisService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(10)
                    )
                    .build();

    @Transactional
    public WeatherRecord fetchAndSaveCurrentWeather(
            UUID farmId
    ) {

        /*
         * =========================================================
         * 1. FIND REAL FARM
         * =========================================================
         */

        Farm farm =
                farmRepository.findById(
                        farmId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + farmId
                        )
                );

        /*
         * =========================================================
         * 2. VALIDATE REAL FARM COORDINATES
         * =========================================================
         */

        if (farm.getLatitude() == null
                || farm.getLongitude() == null) {

            throw new IllegalStateException(
                    "Farm latitude and longitude are required "
                            + "for live weather data."
            );
        }

        /*
         * =========================================================
         * 3. BUILD LIVE WEATHER URL
         * =========================================================
         *
         * Open-Meteo supports these current variables:
         *
         * temperature_2m
         * relative_humidity_2m
         * apparent_temperature
         * precipitation
         * surface_pressure
         * wind_speed_10m
         * wind_direction_10m
         * weather_code
         *
         * No weather values are hardcoded.
         */

        String url =
                OPEN_METEO_URL
                        + "?latitude="
                        + farm.getLatitude()
                        + "&longitude="
                        + farm.getLongitude()
                        + "&current="
                        + "temperature_2m,"
                        + "relative_humidity_2m,"
                        + "apparent_temperature,"
                        + "precipitation,"
                        + "surface_pressure,"
                        + "wind_speed_10m,"
                        + "wind_direction_10m,"
                        + "weather_code"
                        + "&timezone=auto";

        /*
         * =========================================================
         * 4. HTTP REQUEST
         * =========================================================
         */

        HttpRequest httpRequest =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(url)
                        )
                        .timeout(
                                Duration.ofSeconds(15)
                        )
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            /*
             * =====================================================
             * 5. HTTP VALIDATION
             * =====================================================
             */

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Live weather provider returned HTTP "
                                + response.statusCode()
                );
            }

            if (response.body() == null
                    || response.body().isBlank()) {

                throw new RuntimeException(
                        "Live weather provider returned an empty response."
                );
            }

            /*
             * =====================================================
             * 6. PARSE JSON
             * =====================================================
             */

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode current =
                    root.path(
                            "current"
                    );

            if (current.isMissingNode()
                    || current.isNull()
                    || !current.isObject()) {

                throw new RuntimeException(
                        "Live weather response does not contain "
                                + "a valid current weather object."
                );
            }

            /*
             * =====================================================
             * 7. EXTRACT LIVE VALUES
             * =====================================================
             */

            Double temperature =
                    readDouble(
                            current,
                            "temperature_2m"
                    );

            Integer humidity =
                    readInteger(
                            current,
                            "relative_humidity_2m"
                    );

            Double feelsLike =
                    readDouble(
                            current,
                            "apparent_temperature"
                    );

            Double rainfall =
                    readDouble(
                            current,
                            "precipitation"
                    );

            Double pressure =
                    readDouble(
                            current,
                            "surface_pressure"
                    );

            Double windSpeed =
                    readDouble(
                            current,
                            "wind_speed_10m"
                    );

            Integer windDirection =
                    readInteger(
                            current,
                            "wind_direction_10m"
                    );

            Integer weatherCode =
                    readInteger(
                            current,
                            "weather_code"
                    );

            /*
             * =====================================================
             * 8. IMPORTANT HUMIDITY VALIDATION
             * =====================================================
             *
             * We NEVER replace missing humidity with a guessed
             * value.
             *
             * If Open-Meteo actually does not return it, fail
             * instead of saving an incomplete weather record.
             */

            if (humidity == null) {

                throw new IllegalStateException(
                        "Live weather response did not contain "
                                + "relative_humidity_2m. "
                                + "No humidity value was invented."
                );
            }

            /*
             * =====================================================
             * 9. CONDITION
             * =====================================================
             */

            String weatherCondition =
                    weatherConditionFromCode(
                            weatherCode
                    );

            String weatherDescription =
                    weatherDescriptionFromCode(
                            weatherCode
                    );

            /*
             * =====================================================
             * 10. TIME
             * =====================================================
             */

            LocalDateTime recordedAt =
                    parseRecordedAt(
                            current
                    );

            /*
             * =====================================================
             * 11. BUILD REAL WEATHER RECORD
             * =====================================================
             */

            WeatherRecord weatherRecord =
                    WeatherRecord.builder()

                            .farm(
                                    farm
                            )

                            .recordedAt(
                                    recordedAt
                            )

                            .temperature(
                                    temperature
                            )

                            .feelsLike(
                                    feelsLike
                            )

                            .humidity(
                                    humidity
                            )

                            .rainfall(
                                    rainfall
                            )

                            .windSpeed(
                                    windSpeed
                            )

                            .windDirection(
                                    windDirection
                            )

                            .pressure(
                                    pressure
                            )

                            .weatherCondition(
                                    weatherCondition
                            )

                            .weatherDescription(
                                    weatherDescription
                            )

                            .build();

            /*
             * =====================================================
             * 12. SAVE WEATHER
             * =====================================================
             */

            WeatherRecord savedRecord =
                    weatherRecordRepository.save(
                            weatherRecord
                    );

            /*
             * =====================================================
             * 13. WEATHER RISK ANALYSIS
             * =====================================================
             */

            weatherRiskAnalysisService.analyzeWeather(
                    savedRecord
            );

            /*
             * =====================================================
             * 14. COMBINED FARM RISK
             * =====================================================
             */

            combinedRiskAnalysisService.analyzeFarmRisk(
                    farmId
            );

            return savedRecord;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Live weather request was interrupted.",
                    exception
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Unable to retrieve live weather data.",
                    exception
            );
        }
    }

    /*
     * =========================================================
     * DOUBLE READER
     * =========================================================
     */

    private Double readDouble(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(
                        field
                );

        if (value == null
                || value.isNull()
                || !value.isNumber()) {

            return null;
        }

        return value.doubleValue();
    }

    /*
     * =========================================================
     * INTEGER READER
     * =========================================================
     */

    private Integer readInteger(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(
                        field
                );

        if (value == null
                || value.isNull()
                || !value.isNumber()) {

            return null;
        }

        return value.intValue();
    }

    /*
     * =========================================================
     * RECORDED TIME
     * =========================================================
     */

    private LocalDateTime parseRecordedAt(
            JsonNode current
    ) {

        JsonNode timeNode =
                current.get(
                        "time"
                );

        if (timeNode == null
                || timeNode.isNull()
                || timeNode.asText().isBlank()) {

            return LocalDateTime.now();
        }

        try {

            return LocalDateTime.parse(
                    timeNode.asText()
            );

        } catch (Exception exception) {

            return LocalDateTime.now();
        }
    }

    /*
     * =========================================================
     * WEATHER CONDITION
     * =========================================================
     */

    private String weatherConditionFromCode(
            Integer code
    ) {

        if (code == null) {
            return "Unknown";
        }

        if (code == 0) {
            return "Clear";
        }

        if (code == 1
                || code == 2
                || code == 3) {

            return "Clouds";
        }

        if (code == 45
                || code == 48) {

            return "Fog";
        }

        if (code >= 51
                && code <= 57) {

            return "Drizzle";
        }

        if (code >= 61
                && code <= 67) {

            return "Rain";
        }

        if (code >= 71
                && code <= 77) {

            return "Snow";
        }

        if (code >= 80
                && code <= 82) {

            return "Rain Showers";
        }

        if (code >= 85
                && code <= 86) {

            return "Snow Showers";
        }

        if (code == 95
                || code == 96
                || code == 99) {

            return "Thunderstorm";
        }

        return "Unknown";
    }

    /*
     * =========================================================
     * WEATHER DESCRIPTION
     * =========================================================
     */

    private String weatherDescriptionFromCode(
            Integer code
    ) {

        if (code == null) {

            return "Weather description unavailable";
        }

        return switch (code) {

            case 0 ->
                    "Clear sky";

            case 1 ->
                    "Mainly clear";

            case 2 ->
                    "Partly cloudy";

            case 3 ->
                    "Overcast";

            case 45, 48 ->
                    "Foggy conditions";

            case 51, 53, 55 ->
                    "Drizzle";

            case 56, 57 ->
                    "Freezing drizzle";

            case 61, 63, 65 ->
                    "Rain";

            case 66, 67 ->
                    "Freezing rain";

            case 71, 73, 75, 77 ->
                    "Snow conditions";

            case 80, 81, 82 ->
                    "Rain showers";

            case 85, 86 ->
                    "Snow showers";

            case 95 ->
                    "Thunderstorm";

            case 96, 99 ->
                    "Thunderstorm with hail";

            default ->
                    "Weather conditions available";
        };
    }
}