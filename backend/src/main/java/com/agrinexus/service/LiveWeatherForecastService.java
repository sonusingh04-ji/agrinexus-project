package com.agrinexus.service;

import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveWeatherForecastService {

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast";

    private final FarmRepository farmRepository;

    private final WeatherForecastDataRepository
            weatherForecastDataRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(10)
                    )
                    .build();

    @Transactional
    public List<WeatherForecastData> fetchAndSaveForecast(
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
                            + "for live forecast data."
            );
        }

        /*
         * =========================================================
         * 3. BUILD OPEN-METEO DAILY REQUEST
         * =========================================================
         *
         * We request seven forecast days.
         *
         * Real variables:
         *
         * precipitation_sum
         * precipitation_probability_max
         * et0_fao_evapotranspiration
         * temperature_2m_max
         * temperature_2m_min
         * wind_speed_10m_max
         *
         * Solar radiation is deliberately not mapped into
         * solarIrradianceWm2 because Open-Meteo's daily
         * solar radiation sum has a different unit.
         *
         * No value is fabricated.
         */

        String url =
                OPEN_METEO_URL
                        + "?latitude="
                        + farm.getLatitude()
                        + "&longitude="
                        + farm.getLongitude()
                        + "&daily="
                        + "precipitation_sum,"
                        + "precipitation_probability_max,"
                        + "et0_fao_evapotranspiration,"
                        + "temperature_2m_max,"
                        + "temperature_2m_min,"
                        + "wind_speed_10m_max"
                        + "&forecast_days=7"
                        + "&wind_speed_unit=kmh"
                        + "&timezone=auto";

        /*
         * =========================================================
         * 4. HTTP REQUEST
         * =========================================================
         */

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(url)
                        )
                        .timeout(
                                Duration.ofSeconds(20)
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
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            /*
             * =====================================================
             * 5. HTTP VALIDATION
             * =====================================================
             */

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Live weather forecast provider returned HTTP "
                                + response.statusCode()
                );
            }

            if (response.body() == null
                    || response.body().isBlank()) {

                throw new RuntimeException(
                        "Live weather forecast provider returned "
                                + "an empty response."
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

            JsonNode daily =
                    root.path(
                            "daily"
                    );

            if (daily.isMissingNode()
                    || daily.isNull()
                    || !daily.isObject()) {

                throw new RuntimeException(
                        "Live weather forecast response does not "
                                + "contain valid daily data."
                );
            }

            JsonNode dates =
                    daily.path(
                            "time"
                    );

            JsonNode rainfall =
                    daily.path(
                            "precipitation_sum"
                    );

            JsonNode precipitationProbability =
                    daily.path(
                            "precipitation_probability_max"
                    );

            JsonNode et0 =
                    daily.path(
                            "et0_fao_evapotranspiration"
                    );

            JsonNode temperatureMax =
                    daily.path(
                            "temperature_2m_max"
                    );

            JsonNode temperatureMin =
                    daily.path(
                            "temperature_2m_min"
                    );

            JsonNode windSpeed =
                    daily.path(
                            "wind_speed_10m_max"
                    );

            if (!dates.isArray()) {

                throw new RuntimeException(
                        "Forecast dates are missing from "
                                + "the live forecast response."
                );
            }

            /*
             * =====================================================
             * 7. REMOVE EXISTING UPCOMING FORECAST SNAPSHOT
             * =====================================================
             *
             * We replace the farm's current seven-day forecast
             * snapshot instead of continuously appending duplicate
             * dates every time the page loads.
             *
             * This only removes forecast-data rows in the current
             * seven-day window.
             */

            LocalDate today =
                    LocalDate.now();

            LocalDate endDate =
                    today.plusDays(7);

            List<WeatherForecastData> existing =
                    weatherForecastDataRepository
                            .findByFarmIdAndForecastDateBetweenOrderByForecastDateAsc(
                                    farmId,
                                    today,
                                    endDate
                            );

            if (!existing.isEmpty()) {

                weatherForecastDataRepository.deleteAll(
                        existing
                );

                weatherForecastDataRepository.flush();
            }

            /*
             * =====================================================
             * 8. BUILD NEW FORECAST RECORDS
             * =====================================================
             */

            List<WeatherForecastData> records =
                    new ArrayList<>();

            for (int i = 0;
                 i < dates.size();
                 i++) {

                String dateText =
                        dates.get(i).asText();

                if (dateText == null
                        || dateText.isBlank()) {

                    continue;
                }

                LocalDate forecastDate =
                        LocalDate.parse(
                                dateText
                        );

                /*
                 * The provider gives precipitation probability
                 * as percentage (0-100).
                 *
                 * Our entity stores it as 0-1.
                 */

                Double precipitationProbabilityValue =
                        readProbabilityAsFraction(
                                precipitationProbability,
                                i
                        );

                Double rainfallValue =
                        readDouble(
                                rainfall,
                                i
                        );

                Double et0Value =
                        readDouble(
                                et0,
                                i
                        );

                Double temperatureMaxValue =
                        readDouble(
                                temperatureMax,
                                i
                        );

                Double temperatureMinValue =
                        readDouble(
                                temperatureMin,
                                i
                        );

                Double windSpeedValue =
                        readDouble(
                                windSpeed,
                                i
                        );

                /*
                 * Forecast confidence is intentionally null.
                 *
                 * We do not invent a confidence number when the
                 * selected weather provider does not supply one.
                 */

                WeatherForecastData record =
                        WeatherForecastData.builder()

                                .farm(
                                        farm
                                )

                                .forecastDate(
                                        forecastDate
                                )

                                .rainfallMm(
                                        rainfallValue
                                )

                                .precipitationProbability(
                                        precipitationProbabilityValue
                                )

                                .forecastConfidence(
                                        null
                                )

                                .et0Mm(
                                        et0Value
                                )

                                .solarIrradianceWm2(
                                        null
                                )

                                .temperatureMax(
                                        temperatureMaxValue
                                )

                                .temperatureMin(
                                        temperatureMinValue
                                )

                                .windSpeed(
                                        windSpeedValue
                                )

                                .source(
                                        "OPEN_METEO"
                                )

                                .dataStatus(
                                        "FORECAST"
                                )

                                .notes(
                                        "Seven-day daily forecast "
                                                + "synchronized from Open-Meteo. "
                                                + "Forecast confidence is not "
                                                + "provided by the provider and "
                                                + "has not been fabricated."
                                )

                                .build();

                records.add(
                        record
                );
            }

            /*
             * =====================================================
             * 9. REQUIRE ACTUAL FORECAST DATA
             * =====================================================
             */

            if (records.isEmpty()) {

                throw new RuntimeException(
                        "No usable daily forecast records were "
                                + "returned by the weather provider."
                );
            }

            /*
             * =====================================================
             * 10. SAVE FORECAST SNAPSHOT
             * =====================================================
             */

            return weatherForecastDataRepository.saveAll(
                    records
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Live weather forecast request was interrupted.",
                    exception
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Unable to retrieve live weather forecast.",
                    exception
            );
        }
    }

    /*
     * =========================================================
     * READ DOUBLE FROM JSON ARRAY
     * =========================================================
     */

    private Double readDouble(
            JsonNode array,
            int index
    ) {

        if (array == null
                || !array.isArray()
                || index >= array.size()) {

            return null;
        }

        JsonNode value =
                array.get(
                        index
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
     * READ PRECIPITATION PROBABILITY
     * =========================================================
     *
     * Provider:
     *     0-100
     *
     * Entity:
     *     0-1
     */

    private Double readProbabilityAsFraction(
            JsonNode array,
            int index
    ) {

        Double percentage =
                readDouble(
                        array,
                        index
                );

        if (percentage == null) {
            return null;
        }

        return percentage / 100.0;
    }
}