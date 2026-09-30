package com.agrinexus.service;

import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.entity.SowingAdvisorDecision;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.SoilSampleRepository;
import com.agrinexus.repository.SowingAdvisorDecisionRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SowingAdvisorService {

    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-SOWING-V1";

    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final CropHistoryRepository cropHistoryRepository;
    private final SoilSampleRepository soilSampleRepository;
    private final WeatherRecordRepository weatherRecordRepository;
    private final WeatherForecastDataRepository weatherForecastDataRepository;
    private final IrrigationTelemetryRepository irrigationTelemetryRepository;
    private final SowingAdvisorDecisionRepository decisionRepository;

    /*
     * -------------------------------------------------------------
     * Main sowing evaluation.
     * -------------------------------------------------------------
     */
    @Transactional
    public SowingAdvisorDecision evaluate(
            UUID farmId,
            UUID cropId
    ) {

        Farm farm =
                farmRepository.findById(
                        farmId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + farmId
                        )
                );

        Crop crop =
                cropRepository.findById(
                        cropId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Crop not found with ID: "
                                        + cropId
                        )
                );

        /*
         * Latest farm data.
         */
        SoilSample soil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );

        WeatherRecord weather =
                weatherRecordRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        WeatherForecastData forecast =
                weatherForecastDataRepository
                        .findFirstByFarmIdAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                                farmId,
                                LocalDate.now()
                        );

        IrrigationTelemetry telemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        CropHistory previousCropHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );

        /*
         * ---------------------------------------------------------
         * Extract observations.
         * ---------------------------------------------------------
         */

        Double currentMoisture = null;

        if (telemetry != null) {

            currentMoisture =
                    telemetry.getVwc30cm();
        }

        Double currentTemperature =
                weather != null
                        ? weather.getTemperature()
                        : null;

        Integer currentHumidity =
                weather != null
                        ? weather.getHumidity()
                        : null;

        Double recentRainfall =
                weather != null
                        ? weather.getRainfall()
                        : null;

        Double forecastRainfall =
                forecast != null
                        ? forecast.getRainfallMm()
                        : null;

        Double forecastConfidence =
                forecast != null
                        ? forecast.getForecastConfidence()
                        : null;

        Double soilPh =
                soil != null
                        ? soil.getPH()
                        : null;

        /*
         * ---------------------------------------------------------
         * Decision factors.
         * ---------------------------------------------------------
         */
        List<String> factors =
                new ArrayList<>();

        boolean insufficientData =
                weather == null
                        && forecast == null
                        && soil == null
                        && telemetry == null;

        /*
         * ---------------------------------------------------------
         * Check crop sowing season/window.
         *
         * This is deliberately conservative because the existing
         * Crop.sowingWindow field is descriptive text and is not a
         * machine-readable date range.
         * ---------------------------------------------------------
         */
        boolean seasonCompatible =
                isCurrentSeasonCompatible(
                        crop.getSeason(),
                        LocalDate.now()
                );

        if (!seasonCompatible) {

            factors.add(
                    "Current calendar season does not match the crop's configured growing season"
            );
        } else {

            factors.add(
                    "Current calendar season is compatible with the crop's configured season"
            );
        }

        /*
         * ---------------------------------------------------------
         * Temperature suitability.
         * ---------------------------------------------------------
         */
        boolean temperatureSuitable =
                isTemperatureSuitable(
                        currentTemperature,
                        crop.getIdealTemperatureMin(),
                        crop.getIdealTemperatureMax()
                );

        if (currentTemperature != null) {

            if (temperatureSuitable) {

                factors.add(
                        "Current temperature is within the configured crop temperature range"
                );

            } else {

                factors.add(
                        "Current temperature is outside the configured crop temperature range"
                );
            }
        } else {

            factors.add(
                    "Current temperature is unavailable"
            );
        }

        /*
         * ---------------------------------------------------------
         * Soil pH suitability.
         * ---------------------------------------------------------
         */
        boolean soilPhSuitable =
                isSoilPhSuitable(
                        soilPh,
                        crop.getSoilPhMin(),
                        crop.getSoilPhMax()
                );

        if (soilPh != null) {

            if (soilPhSuitable) {

                factors.add(
                        "Latest soil pH is within the crop's configured pH range"
                );

            } else {

                factors.add(
                        "Latest soil pH is outside the crop's configured pH range"
                );
            }
        } else {

            factors.add(
                    "Latest soil pH is unavailable"
            );
        }

        /*
         * ---------------------------------------------------------
         * Soil moisture screening.
         *
         * VWC values here are treated as a project-level screening
         * signal. Exact crop-specific soil-moisture calibration
         * should later be connected to soil texture/root-zone data.
         * ---------------------------------------------------------
         */
        MoistureStatus moistureStatus =
                evaluateMoisture(
                        currentMoisture
                );

        switch (moistureStatus) {

            case SUITABLE -> factors.add(
                    "Root-zone moisture is in the screening range"
            );

            case TOO_DRY -> factors.add(
                    "Root-zone moisture may be too low for sowing"
            );

            case TOO_WET -> factors.add(
                    "Root-zone moisture may be too high for timely field operation"
            );

            case UNKNOWN -> factors.add(
                    "Root-zone moisture is unavailable"
            );
        }

        /*
         * ---------------------------------------------------------
         * Rainfall screening.
         * ---------------------------------------------------------
         */
        boolean heavyRecentRain =
                recentRainfall != null
                        && recentRainfall >= 25.0;

        boolean heavyForecastRain =
                forecastRainfall != null
                        && forecastRainfall >= 25.0
                        && (
                        forecastConfidence == null
                                || forecastConfidence >= 0.70
                );

        if (heavyRecentRain) {

            factors.add(
                    "Recent rainfall is high enough to warrant field-condition checking"
            );
        }

        if (heavyForecastRain) {

            factors.add(
                    "Forecast rainfall is high enough to consider delaying field operations"
            );
        }

        /*
         * ---------------------------------------------------------
         * Build decision.
         * ---------------------------------------------------------
         */
        String decision;

        String recommendedWindow;

        String reason;

        String recommendation;

        if (insufficientData) {

            decision =
                    "INSUFFICIENT_DATA";

            recommendedWindow =
                    "INSUFFICIENT_DATA";

            reason =
                    "There is not enough current farm, soil or weather information to make a reliable sowing decision.";

            recommendation =
                    "Collect current field moisture and weather information before deciding when to sow.";

        } else if (!seasonCompatible) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "OUTSIDE_CROP_WINDOW";

            reason =
                    "The current calendar season does not match the crop's configured growing season.";

            recommendation =
                    "Wait for the crop's appropriate sowing period and review the weather and soil conditions again.";

        } else if (heavyForecastRain) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "WAIT_FOR_WEATHER";

            reason =
                    "Forecast rainfall may create unsuitable field conditions for sowing.";

            recommendation =
                    "Delay sowing until the expected rainfall has passed and the field is suitable for operation.";

        } else if (heavyRecentRain) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "WAIT_FOR_FIELD_CONDITION";

            reason =
                    "Recent rainfall may have left the field too wet for appropriate sowing operations.";

            recommendation =
                    "Check field moisture and workability after the rain before sowing.";

        } else if (moistureStatus == MoistureStatus.TOO_WET) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "WAIT_FOR_MOISTURE";

            reason =
                    "Current root-zone moisture is above the conservative screening range for field operation.";

            recommendation =
                    "Wait until the field reaches suitable moisture and workability.";

        } else if (moistureStatus == MoistureStatus.TOO_DRY) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "WAIT_FOR_MOISTURE";

            reason =
                    "Current root-zone moisture may be too low for reliable sowing establishment.";

            recommendation =
                    "Check soil moisture and follow the validated irrigation or moisture-management plan before sowing.";

        } else if (!soilPhSuitable
                && soilPh != null) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "CHECK_SOIL";

            reason =
                    "Latest soil pH is outside the configured suitability range for the selected crop.";

            recommendation =
                    "Review soil conditions and obtain appropriate agronomic guidance before sowing.";

        } else if (currentTemperature != null
                && !temperatureSuitable) {

            decision =
                    "WAIT";

            recommendedWindow =
                    "WAIT_FOR_TEMPERATURE";

            reason =
                    "Current temperature is outside the configured crop temperature range.";

            recommendation =
                    "Wait for more suitable temperature conditions and reassess the sowing window.";

        } else if (seasonCompatible
                && (
                moistureStatus == MoistureStatus.SUITABLE
                        || moistureStatus == MoistureStatus.UNKNOWN
        )
                && (
                currentTemperature == null
                        || temperatureSuitable
        )) {

            decision =
                    "SOW_NOW";

            recommendedWindow =
                    "NOW";

            reason =
                    "Current season and available soil/weather conditions are compatible with the crop's configured sowing requirements.";

            recommendation =
                    "Sowing conditions are currently acceptable based on the available information. Confirm field workability locally before sowing.";

        } else {

            decision =
                    "WAIT";

            recommendedWindow =
                    "REASSESS";

            reason =
                    "Some required sowing conditions are not sufficiently established.";

            recommendation =
                    "Reassess soil moisture, weather and crop requirements before sowing.";
        }

        /*
         * ---------------------------------------------------------
         * Confidence.
         * ---------------------------------------------------------
         */
        double confidence =
                calculateConfidence(
                        soil,
                        weather,
                        forecast,
                        telemetry,
                        seasonCompatible,
                        temperatureSuitable,
                        soilPhSuitable
                );

        /*
         * ---------------------------------------------------------
         * Data status.
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        weather,
                        forecast,
                        telemetry
                );

        /*
         * ---------------------------------------------------------
         * Data used.
         * ---------------------------------------------------------
         */
        String dataUsed =
                buildDataUsed(
                        soil,
                        weather,
                        forecast,
                        telemetry,
                        previousCropHistory
                );

        /*
         * ---------------------------------------------------------
         * Uncertainty.
         * ---------------------------------------------------------
         */
        String uncertainty =
                buildUncertaintyNote(
                        crop,
                        soil,
                        weather,
                        forecast,
                        telemetry
                );

        /*
         * ---------------------------------------------------------
         * Save decision.
         * ---------------------------------------------------------
         */
        SowingAdvisorDecision result =
                SowingAdvisorDecision.builder()

                        .farm(farm)

                        .crop(crop)

                        .decision(
                                decision
                        )

                        .recommendedWindow(
                                recommendedWindow
                        )

                        .season(
                                crop.getSeason()
                        )

                        .currentMoisture(
                                currentMoisture
                        )

                        .currentTemperature(
                                currentTemperature
                        )

                        .currentHumidity(
                                currentHumidity
                        )

                        .recentRainfall(
                                recentRainfall
                        )

                        .forecastRainfall(
                                forecastRainfall
                        )

                        .forecastConfidence(
                                forecastConfidence
                        )

                        .idealTemperatureMin(
                                crop.getIdealTemperatureMin()
                        )

                        .idealTemperatureMax(
                                crop.getIdealTemperatureMax()
                        )

                        .soilPh(
                                soilPh
                        )

                        .cropSoilPhMin(
                                crop.getSoilPhMin()
                        )

                        .cropSoilPhMax(
                                crop.getSoilPhMax()
                        )

                        .reason(
                                reason
                        )

                        .contributingFactors(
                                String.join(
                                        "; ",
                                        factors
                                )
                        )

                        .recommendation(
                                recommendation
                        )

                        .dataUsed(
                                dataUsed
                        )

                        .dataStatus(
                                dataStatus
                        )

                        .methodologyVersion(
                                METHODOLOGY_VERSION
                        )

                        .confidence(
                                confidence
                        )

                        .uncertaintyNote(
                                uncertainty
                        )

                        .evaluatedAt(
                                LocalDateTime.now()
                        )

                        .validUntil(
                                LocalDateTime.now()
                                        .plusDays(3)
                        )

                        .build();

        return decisionRepository.save(
                result
        );
    }

    /*
     * -------------------------------------------------------------
     * Latest decision.
     * -------------------------------------------------------------
     */
    public SowingAdvisorDecision getLatest(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return decisionRepository
                .findFirstByFarmIdOrderByEvaluatedAtDesc(
                        farmId
                )
                .orElse(null);
    }

    /*
     * -------------------------------------------------------------
     * All decisions for farm.
     * -------------------------------------------------------------
     */
    public List<SowingAdvisorDecision> getByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return decisionRepository
                .findByFarmIdOrderByEvaluatedAtDesc(
                        farmId
                );
    }

    /*
     * -------------------------------------------------------------
     * Decision by ID.
     * -------------------------------------------------------------
     */
    public SowingAdvisorDecision getById(
            UUID id
    ) {

        return decisionRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sowing advisor decision not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * -------------------------------------------------------------
     * Current season screening.
     *
     * Rabi  -> Oct/Nov/Dec/Jan/Feb/Mar
     * Kharif -> Jun/Jul/Aug/Sep/Oct
     * Summer -> Mar/Apr/May/Jun
     *
     * This is a broad calendar screen only.
     * -------------------------------------------------------------
     */
    private boolean isCurrentSeasonCompatible(
            String cropSeason,
            LocalDate date
    ) {

        if (cropSeason == null
                || cropSeason.isBlank()) {

            return true;
        }

        String season =
                cropSeason
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        int month =
                date.getMonthValue();

        if (season.contains("RABI")) {

            return month == 10
                    || month == 11
                    || month == 12
                    || month == 1
                    || month == 2
                    || month == 3;
        }

        if (season.contains("KHARIF")) {

            return month == 6
                    || month == 7
                    || month == 8
                    || month == 9
                    || month == 10;
        }

        if (season.contains("SUMMER")
                || season.contains("ZAID")) {

            return month == 3
                    || month == 4
                    || month == 5
                    || month == 6;
        }

        /*
         * Unknown season is not treated as automatically
         * unsuitable.
         */
        return true;
    }

    /*
     * -------------------------------------------------------------
     * Temperature suitability.
     * -------------------------------------------------------------
     */
    private boolean isTemperatureSuitable(
            Double temperature,
            Double min,
            Double max
    ) {

        if (temperature == null
                || min == null
                || max == null) {

            return true;
        }

        return temperature >= min
                && temperature <= max;
    }

    /*
     * -------------------------------------------------------------
     * Soil pH suitability.
     * -------------------------------------------------------------
     */
    private boolean isSoilPhSuitable(
            Double ph,
            Double min,
            Double max
    ) {

        if (ph == null
                || min == null
                || max == null) {

            return true;
        }

        return ph >= min
                && ph <= max;
    }

    /*
     * -------------------------------------------------------------
     * Moisture screening.
     *
     * NOTE:
     * These broad thresholds are not crop-specific soil-water
     * calibration. They are only a first screening layer.
     *
     * <20%  -> TOO_DRY
     * 20-40 -> SUITABLE
     * >40%  -> TOO_WET
     * -------------------------------------------------------------
     */
    private MoistureStatus evaluateMoisture(
            Double moisture
    ) {

        if (moisture == null) {

            return MoistureStatus.UNKNOWN;
        }

        if (moisture < 20.0) {

            return MoistureStatus.TOO_DRY;
        }

        if (moisture <= 40.0) {

            return MoistureStatus.SUITABLE;
        }

        return MoistureStatus.TOO_WET;
    }

    /*
     * -------------------------------------------------------------
     * Confidence.
     * -------------------------------------------------------------
     */
    private double calculateConfidence(
            SoilSample soil,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry,
            boolean seasonCompatible,
            boolean temperatureSuitable,
            boolean soilPhSuitable
    ) {

        int available =
                0;

        int total =
                4;

        if (soil != null) {

            available++;
        }

        if (weather != null) {

            available++;
        }

        if (forecast != null) {

            available++;
        }

        if (telemetry != null) {

            available++;
        }

        double confidence =
                0.55
                        + (
                        (
                                (double) available
                                        / total
                        )
                                * 0.30
                );

        if (!seasonCompatible) {

            confidence += 0.05;
        }

        if (!temperatureSuitable) {

            confidence += 0.03;
        }

        if (!soilPhSuitable) {

            confidence += 0.03;
        }

        return Math.min(
                0.95,
                Math.max(
                        0.50,
                        confidence
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Data status.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry
    ) {

        boolean testForecast =
                forecast != null
                        && (
                        "TEST".equalsIgnoreCase(
                                forecast.getDataStatus()
                        )
                                || "TEST_FORECAST".equalsIgnoreCase(
                                forecast.getSource()
                        )
                );

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

        if (testForecast
                || testTelemetry) {

            return "TEST";
        }

        if (weather != null
                || forecast != null
                || telemetry != null) {

            return "MIXED";
        }

        return "UNKNOWN";
    }

    /*
     * -------------------------------------------------------------
     * Data used.
     * -------------------------------------------------------------
     */
    private String buildDataUsed(
            SoilSample soil,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry,
            CropHistory cropHistory
    ) {

        List<String> sources =
                new ArrayList<>();

        if (soil != null) {

            sources.add(
                    "SoilSample"
            );
        }

        if (weather != null) {

            sources.add(
                    "WeatherRecord"
            );
        }

        if (forecast != null) {

            sources.add(
                    "WeatherForecastData"
            );
        }

        if (telemetry != null) {

            sources.add(
                    "IrrigationTelemetry"
            );
        }

        if (cropHistory != null) {

            sources.add(
                    "CropHistory"
            );
        }

        sources.add(
                "Crop"
        );

        return String.join(
                ", ",
                sources
        );
    }

    /*
     * -------------------------------------------------------------
     * Uncertainty note.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            Crop crop,
            SoilSample soil,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "This sowing recommendation is decision-support only."
        );

        result.append(
                " The crop sowingWindow field is descriptive text and is not parsed as an exact calendar date."
        );

        result.append(
                " Moisture thresholds in this first version are broad screening thresholds and require local soil and crop calibration."
        );

        if (forecast != null
                && (
                "TEST".equalsIgnoreCase(
                        forecast.getDataStatus()
                )
                        || "TEST_FORECAST".equalsIgnoreCase(
                        forecast.getSource()
                )
        )) {

            result.append(
                    " Weather forecast is synthetic test data."
            );
        }

        if (telemetry != null
                && (
                "TEST".equalsIgnoreCase(
                        telemetry.getSensorStatus()
                )
                        || "TEST_SENSOR".equalsIgnoreCase(
                        telemetry.getSource()
                )
        )) {

            result.append(
                    " Irrigation telemetry is synthetic test data."
            );
        }

        if (soil == null) {

            result.append(
                    " No recent soil sample is available."
            );
        }

        if (weather == null) {

            result.append(
                    " No current weather record is available."
            );
        }

        result.append(
                " Confirm field workability and local agronomic guidance before sowing."
        );

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Farm verification.
     * -------------------------------------------------------------
     */
    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    /*
     * -------------------------------------------------------------
     * Moisture status.
     * -------------------------------------------------------------
     */
    private enum MoistureStatus {

        SUITABLE,
        TOO_DRY,
        TOO_WET,
        UNKNOWN
    }
}