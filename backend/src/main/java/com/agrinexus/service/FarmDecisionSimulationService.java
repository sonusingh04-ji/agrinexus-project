package com.agrinexus.service;

import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FarmDecisionSimulation;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmDecisionSimulationRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
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
public class FarmDecisionSimulationService {

    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-SIMULATION-V1";

    private final FarmRepository farmRepository;

    private final CropRepository cropRepository;

    private final CropHistoryRepository cropHistoryRepository;

    private final IrrigationTelemetryRepository irrigationTelemetryRepository;

    private final WeatherRecordRepository weatherRecordRepository;

    private final WeatherForecastDataRepository weatherForecastDataRepository;

    private final FarmDecisionSimulationRepository simulationRepository;

    /*
     * -------------------------------------------------------------
     * Main simulation.
     *
     * Supported comparison:
     *
     * IRRIGATE_NOW vs WAIT_FOR_RAIN
     *
     * -------------------------------------------------------------
     */
    @Transactional
    public FarmDecisionSimulation simulate(
            UUID farmId,
            UUID cropId,
            String scenarioA,
            String scenarioB
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

        Crop crop = null;

        if (cropId != null) {

            crop =
                    cropRepository.findById(
                            cropId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Crop not found with ID: "
                                            + cropId
                            )
                    );
        }

        /*
         * ---------------------------------------------------------
         * Current field context.
         * ---------------------------------------------------------
         */
        IrrigationTelemetry telemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
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

