package com.agrinexus.service;

import com.agrinexus.dto.RainfallHarvestingResponse;
import com.agrinexus.entity.IrrigationConfig;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.entity.WaterStorageProfile;
import com.agrinexus.repository.IrrigationConfigRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
import com.agrinexus.repository.WaterStorageProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RainfallHarvestingService {

    private final IrrigationTelemetryRepository telemetryRepository;
    private final IrrigationConfigRepository configRepository;
    private final WaterStorageProfileRepository storageRepository;
    private final WeatherForecastDataRepository forecastRepository;

    /**
     * Evaluate rainfall harvesting using the stored farm forecast.
     *
     * Data priority:
     *
     * 1. Stored weather forecast
     * 2. Latest telemetry for current conditions
     * 3. Farm configuration/storage profile
     */
    public RainfallHarvestingResponse evaluate(
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
         * Water storage profile
         * ---------------------------------------------------------
         */
        WaterStorageProfile storage =
                storageRepository.findByFarmId(
                        farmId
                );

        /*
         * ---------------------------------------------------------
         * Forecast data
         * ---------------------------------------------------------
         *
         * Look ahead over the configured forecast horizon.
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
         * Validate forecast availability
         * ---------------------------------------------------------
         */
        if (forecasts.isEmpty()) {

            return RainfallHarvestingResponse.builder()
                    .farmId(farmId)
                    .decision("INSUFFICIENT_DATA")
                    .recommendation(
                            "No upcoming rainfall forecast is available for this farm."
                    )
                    .reason(
                            "A rainfall forecast is required before evaluating rainfall harvesting opportunities."
                    )
                    .dataStatus("NOT_AVAILABLE")
                    .evaluatedAt(
                            LocalDateTime.now()
                    )
                    .build();
        }

        /*
         * ---------------------------------------------------------
         * Forecast rainfall
         * ---------------------------------------------------------
         *
         * Sum the available rainfall forecasts within the
         * configured forecast horizon.
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

        /*
         * ---------------------------------------------------------
         * Forecast confidence
         * ---------------------------------------------------------
         *
         * Use the lowest available confidence as a conservative
         * decision-support indicator.
         */
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
         * Current soil moisture
         * ---------------------------------------------------------
         */
        Double currentMoisture =
                telemetry != null
                        ? telemetry.getVwc30cm()
                        : null;

        /*
         * ---------------------------------------------------------
         * Field capacity
         * ---------------------------------------------------------
         */
        Double fieldCapacity =
                config != null
                        ? config.getFieldCapacity()
                        : null;

        /*
         * ---------------------------------------------------------
         * Effective rainfall
         * ---------------------------------------------------------
         *
         * This is a prototype-level conversion factor.
         *
         * It is intentionally documented as an assumption and
         * should later be replaced by a validated local/model-based
         * effective-rainfall calculation.
         */
        Double effectiveRainfall =
                forecastRainfall * 0.80;

        /*
         * ---------------------------------------------------------
         * Estimated captured water
         * ---------------------------------------------------------
         *
         * Approximation:
         *
         * mm × m² = litres
         *
         * because 1 mm over 1 m² = 1 litre.
         */
        Double estimatedCapture =
                null;

        if (storage != null
                && storage.getCatchmentAreaM2() != null) {

            estimatedCapture =
                    effectiveRainfall
                            * storage.getCatchmentAreaM2();

            /*
             * Apply available runoff coefficient when present.
             */
            if (storage.getRunoffCoefficient() != null) {

                estimatedCapture =
                        estimatedCapture
                                * storage.getRunoffCoefficient();
            }
        }

        /*
         * ---------------------------------------------------------
         * Storage status
         * ---------------------------------------------------------
         */
        boolean storageNearFull =
                storage != null
                        && storage.getStorageCapacityLiters() != null
                        && storage.getCurrentStorageLiters() != null
                        && storage.getCurrentStorageLiters()
                        >= storage.getStorageCapacityLiters()
                        * (
                        storage.getStorageSafetyLimitPercent() != null
                                ? storage.getStorageSafetyLimitPercent()
                                / 100.0
                                : 0.95
                );

        /*
         * ---------------------------------------------------------
         * Forecast confidence check
         * ---------------------------------------------------------
         */
        boolean forecastReliable =
                config == null
                        || config.getMinimumForecastConfidence() == null
                        || forecastConfidence == null
                        || forecastConfidence
                        >= config.getMinimumForecastConfidence();

        /*
         * ---------------------------------------------------------
         * Decision
         * ---------------------------------------------------------
         */
        String decision;

        if (storageNearFull) {

            decision =
                    "STORAGE_NEAR_FULL";

        } else if (forecastRainfall > 0
                && !forecastReliable) {

            decision =
                    "MONITOR_FORECAST";

        } else if (forecastRainfall > 0
                && fieldCapacity != null
                && currentMoisture != null
                && currentMoisture < fieldCapacity) {

            decision =
                    "CAPTURE_AND_REVIEW_IRRIGATION";

        } else if (forecastRainfall > 0) {

            decision =
                    "CAPTURE_AND_MONITOR";

        } else {

            decision =
                    "MONITOR";
        }

        /*
         * ---------------------------------------------------------
         * Recommendation
         * ---------------------------------------------------------
         */
        String recommendation;

        switch (decision) {

            case "STORAGE_NEAR_FULL":

                recommendation =
                        "Storage is near its configured safety limit. Follow the site's safe overflow or diversion procedure.";

                break;

            case "MONITOR_FORECAST":

                recommendation =
                        "Rainfall is forecast, but forecast confidence is below the configured minimum. Monitor updated forecasts before changing irrigation plans.";

                break;

            case "CAPTURE_AND_REVIEW_IRRIGATION":

                recommendation =
                        "Use suitable rainwater-capture/storage infrastructure and review planned irrigation against the expected rainfall.";

                break;

            case "CAPTURE_AND_MONITOR":

                recommendation =
                        "Monitor the rainfall forecast and use suitable rainwater-capture/storage infrastructure where appropriate.";

                break;

            default:

                recommendation =
                        "No significant rainfall-harvesting action is indicated from the currently stored forecast.";
        }

        /*
         * ---------------------------------------------------------
         * Reason
         * ---------------------------------------------------------
         */
        String reason =
                buildReason(
                        forecastRainfall,
                        effectiveRainfall,
                        forecastConfidence,
                        currentMoisture,
                        fieldCapacity,
                        storage,
                        forecastReliable
                );

        /*
         * ---------------------------------------------------------
         * Provenance
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        forecasts,
                        telemetry,
                        config,
                        storage
                );

        /*
         * ---------------------------------------------------------
         * Response
         * ---------------------------------------------------------
         */
        return RainfallHarvestingResponse.builder()
                .farmId(farmId)
                .forecastRainfallMm(
                        forecastRainfall
                )
                .effectiveRainfallMm(
                        effectiveRainfall
                )
                .forecastConfidence(
                        forecastConfidence
                )
                .currentMoisture(
                        currentMoisture
                )
                .fieldCapacity(
                        fieldCapacity
                )
                .storageCapacityLiters(
                        storage != null
                                ? storage.getStorageCapacityLiters()
                                : null
                )
                .currentStorageLiters(
                        storage != null
                                ? storage.getCurrentStorageLiters()
                                : null
                )
                .estimatedCaptureLiters(
                        estimatedCapture
                )
                .decision(
                        decision
                )
                .recommendation(
                        recommendation
                )
                .reason(
                        reason
                )
                .dataStatus(
                        dataStatus
                )
                .evaluatedAt(
                        LocalDateTime.now()
                )
                .build();
    }

    /*
     * -------------------------------------------------------------
     * Build explainable reason.
     * -------------------------------------------------------------
     */
    private String buildReason(
            Double forecastRainfall,
            Double effectiveRainfall,
            Double forecastConfidence,
            Double currentMoisture,
            Double fieldCapacity,
            WaterStorageProfile storage,
            boolean forecastReliable
    ) {

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                "Stored forecast rainfall over the configured horizon is "
        );

        reason.append(
                forecastRainfall
        );

        reason.append(
                " mm, with estimated effective rainfall of "
        );

        reason.append(
                effectiveRainfall
        );

        reason.append(
                " mm."
        );

        if (forecastConfidence != null) {

            reason.append(
                    " Lowest forecast confidence is "
            );

            reason.append(
                    forecastConfidence * 100
            );

            reason.append(
                    "%."
            );
        }

        if (!forecastReliable) {

            reason.append(
                    " Forecast confidence is below the configured minimum."
            );
        }

        if (currentMoisture != null
                && fieldCapacity != null) {

            reason.append(
                    " Current root-zone moisture is "
            );

            reason.append(
                    currentMoisture
            );

            reason.append(
                    "% compared with field capacity of "
            );

            reason.append(
                    fieldCapacity
            );

            reason.append(
                    "%."
            );
        }

        if (storage != null
                && storage.getCatchmentAreaM2() != null) {

            reason.append(
                    " Configured catchment area is "
            );

            reason.append(
                    storage.getCatchmentAreaM2()
            );

            reason.append(
                    " m²."
            );
        }

        return reason.toString();
    }

    /*
     * -------------------------------------------------------------
     * Data provenance.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            List<WeatherForecastData> forecasts,
            IrrigationTelemetry telemetry,
            IrrigationConfig config,
            WaterStorageProfile storage
    ) {

        boolean hasTestForecast =
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

        boolean hasTestTelemetry =
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                );

        boolean hasTestConfiguration =
                config != null
                        && (
                        "ESTIMATED".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "TEST".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "TEST_CONFIGURATION".equalsIgnoreCase(
                                config.getSource()
                        )
                );

        boolean hasTestStorage =
                storage != null
                        && (
                        "ESTIMATED".equalsIgnoreCase(
                                storage.getDataStatus()
                        )
                                || "TEST".equalsIgnoreCase(
                                storage.getDataStatus()
                        )
                                || "TEST_CONFIGURATION".equalsIgnoreCase(
                                storage.getSource()
                        )
                );

        if (hasTestForecast
                || hasTestTelemetry
                || hasTestConfiguration
                || hasTestStorage) {

            return "TEST";
        }

        if (telemetry != null
                && !forecasts.isEmpty()) {

            return "MIXED";
        }

        if (!forecasts.isEmpty()) {

            return "FORECAST";
        }

        return "UNKNOWN";
    }
}