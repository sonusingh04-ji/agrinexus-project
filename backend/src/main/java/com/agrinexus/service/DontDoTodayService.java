package com.agrinexus.service;

import com.agrinexus.dto.DontDoTodayResponse;
import com.agrinexus.entity.IrrigationConfig;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.IrrigationConfigRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DontDoTodayService {

    private final IrrigationTelemetryRepository telemetryRepository;
    private final IrrigationConfigRepository configRepository;
    private final WeatherForecastDataRepository forecastRepository;
    private final WeatherRecordRepository weatherRecordRepository;

    /*
     * Evaluate "Don't Do This Today" conditions.
     *
     * The service combines:
     *
     * 1. Current telemetry
     * 2. Stored irrigation configuration
     * 3. Stored weather forecast
     * 4. Latest weather record
     *
     * It can report multiple applicable conditions together.
     */
    public DontDoTodayResponse evaluate(
            UUID farmId
    ) {

        /*
         * ---------------------------------------------------------
         * Latest telemetry
         * ---------------------------------------------------------
         */
        IrrigationTelemetry telemetry =
                telemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        /*
         * ---------------------------------------------------------
         * Irrigation configuration
         * ---------------------------------------------------------
         */
        IrrigationConfig config =
                configRepository.findByFarmId(
                        farmId
                );

        /*
         * ---------------------------------------------------------
         * Latest current weather
         * ---------------------------------------------------------
         */
        WeatherRecord weather =
                weatherRecordRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        /*
         * ---------------------------------------------------------
         * Stored forecast
         * ---------------------------------------------------------
         *
         * Use the configured forecast horizon.
         */
        LocalDate today =
                LocalDate.now();

        int horizonDays =
                config != null
                        && config.getForecastHorizonDays() != null
                        ? config.getForecastHorizonDays()
                        : 7;

        List<WeatherForecastData> forecasts =
                forecastRepository
                        .findByFarmIdAndForecastDateBetweenOrderByForecastDateAsc(
                                farmId,
                                today,
                                today.plusDays(horizonDays)
                        );

        /*
         * ---------------------------------------------------------
         * Current values
         * ---------------------------------------------------------
         */
        Double currentMoisture =
                telemetry != null
                        ? telemetry.getVwc30cm()
                        : null;

        Double fieldCapacity =
                config != null
                        ? config.getFieldCapacity()
                        : null;

        Double currentTemperature =
                weather != null
                        ? weather.getTemperature()
                        : null;

        Double currentWindSpeed =
                weather != null
                        ? weather.getWindSpeed()
                        : null;

        Double currentSolarIrradiance =
                telemetry != null
                        ? telemetry.getSolarIrradianceWm2()
                        : null;

        /*
         * ---------------------------------------------------------
         * Forecast values
         * ---------------------------------------------------------
         */
        Double forecastRainfall =
                forecasts.stream()
                        .map(
                                WeatherForecastData
                                        ::getRainfallMm
                        )
                        .filter(
                                value -> value != null
                        )
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .sum();

        Double forecastConfidence =
                forecasts.stream()
                        .map(
                                WeatherForecastData
                                        ::getForecastConfidence
                        )
                        .filter(
                                value -> value != null
                        )
                        .min(Double::compareTo)
                        .orElse(null);

        /*
         * ---------------------------------------------------------
         * Configured confidence threshold
         * ---------------------------------------------------------
         */
        Double minimumForecastConfidence =
                config != null
                        ? config.getMinimumForecastConfidence()
                        : null;

        boolean forecastReliable =
                forecastConfidence == null
                        || minimumForecastConfidence == null
                        || forecastConfidence
                        >= minimumForecastConfidence;

        /*
         * ---------------------------------------------------------
         * Collect ALL applicable conditions.
         * ---------------------------------------------------------
         */
        List<String> reasons =
                new ArrayList<>();

        List<String> messages =
                new ArrayList<>();

        /*
         * =========================================================
         * RULE 1 — Adequate forecast rainfall
         * =========================================================
         *
         * We only rely on the rainfall forecast when it has
         * acceptable confidence.
         */
        if (forecastRainfall != null
                && forecastRainfall >= 20.0
                && forecastReliable) {

            reasons.add(
                    "Stored forecast indicates "
                            + forecastRainfall
                            + " mm of rainfall within the configured forecast horizon."
            );

            messages.add(
                    "Delay irrigation and review the rainfall forecast first."
            );
        }

        /*
         * =========================================================
         * RULE 2 — Soil near field capacity
         * =========================================================
         *
         * Configured target:
         *
         * field capacity × target fraction
         */
        if (currentMoisture != null
                && fieldCapacity != null
                && config != null) {

            Double targetFraction =
                    config.getTargetFieldCapacityFraction();

            if (targetFraction == null) {
                targetFraction = 0.95;
            }

            Double targetMoisture =
                    fieldCapacity
                            * targetFraction;

            if (currentMoisture >= targetMoisture) {

                reasons.add(
                        "Current root-zone moisture is "
                                + currentMoisture
                                + "% compared with a configured target of "
                                + targetMoisture
                                + "%."
                );

                messages.add(
                        "Avoid additional irrigation unless field conditions justify it."
                );
            }
        }

        /*
         * =========================================================
         * RULE 3 — Extreme heat
         * =========================================================
         */
        if (currentTemperature != null
                && currentTemperature >= 42.0) {

            reasons.add(
                    "Current temperature is "
                            + currentTemperature
                            + "°C, within the configured extreme-heat warning range."
            );

            messages.add(
                    "Review irrigation timing carefully under extreme heat."
            );
        }

        /*
         * =========================================================
         * RULE 4 — High wind
         * =========================================================
         */
        if (currentWindSpeed != null
                && currentWindSpeed >= 25.0) {

            reasons.add(
                    "Current wind speed is "
                            + currentWindSpeed
                            + ", within the configured high-wind warning range."
            );

            messages.add(
                    "Review field operations before proceeding under high wind."
            );
        }

        /*
         * =========================================================
         * RULE 5 — Low solar
         * =========================================================
         */
        if (currentSolarIrradiance != null
                && currentSolarIrradiance < 200.0) {

            reasons.add(
                    "Current solar irradiance is "
                            + currentSolarIrradiance
                            + " W/m², below the configured solar-pumping threshold."
            );

            messages.add(
                    "Delay non-urgent solar pumping until solar availability improves."
            );
        }

        /*
         * =========================================================
         * RULE 6 — Low forecast confidence
         * =========================================================
         */
        if (forecastConfidence != null
                && minimumForecastConfidence != null
                && forecastConfidence
                < minimumForecastConfidence) {

            reasons.add(
                    "Forecast confidence is "
                            + (forecastConfidence * 100.0)
                            + "%, below the configured minimum of "
                            + (minimumForecastConfidence * 100.0)
                            + "%."
            );

            messages.add(
                    "Do not rely on the current rainfall forecast without checking for an updated forecast."
            );
        }

        /*
         * ---------------------------------------------------------
         * Decide overall action
         * ---------------------------------------------------------
         */
        String action =
                "IRRIGATION";

        String decision;

        String title;

        /*
         * Storage/current moisture has the strongest direct
         * irrigation-prevention meaning.
         */
        boolean irrigationAvoid =
                currentMoisture != null
                        && fieldCapacity != null
                        && config != null
                        && currentMoisture
                        >= fieldCapacity
                        * (
                        config.getTargetFieldCapacityFraction()
                                != null
                                ? config.getTargetFieldCapacityFraction()
                                : 0.95
                );

        boolean rainfallDelay =
                forecastRainfall != null
                        && forecastRainfall >= 20.0
                        && forecastReliable;

        boolean extremeWeather =
                (currentTemperature != null
                        && currentTemperature >= 42.0)
                        ||
                        (currentWindSpeed != null
                                && currentWindSpeed >= 25.0);

        boolean lowSolar =
                currentSolarIrradiance != null
                        && currentSolarIrradiance < 200.0;

        if (irrigationAvoid) {

            decision = "AVOID";

            title = "Don't Irrigate Today";

        } else if (rainfallDelay) {

            decision = "DELAY";

            title = "Delay Irrigation";

        } else if (extremeWeather) {

            decision = "REVIEW";

            title = "Weather Advisory";

        } else if (lowSolar) {

            action = "SOLAR_PUMPING";

            decision = "DELAY";

            title = "Delay Solar Pumping";

        } else {

            decision = "NO_OVERRIDE";

            title = "No Preventive Override";
        }

        /*
         * ---------------------------------------------------------
         * Combine messages
         * ---------------------------------------------------------
         */
        String combinedMessage;

        if (messages.isEmpty()) {

            combinedMessage =
                    "No major prevention rule was triggered by the currently available data.";

        } else {

            combinedMessage =
                    String.join(
                            " ",
                            messages
                    );
        }

        /*
         * ---------------------------------------------------------
         * Combine reasons
         * ---------------------------------------------------------
         */
        String combinedReason;

        if (reasons.isEmpty()) {

            combinedReason =
                    "Current available field, forecast and weather information does not indicate a specific preventive override.";

        } else {

            combinedReason =
                    String.join(
                            " ",
                            reasons
                    );
        }

        /*
         * ---------------------------------------------------------
         * Data provenance
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        telemetry,
                        forecasts,
                        config
                );

        /*
         * ---------------------------------------------------------
         * Response
         * ---------------------------------------------------------
         */
        return DontDoTodayResponse.builder()
                .farmId(farmId)
                .action(action)
                .decision(decision)
                .title(title)
                .message(combinedMessage)
                .reason(combinedReason)
                .forecastRainfall(forecastRainfall)
                .forecastConfidence(forecastConfidence)
                .currentMoisture(currentMoisture)
                .fieldCapacity(fieldCapacity)
                .temperature(currentTemperature)
                .windSpeed(currentWindSpeed)
                .solarIrradiance(currentSolarIrradiance)
                .dataStatus(dataStatus)
                .evaluatedAt(
                        LocalDateTime.now()
                )
                .build();
    }

    /*
     * -------------------------------------------------------------
     * Determine provenance.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            IrrigationTelemetry telemetry,
            List<WeatherForecastData> forecasts,
            IrrigationConfig config
    ) {

        boolean testTelemetry =
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                );

        boolean testForecast =
                forecasts.stream()
                        .anyMatch(
                                forecast ->
                                        "TEST".equalsIgnoreCase(
                                                forecast.getDataStatus()
                                        )
                                                || "TEST_FORECAST"
                                                .equalsIgnoreCase(
                                                        forecast.getSource()
                                                )
                        );

        boolean testConfig =
                config != null
                        && (
                        "TEST".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "ESTIMATED".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "TEST_CONFIGURATION".equalsIgnoreCase(
                                config.getSource()
                        )
                );

        if (testTelemetry
                || testForecast
                || testConfig) {

            return "TEST";
        }

        if (telemetry != null
                && !forecasts.isEmpty()) {

            return "MIXED";
        }

        if (!forecasts.isEmpty()) {

            return "FORECAST";
        }

        if (telemetry != null) {

            return "MEASURED";
        }

        return "UNKNOWN";
    }
}