        CropHistory cropHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );

        /*
         * ---------------------------------------------------------
         * Normalize scenarios.
         * ---------------------------------------------------------
         */
        String normalizedA =
                normalizeScenario(
                        scenarioA
                );

        String normalizedB =
                normalizeScenario(
                        scenarioB
                );

        /*
         * ---------------------------------------------------------
         * Current moisture.
         * ---------------------------------------------------------
         */
        Double currentMoisture =
                telemetry != null
                        ? telemetry.getVwc30cm()
                        : null;

        /*
         * ---------------------------------------------------------
         * Forecast rainfall.
         * ---------------------------------------------------------
         */
        Double forecastRainfall =
                forecast != null
                        ? forecast.getRainfallMm()
                        : null;

        Double forecastConfidence =
                forecast != null
                        ? forecast.getForecastConfidence()
                        : null;

        /*
         * ---------------------------------------------------------
         * Evaluate scenario A.
         * ---------------------------------------------------------
         */
        ScenarioResult resultA =
                evaluateScenario(
                        normalizedA,
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence,
                        telemetry,
                        weather
                );

        /*
         * ---------------------------------------------------------
         * Evaluate scenario B.
         * ---------------------------------------------------------
         */
        ScenarioResult resultB =
                evaluateScenario(
                        normalizedB,
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence,
                        telemetry,
                        weather
                );

        /*
         * ---------------------------------------------------------
         * Compare scenarios.
         * ---------------------------------------------------------
         */
        ComparisonResult comparison =
                compareScenarios(
                        resultA,
                        resultB,
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence
                );

        /*
         * ---------------------------------------------------------
         * Water difference.
         *
         * This version uses a transparent normalized simulation
         * difference rather than pretending it knows an exact
         * irrigation volume when no validated volume model exists.
         * ---------------------------------------------------------
         */
        Double waterDifference =
                calculateWaterDifference(
                        resultA,
                        resultB
                );

        /*
         * ---------------------------------------------------------
         * Energy/cost.
         *
         * We only populate this when sufficient telemetry exists.
         *
         * Exact tariff/fuel cost is not available in the current
         * model, so we do not fabricate a rupee value.
         * ---------------------------------------------------------
         */
        Double energyCostDifference =
                null;

        /*
         * ---------------------------------------------------------
         * Timing comparison.
         * ---------------------------------------------------------
         */
        String timingDifference =
                buildTimingDifference(
                        resultA,
                        resultB,
                        forecast
                );

        /*
         * ---------------------------------------------------------
         * Explanations.
         * ---------------------------------------------------------
         */
        String scenarioAExplanation =
                buildScenarioExplanation(
                        resultA,
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence
                );

        String scenarioBExplanation =
                buildScenarioExplanation(
                        resultB,
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence
                );

        /*
         * ---------------------------------------------------------
         * Recommendation.
         * ---------------------------------------------------------
         */
        String recommendation =
                buildRecommendation(
                        comparison,
                        normalizedA,
                        normalizedB
                );

        /*
         * ---------------------------------------------------------
         * Assumptions.
         * ---------------------------------------------------------
         */
        String assumptions =
                buildAssumptions(
                        currentMoisture,
                        forecastRainfall,
                        forecastConfidence,
                        weather,
                        telemetry,
                        cropHistory
                );

        /*
         * ---------------------------------------------------------
         * Data used.
         * ---------------------------------------------------------
         */
        String dataUsed =
                buildDataUsed(
                        telemetry,
                        weather,
                        forecast,
                        cropHistory,
                        crop
                );

        /*
         * ---------------------------------------------------------
         * Data status.
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        telemetry,
                        forecast,
                        weather
                );

        /*
         * ---------------------------------------------------------
         * Confidence.
         * ---------------------------------------------------------
         */
        double confidence =
                calculateConfidence(
                        telemetry,
                        weather,
                        forecast,
                        crop
                );

        /*
         * ---------------------------------------------------------
         * Uncertainty.
         * ---------------------------------------------------------
         */
        String uncertaintyNote =
                buildUncertaintyNote(
                        telemetry,
                        forecast,
                        weather,
                        cropHistory
                );

        /*
         * ---------------------------------------------------------
         * Save simulation.
         * ---------------------------------------------------------
         */
        FarmDecisionSimulation simulation =
                FarmDecisionSimulation.builder()

                        .farm(farm)

                        .crop(crop)

                        .scenario(
                                normalizedA
                        )

                        .comparisonScenario(
                                normalizedB
                        )

                        .conclusion(
                                comparison.conclusion()
                        )

                        .currentMoisture(
                                currentMoisture
                        )

                        .forecastRainfall(
                                forecastRainfall
                        )

                        .forecastConfidence(
                                forecastConfidence
                        )

                        .waterDifference(
                                waterDifference
                        )

                        .energyCostDifference(
                                energyCostDifference
                        )

                        .timingDifference(
                                timingDifference
                        )

                        .scenarioAExplanation(
                                scenarioAExplanation
                        )

                        .scenarioBExplanation(
                                scenarioBExplanation
                        )

                        .recommendation(
                                recommendation
                        )

                        .assumptions(
                                assumptions
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
                                uncertaintyNote
                        )

                        .simulatedAt(
                                LocalDateTime.now()
                        )

                        .validUntil(
                                LocalDateTime.now()
                                        .plusDays(3)
                        )

                        .build();

        return simulationRepository.save(
                simulation
        );
    }

    /*
     * -------------------------------------------------------------
     * Scenario normalization.
     * -------------------------------------------------------------
     */
    private String normalizeScenario(
            String scenario
    ) {

        if (scenario == null
                || scenario.isBlank()) {

            return "UNKNOWN";
        }

        String value =
                scenario
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace(
                                " ",
                                "_"
                        );

        if (value.equals(
                "IRRIGATE"
        )
                || value.equals(
                "IRRIGATE_TODAY"
        )) {

            return "IRRIGATE_NOW";
        }

        if (value.equals(
                "WAIT"
        )
                || value.equals(
                "WAIT_FOR_RAIN"
        )
                || value.equals(
                "WAIT_RAIN"
        )) {

            return "WAIT_FOR_RAIN";
        }

        if (value.equals(
                "DELAY"
        )
                || value.equals(
                "DELAY_OPERATION"
        )) {

            return "DELAY_OPERATION";
        }

        return value;
    }

    /*
     * -------------------------------------------------------------
     * Evaluate individual scenario.
     * -------------------------------------------------------------
     */
    private ScenarioResult evaluateScenario(
            String scenario,
            Double currentMoisture,
            Double forecastRainfall,
            Double forecastConfidence,
            IrrigationTelemetry telemetry,
            WeatherRecord weather
    ) {

        /*
         * ---------------------------------------------------------
         * IRRIGATE_NOW
         * ---------------------------------------------------------
         */
        if ("IRRIGATE_NOW".equals(
                scenario
        )) {

            /*
             * If moisture is already adequate, irrigation has a
             * weaker simulated benefit.
             */
            if (currentMoisture != null
                    && currentMoisture >= 30.0) {

                return new ScenarioResult(
                        scenario,
                        45.0,
                        30.0,
                        "Current root-zone moisture is already in the screening range.",
                        false
                );
            }

            /*
             * Lower moisture makes immediate irrigation more
             * favorable in this screening model.
             */
            return new ScenarioResult(
                    scenario,
                    75.0,
                    60.0,
                    "Current root-zone moisture is below the preferred screening range.",
                    true
            );
        }

        /*
         * ---------------------------------------------------------
         * WAIT_FOR_RAIN
         * ---------------------------------------------------------
         */
        if ("WAIT_FOR_RAIN".equals(
                scenario
        )) {

            if (forecastRainfall != null
                    && forecastRainfall >= 20.0
                    && (
                    forecastConfidence == null
                            || forecastConfidence >= 0.70
            )) {

                return new ScenarioResult(
                        scenario,
                        80.0,
                        0.0,
                        "Forecast rainfall provides a meaningful reason to consider waiting before additional irrigation.",
                        false
                );
            }

            if (forecastRainfall != null
                    && forecastRainfall >= 10.0) {

                return new ScenarioResult(
                        scenario,
                        65.0,
                        0.0,
                        "Some forecast rainfall is available, but the rainfall signal is not strong enough for a high-confidence waiting decision.",
                        false
                );
            }

            return new ScenarioResult(
                    scenario,
                    40.0,
                    0.0,
                    "The available forecast does not provide a strong reason to wait for rainfall.",
                    false
            );
        }

        /*
         * ---------------------------------------------------------
         * DELAY_OPERATION
         * ---------------------------------------------------------
         */
        if ("DELAY_OPERATION".equals(
                scenario
        )) {

            if (weather != null
                    && weather.getWeatherCondition() != null) {

                String condition =
                        weather.getWeatherCondition()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                if (containsAny(
                        condition,
                        "extreme heat",
                        "severe heat",
                        "storm",
                        "heavy rain",
                        "strong wind"
                )) {

                    return new ScenarioResult(
                            scenario,
                            80.0,
                            0.0,
                            "Current weather conditions may be unsuitable for field operation.",
                            false
                    );
                }
            }

            return new ScenarioResult(
                    scenario,
                    55.0,
                    0.0,
                    "No strong weather-based reason to delay the operation was identified.",
                    false
            );
        }

        /*
         * ---------------------------------------------------------
         * Unknown scenario.
         * ---------------------------------------------------------
         */
        return new ScenarioResult(
                scenario,
                50.0,
                0.0,
                "The scenario is not supported by the current simulation methodology.",
                false
        );
    }

    /*
     * -------------------------------------------------------------
     * Compare scenarios.
     * -------------------------------------------------------------
     */
    private ComparisonResult compareScenarios(
            ScenarioResult a,
            ScenarioResult b,
            Double currentMoisture,
            Double forecastRainfall,
            Double forecastConfidence
    ) {

        double difference =
                Math.abs(
                        a.score()
                                - b.score()
                );

        if ("UNKNOWN".equals(
                a.scenario()
        )
                || "UNKNOWN".equals(
                b.scenario()
        )) {

            return new ComparisonResult(
                    "INSUFFICIENT_DATA"
            );
        }

        if (difference < 5.0) {

            return new ComparisonResult(
                    "TOO_CLOSE_TO_CALL"
            );
        }

        if (a.score() > b.score()) {

            return new ComparisonResult(
                    "SCENARIO_A_FAVORED"
            );
        }

        return new ComparisonResult(
                "SCENARIO_B_FAVORED"
        );
    }

    /*
     * -------------------------------------------------------------
     * Water comparison.
     *
     * This is a normalized scenario difference, NOT an estimated
     * number of litres unless a validated irrigation-volume model
     * is connected.
     * -------------------------------------------------------------
     */
    private Double calculateWaterDifference(
            ScenarioResult a,
            ScenarioResult b
    ) {

        double difference =
                Math.abs(
                        a.waterDemandIndex()
                                - b.waterDemandIndex()
                );

        return round2(
                difference
        );
    }

    /*
     * -------------------------------------------------------------
     * Timing difference.
     * -------------------------------------------------------------
     */
    private String buildTimingDifference(
            ScenarioResult a,
            ScenarioResult b,
            WeatherForecastData forecast
    ) {

        if ("WAIT_FOR_RAIN".equals(
                a.scenario()
        )
                && forecast != null) {

            return
                    "Scenario A waits for the available forecast rainfall before considering additional irrigation.";
        }

        if ("WAIT_FOR_RAIN".equals(
                b.scenario()
        )
                && forecast != null) {

            return
                    "Scenario B waits for the available forecast rainfall before considering additional irrigation.";
        }

        if ("IRRIGATE_NOW".equals(
                a.scenario()
        )
                && "WAIT_FOR_RAIN".equals(
                b.scenario()
        )) {

            return
                    "Irrigate-now acts immediately; wait-for-rain delays the decision until the forecast rainfall opportunity passes.";
        }

        if ("WAIT_FOR_RAIN".equals(
                a.scenario()
        )
                && "IRRIGATE_NOW".equals(
                b.scenario()
        )) {

            return
                    "Wait-for-rain delays the action; irrigate-now acts immediately.";
        }

        return
                "Timing difference depends on the selected scenarios and current field conditions.";
    }

    /*
     * -------------------------------------------------------------
     * Scenario explanation.
     * -------------------------------------------------------------
     */
    private String buildScenarioExplanation(
            ScenarioResult result,
            Double currentMoisture,
            Double forecastRainfall,
            Double forecastConfidence
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                result.scenario()
        );

        text.append(
                " scenario score="
        );

        text.append(
                round2(
                        result.score()
                )
        );

        text.append(
                "/100. "
        );

        text.append(
                result.reason()
        );

        if (currentMoisture != null) {

            text.append(
                    " Current moisture="
            );

            text.append(
                    currentMoisture
            );

            text.append(
                    "%."
            );
        }

        if (forecastRainfall != null) {

            text.append(
                    " Forecast rainfall="
            );

            text.append(
                    forecastRainfall
            );

            text.append(
                    " mm."
            );
        }

        if (forecastConfidence != null) {

            text.append(
                    " Forecast confidence="
            );

            text.append(
                    forecastConfidence * 100.0
            );

            text.append(
                    "%."
            );
        }

        return text.toString();
    }

    /*
     * -------------------------------------------------------------
     * Recommendation.
     * -------------------------------------------------------------
     */
    private String buildRecommendation(
            ComparisonResult comparison,
            String scenarioA,
            String scenarioB
    ) {

        return switch (
                comparison.conclusion()
                ) {

            case "SCENARIO_A_FAVORED" ->
                    "The available information currently favors Scenario A ("
                            + scenarioA
                            + "). Review the assumptions and confirm field conditions before acting.";

            case "SCENARIO_B_FAVORED" ->
                    "The available information currently favors Scenario B ("
                            + scenarioB
                            + "). Review the assumptions and confirm field conditions before acting.";

            case "TOO_CLOSE_TO_CALL" ->
                    "The two scenarios are too close under the current screening model. Recheck field conditions and the latest forecast.";

            default ->
                    "There is not enough reliable information to confidently favor either scenario.";
        };
    }

    /*
     * -------------------------------------------------------------
     * Assumptions.
     * -------------------------------------------------------------
     */
    private String buildAssumptions(
            Double currentMoisture,
            Double forecastRainfall,
            Double forecastConfidence,
            WeatherRecord weather,
            IrrigationTelemetry telemetry,
            CropHistory cropHistory
    ) {

        List<String> assumptions =
                new ArrayList<>();

        if (currentMoisture != null) {

            assumptions.add(
                    "Current VWC is used as a field-moisture screening signal."
            );
        } else {

            assumptions.add(
                    "Current VWC is unavailable."
            );
        }

        if (forecastRainfall != null) {

            assumptions.add(
                    "Forecast rainfall is treated as an opportunity signal rather than a guaranteed rainfall event."
            );
        } else {

            assumptions.add(
                    "Forecast rainfall is unavailable."
            );
        }

        if (forecastConfidence != null) {

            assumptions.add(
                    "Forecast confidence is considered when evaluating the wait-for-rain scenario."
            );
        }

        if (weather != null) {

            assumptions.add(
                    "Current weather condition is considered for operation-delay scenarios."
            );
        }

        if (telemetry != null) {

            assumptions.add(
                    "Latest irrigation telemetry is used for current moisture context."
            );
        }

        if (cropHistory != null) {

            assumptions.add(
                    "Latest crop-history information is available but this V1 simulator does not estimate yield."
            );
        }

        assumptions.add(
                "No exact irrigation volume or rupee saving is fabricated when validated pump-flow, duration and tariff data are unavailable."
        );

        return String.join(
                " ",
                assumptions
        );
    }

    /*
     * -------------------------------------------------------------
     * Data sources.
     * -------------------------------------------------------------
     */
    private String buildDataUsed(
            IrrigationTelemetry telemetry,
            WeatherRecord weather,
            WeatherForecastData forecast,
            CropHistory cropHistory,
            Crop crop
    ) {

        List<String> sources =
                new ArrayList<>();

        if (telemetry != null) {

            sources.add(
                    "IrrigationTelemetry"
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

        if (cropHistory != null) {

            sources.add(
                    "CropHistory"
            );
        }

        if (crop != null) {

            sources.add(
                    "Crop"
            );
        }

        if (sources.isEmpty()) {

            return "No source data available.";
        }

        return String.join(
                ", ",
                sources
        );
    }

    /*
     * -------------------------------------------------------------
     * Data status.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            IrrigationTelemetry telemetry,
            WeatherForecastData forecast,
            WeatherRecord weather
    ) {

        boolean test =
                false;

        if (telemetry != null
                && (
                "TEST".equalsIgnoreCase(
                        telemetry.getSensorStatus()
                )
                        || "TEST_SENSOR".equalsIgnoreCase(
                        telemetry.getSource()
                )
        )) {

            test = true;
        }

        if (forecast != null
                && (
                "TEST".equalsIgnoreCase(
                        forecast.getDataStatus()
                )
                        || "TEST_FORECAST".equalsIgnoreCase(
                        forecast.getSource()
                )
        )) {

            test = true;
        }

        if (test) {

            return "TEST";
        }

        if (telemetry != null
                || forecast != null
                || weather != null) {

            return "MIXED";
        }

        return "UNKNOWN";
    }

    /*
     * -------------------------------------------------------------
     * Confidence.
     * -------------------------------------------------------------
     */
    private double calculateConfidence(
            IrrigationTelemetry telemetry,
            WeatherRecord weather,
            WeatherForecastData forecast,
            Crop crop
    ) {

        int available =
                0;

        int total =
                4;

        if (telemetry != null) {
            available++;
        }

        if (weather != null) {
            available++;
        }

        if (forecast != null) {
            available++;
        }

        if (crop != null) {
            available++;
        }

        double confidence =
                0.50
                        + (
                        (
                                (double) available
                                        / total
                        )
                                * 0.40
                );

        return Math.min(
                0.90,
                Math.max(
                        0.50,
                        confidence
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Uncertainty.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            IrrigationTelemetry telemetry,
            WeatherForecastData forecast,
            WeatherRecord weather,
            CropHistory cropHistory
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "This farm decision simulator is a decision-support tool, not a guaranteed prediction of future outcomes."
        );

        text.append(
                " Scenario scores are rule-based under methodology version "
        );

        text.append(
                METHODOLOGY_VERSION
        );

        text.append(
                "."
        );

        if (forecast != null) {

            text.append(
                    " Forecast rainfall is uncertain and should not be treated as guaranteed."
            );
        }

        if (telemetry == null) {

            text.append(
                    " No current irrigation telemetry is available."
            );
        }

        if (weather == null) {

            text.append(
                    " No current weather record is available."
            );
        }

        if (cropHistory == null) {

            text.append(
                    " No recent crop-history record is available."
            );
        }

        text.append(
                " Exact water savings, energy savings and monetary savings require validated field-specific flow, pump, runtime and energy/fuel-cost data."
        );

        text.append(
                " Confirm important decisions using current field observations and appropriate agricultural guidance."
        );

        return text.toString();
    }

    /*
     * -------------------------------------------------------------
     * Latest simulation.
     * -------------------------------------------------------------
     */
    public FarmDecisionSimulation getLatest(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return simulationRepository
                .findFirstByFarmIdOrderBySimulatedAtDesc(
                        farmId
                )
                .orElse(null);
    }

    /*
     * -------------------------------------------------------------
     * All simulations.
     * -------------------------------------------------------------
     */
    public List<FarmDecisionSimulation> getByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return simulationRepository
                .findByFarmIdOrderBySimulatedAtDesc(
                        farmId
                );
    }

    /*
     * -------------------------------------------------------------
     * Simulation by ID.
     * -------------------------------------------------------------
     */
    public FarmDecisionSimulation getById(
            UUID id
    ) {

        return simulationRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Farm decision simulation not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * -------------------------------------------------------------
     * Utility.
     * -------------------------------------------------------------
     */
    private boolean containsAny(
            String text,
            String... keywords
    ) {

        if (text == null) {
            return false;
        }

        for (String keyword : keywords) {

            if (text.contains(
                    keyword
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * -------------------------------------------------------------
     * Round.
     * -------------------------------------------------------------
     */
    private double round2(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
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
     * Scenario result.
     * -------------------------------------------------------------
     */
    private record ScenarioResult(
            String scenario,
            double score,
            double waterDemandIndex,
            String reason,
            boolean irrigationRequired
    ) {
    }

    /*
     * -------------------------------------------------------------
     * Comparison result.
     * -------------------------------------------------------------
     */
    private record ComparisonResult(
            String conclusion
    ) {
    }
}