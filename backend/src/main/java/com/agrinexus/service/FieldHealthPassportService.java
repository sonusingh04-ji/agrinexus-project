package com.agrinexus.service;

import com.agrinexus.dto.DigitalMemoryResponse;
import com.agrinexus.dto.FieldHealthPassportResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SoilRecoveryPlan;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilRecoveryPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FieldHealthPassportService {

    private final FarmRepository farmRepository;

    private final FarmDigitalMemoryService digitalMemoryService;

    private final SoilRecoveryPlanRepository recoveryPlanRepository;


    public FieldHealthPassportResponse getPassport(
            UUID farmId
    ) {

        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * =========================================================
         * Get aggregated historical memory
         * =========================================================
         */

        DigitalMemoryResponse memory =
                digitalMemoryService.getFarmMemory(farmId);


        /*
         * =========================================================
         * Latest crop
         * =========================================================
         */

        DigitalMemoryResponse.CropMemoryItem latestCrop =
                memory.getCrops() == null
                        ? null
                        : memory.getCrops()
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * =========================================================
         * Latest soil
         * =========================================================
         */

        DigitalMemoryResponse.SoilMemoryItem latestSoil =
                memory.getSoil() == null
                        ? null
                        : memory.getSoil()
                        .stream()
                        .filter(item ->
                                item.getSampleDate() != null
                        )
                        .findFirst()
                        .orElse(
                                memory.getSoil() == null
                                        ? null
                                        : memory.getSoil()
                                        .stream()
                                        .findFirst()
                                        .orElse(null)
                        );


        /*
         * =========================================================
         * Latest irrigation telemetry
         * =========================================================
         */

        DigitalMemoryResponse.IrrigationTelemetryMemoryItem
                latestTelemetry =
                memory.getIrrigationTelemetry() == null
                        ? null
                        : memory.getIrrigationTelemetry()
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * =========================================================
         * Latest weather
         * =========================================================
         */

        DigitalMemoryResponse.WeatherMemoryItem latestWeather =
                memory.getWeather() == null
                        ? null
                        : memory.getWeather()
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * =========================================================
         * Latest nutrient balance
         * =========================================================
         */

        DigitalMemoryResponse.NutrientBalanceMemoryItem latestBalance =
                memory.getNutrientBalances() == null
                        ? null
                        : memory.getNutrientBalances()
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * =========================================================
         * Highest/current weather risk
         * =========================================================
         */

        DigitalMemoryResponse.WeatherRiskMemoryItem highestRisk =
                memory.getWeatherRisks() == null
                        ? null
                        : memory.getWeatherRisks()
                        .stream()
                        .max(
                                Comparator.comparing(
                                        risk ->
                                                risk.getRiskScore() != null
                                                        ? risk.getRiskScore()
                                                        : 0.0
                                )
                        )
                        .orElse(null);


        /*
         * =========================================================
         * Recovery plans
         * =========================================================
         */

        List<SoilRecoveryPlan> recoveryEntities =
                recoveryPlanRepository
                        .findByFarmIdOrderBySeasonNumberAsc(
                                farmId
                        );


        List<FieldHealthPassportResponse.RecoveryPassportItem>
                recoveryPlans =
                recoveryEntities
                        .stream()
                        .map(plan ->
                                FieldHealthPassportResponse
                                        .RecoveryPassportItem
                                        .builder()
                                        .id(plan.getId())
                                        .seasonNumber(
                                                plan.getSeasonNumber()
                                        )
                                        .season(plan.getSeason())
                                        .startDate(
                                                plan.getStartDate()
                                        )
                                        .endDate(
                                                plan.getEndDate()
                                        )
                                        .focusArea(
                                                plan.getFocusArea()
                                        )
                                        .baselineValue(
                                                plan.getBaselineValue()
                                        )
                                        .currentValue(
                                                plan.getCurrentValue()
                                        )
                                        .targetValue(
                                                plan.getTargetValue()
                                        )
                                        .unit(plan.getUnit())
                                        .trend(plan.getTrend())
                                        .actionType(
                                                plan.getActionType()
                                        )
                                        .recommendation(
                                                plan.getRecommendation()
                                        )
                                        .status(plan.getStatus())
                                        .dataStatus(
                                                plan.getDataStatus()
                                        )
                                        .build()
                        )
                        .toList();


        /*
         * =========================================================
         * Historical crop timeline
         * =========================================================
         */

        List<FieldHealthPassportResponse.CropPassportItem>
                cropHistory =
                memory.getCrops() == null
                        ? List.of()
                        : memory.getCrops()
                        .stream()
                        .map(crop ->
                                FieldHealthPassportResponse
                                        .CropPassportItem
                                        .builder()
                                        .id(crop.getId())
                                        .cropId(crop.getCropId())
                                        .cropName(crop.getCropName())
                                        .season(crop.getSeason())
                                        .sowingDate(crop.getSowingDate())
                                        .harvestDate(crop.getHarvestDate())
                                        .cropStage(crop.getCropStage())
                                        .residuePresent(
                                                crop.getResiduePresent()
                                        )
                                        .residueType(
                                                crop.getResidueType()
                                        )
                                        .build()
                        )
                        .toList();


        /*
         * =========================================================
         * Soil trend
         * =========================================================
         */

        String soilTrend =
                determineSoilTrend(
                        recoveryEntities,
                        latestSoil
                );


        /*
         * =========================================================
         * Health score
         * =========================================================
         */

        Double healthScore =
                calculateHealthScore(
                        latestSoil,
                        latestTelemetry,
                        latestBalance,
                        highestRisk,
                        recoveryEntities
                );


        String overallHealthStatus =
                calculateHealthStatus(
                        healthScore,
                        latestSoil
                );


        String healthSummary =
                buildHealthSummary(
                        overallHealthStatus,
                        latestSoil,
                        latestTelemetry,
                        latestBalance,
                        highestRisk,
                        recoveryEntities
                );


        /*
         * =========================================================
         * Latest recovery plan
         * =========================================================
         */

        SoilRecoveryPlan latestRecovery =
                recoveryEntities
                        .stream()
                        .max(
                                Comparator.comparing(
                                        plan ->
                                                plan.getSeasonNumber() != null
                                                        ? plan.getSeasonNumber()
                                                        : 0
                                )
                        )
                        .orElse(null);


        /*
         * =========================================================
         * Final Passport
         * =========================================================
         */

        return FieldHealthPassportResponse.builder()

                .farmId(farm.getId())
                .farmName(farm.getName())
                .location(farm.getLocation())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .generatedAt(LocalDateTime.now())

                .overallHealthStatus(
                        overallHealthStatus
                )

                .healthScore(
                        healthScore
                )

                .healthSummary(
                        healthSummary
                )

                .currentCropId(
                        latestCrop != null
                                ? latestCrop.getCropId()
                                : null
                )

                .currentCropName(
                        latestCrop != null
                                ? latestCrop.getCropName()
                                : null
                )

                .currentSeason(
                        latestCrop != null
                                ? latestCrop.getSeason()
                                : null
                )

                .currentCropStage(
                        latestCrop != null
                                ? latestCrop.getCropStage()
                                : null
                )

                .latestSoilSampleId(
                        latestSoil != null
                                ? latestSoil.getId()
                                : null
                )

                .latestSoilSampleDate(
                        latestSoil != null
                                ? latestSoil.getSampleDate()
                                : null
                )

                .soilType(
                        latestSoil != null
                                ? latestSoil.getSoilType()
                                : null
                )

                .soilPh(
                        latestSoil != null
                                ? latestSoil.getPH()
                                : null
                )

                .electricalConductivity(
                        latestSoil != null
                                ? latestSoil.getElectricalConductivity()
                                : null
                )

                .organicCarbon(
                        latestSoil != null
                                ? latestSoil.getOrganicCarbon()
                                : null
                )

                .soilNitrogen(
                        latestSoil != null
                                ? latestSoil.getNitrogen()
                                : null
                )

                .soilPhosphorus(
                        latestSoil != null
                                ? latestSoil.getPhosphorus()
                                : null
                )

                .soilPotassium(
                        latestSoil != null
                                ? latestSoil.getPotassium()
                                : null
                )

                .soilZinc(
                        latestSoil != null
                                ? latestSoil.getZinc()
                                : null
                )

                .soilSulphur(
                        latestSoil != null
                                ? latestSoil.getSulphur()
                                : null
                )

                .soilIron(
                        latestSoil != null
                                ? latestSoil.getIron()
                                : null
                )

                .soilManganese(
                        latestSoil != null
                                ? latestSoil.getManganese()
                                : null
                )

                .soilCopper(
                        latestSoil != null
                                ? latestSoil.getCopper()
                                : null
                )

                .soilSource(
                        latestSoil != null
                                ? latestSoil.getSource()
                                : null
                )

                .soilTrend(
                        soilTrend
                )

                .recoveryFocusArea(
                        latestRecovery != null
                                ? latestRecovery.getFocusArea()
                                : null
                )

                .recoveryBaseline(
                        latestRecovery != null
                                ? latestRecovery.getBaselineValue()
                                : null
                )

                .recoveryCurrent(
                        latestRecovery != null
                                ? latestRecovery.getCurrentValue()
                                : null
                )

                .recoveryTarget(
                        latestRecovery != null
                                ? latestRecovery.getTargetValue()
                                : null
                )

                .recoveryUnit(
                        latestRecovery != null
                                ? latestRecovery.getUnit()
                                : null
                )

                .recoveryAction(
                        latestRecovery != null
                                ? latestRecovery.getActionType()
                                : null
                )

                .recoveryRecommendation(
                        latestRecovery != null
                                ? latestRecovery.getRecommendation()
                                : null
                )

                .latestIrrigationReadingAt(
                        latestTelemetry != null
                                ? latestTelemetry.getRecordedAt()
                                : null
                )

                .currentVwc15cm(
                        latestTelemetry != null
                                ? latestTelemetry.getVwc15cm()
                                : null
                )

                .currentVwc30cm(
                        latestTelemetry != null
                                ? latestTelemetry.getVwc30cm()
                                : null
                )

                .currentVwc60cm(
                        latestTelemetry != null
                                ? latestTelemetry.getVwc60cm()
                                : null
                )

                .pumpState(
                        latestTelemetry != null
                                ? latestTelemetry.getPumpState()
                                : null
                )

                .rainfallMm(
                        latestTelemetry != null
                                ? latestTelemetry.getRainfallMm()
                                : null
                )

                .et0Mm(
                        latestTelemetry != null
                                ? latestTelemetry.getEt0Mm()
                                : null
                )

                .etcMm(
                        latestTelemetry != null
                                ? latestTelemetry.getEtcMm()
                                : null
                )

                .irrigationSource(
                        latestTelemetry != null
                                ? latestTelemetry.getSource()
                                : null
                )

                .irrigationSensorStatus(
                        latestTelemetry != null
                                ? latestTelemetry.getSensorStatus()
                                : null
                )

                .nitrogenBalance(
                        latestBalance != null
                                ? latestBalance.getNitrogenBalance()
                                : null
                )

                .phosphorusBalance(
                        latestBalance != null
                                ? latestBalance.getPhosphorusBalance()
                                : null
                )

                .potassiumBalance(
                        latestBalance != null
                                ? latestBalance.getPotassiumBalance()
                                : null
                )

                .sulphurBalance(
                        latestBalance != null
                                ? latestBalance.getSulphurBalance()
                                : null
                )

                .zincBalance(
                        latestBalance != null
                                ? latestBalance.getZincBalance()
                                : null
                )

                .nitrogenStatus(
                        getStatus(
                                latestBalance,
                                "N"
                        )
                )

                .phosphorusStatus(
                        getStatus(
                                latestBalance,
                                "P"
                        )
                )

                .potassiumStatus(
                        getStatus(
                                latestBalance,
                                "K"
                        )
                )

                .sulphurStatus(
                        getStatus(
                                latestBalance,
                                "S"
                        )
                )

                .zincStatus(
                        getStatus(
                                latestBalance,
                                "ZN"
                        )
                )

                .latestWeatherTime(
                        latestWeather != null
                                ? latestWeather.getRecordedAt()
                                : null
                )

                .latestTemperature(
                        latestWeather != null
                                ? latestWeather.getTemperature()
                                : null
                )

                .latestHumidity(
                        latestWeather != null
                                ? latestWeather.getHumidity() != null
                                ? latestWeather.getHumidity().doubleValue()
                                : null
                                : null
                )

                .latestRainfall(
                        latestWeather != null
                                ? latestWeather.getRainfall()
                                : null
                )

                .latestWindSpeed(
                        latestWeather != null
                                ? latestWeather.getWindSpeed()
                                : null
                )

                .latestWeatherCondition(
                        latestWeather != null
                                ? latestWeather.getWeatherCondition()
                                : null
                )

                .activeWeatherRiskCount(
                        memory.getWeatherRisks() != null
                                ? (int) memory.getWeatherRisks()
                                .stream()
                                .filter(risk ->
                                        risk.getValidUntil() == null
                                                || risk.getValidUntil()
                                                .isAfter(
                                                        LocalDateTime.now()
                                                )
                                )
                                .count()
                                : 0
                )

                .highestWeatherRisk(
                        highestRisk != null
                                ? highestRisk.getRiskType()
                                : null
                )

                .highestWeatherRiskScore(
                        highestRisk != null
                                ? highestRisk.getRiskScore()
                                : null
                )

                .cropHistoryCount(
                        memory.getCropHistoryCount()
                )

                .soilSampleCount(
                        memory.getSoilSampleCount()
                )

                .irrigationEventCount(
                        memory.getIrrigationEventCount()
                )

                .irrigationTelemetryCount(
                        memory.getIrrigationTelemetryCount()
                )

                .fertilizerRecommendationCount(
                        memory.getFertilizerRecommendationCount()
                )

                .fertilizerScheduleCount(
                        memory.getFertilizerScheduleCount()
                )

                .weatherRecordCount(
                        memory.getWeatherRecordCount()
                )

                .nutrientBalanceCount(
                        memory.getNutrientBalanceCount()
                )

                .weatherRiskCount(
                        memory.getWeatherRiskCount()
                )

                .recoveryPlanCount(
                        recoveryPlans.size()
                )

                .cropHistory(
                        cropHistory
                )

                .recoveryPlans(
                        recoveryPlans
                )

                .dataStatus("MIXED")

                .source(
                        "AGRINEXUS_DIGITAL_MEMORY_AND_SOIL_RECOVERY"
                )

                .uncertaintyNote(
                        "This Field Health Passport is a "
                                + "long-term decision-support summary "
                                + "built from stored farm records. "
                                + "It may contain measured, calculated, "
                                + "estimated and test data. The health "
                                + "score is a transparent summary "
                                + "indicator and is not a yield prediction "
                                + "or medical-style diagnosis of the field. "
                                + "Important agricultural decisions "
                                + "should be validated against current "
                                + "soil tests, field observations and "
                                + "local agronomic guidance."
                )

                .build();
    }


    /*
     * =========================================================
     * Soil trend
     * =========================================================
     */

    private String determineSoilTrend(
            List<SoilRecoveryPlan> plans,
            DigitalMemoryResponse.SoilMemoryItem latestSoil
    ) {

        if (plans != null && !plans.isEmpty()) {

            SoilRecoveryPlan latest =
                    plans.stream()
                            .max(
                                    Comparator.comparing(
                                            plan ->
                                                    plan.getSeasonNumber() != null
                                                            ? plan.getSeasonNumber()
                                                            : 0
                                    )
                            )
                            .orElse(null);

            if (latest != null
                    && latest.getTrend() != null) {

                return latest.getTrend();
            }
        }

        if (latestSoil == null) {
            return "INSUFFICIENT_DATA";
        }

        return "STABLE";
    }


    /*
     * =========================================================
     * Health score
     *
     * This is intentionally a simple transparent indicator.
     * It is NOT a scientific soil-health index.
     * =========================================================
     */

    private Double calculateHealthScore(
            DigitalMemoryResponse.SoilMemoryItem soil,
            DigitalMemoryResponse.IrrigationTelemetryMemoryItem telemetry,
            DigitalMemoryResponse.NutrientBalanceMemoryItem balance,
            DigitalMemoryResponse.WeatherRiskMemoryItem risk,
            List<SoilRecoveryPlan> recoveryPlans
    ) {

        double score = 50.0;


        /*
         * Soil-data availability
         */
        if (soil != null) {
            score += 15.0;
        }


        /*
         * Irrigation/current moisture availability
         */
        if (telemetry != null) {

            if (telemetry.getVwc30cm() != null) {
                score += 10.0;
            }

            if (telemetry.getSensorStatus() != null
                    && "TEST".equalsIgnoreCase(
                    telemetry.getSensorStatus()
            )) {

                score -= 2.0;
            }
        }


        /*
         * Nutrient balance
         */
        if (balance != null) {

            if (isPositiveOrBalanced(
                    calculateStatus(
                            balance.getNitrogenBalance()
                    )
            )) {
                score += 4.0;
            }

            if (isPositiveOrBalanced(
                    calculateStatus(
                            balance.getPhosphorusBalance()
                    )
            )) {
                score += 4.0;
            }

            if (isPositiveOrBalanced(
                    calculateStatus(
                            balance.getPotassiumBalance()
                    )
            )) {
                score += 4.0;
            }
        }


        /*
         * Current risk
         */
        if (risk != null
                && risk.getRiskScore() != null) {

            score -=
                    risk.getRiskScore() * 0.15;
        }


        /*
         * Recovery-plan availability
         */
        if (recoveryPlans != null
                && !recoveryPlans.isEmpty()) {

            score += 5.0;
        }


        score =
                Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                score
                        )
                );


        return Math.round(
                score * 100.0
        ) / 100.0;
    }


    private boolean isPositiveOrBalanced(
            String status
    ) {

        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("POSITIVE")
                || status.equalsIgnoreCase("BALANCED")
                || status.equalsIgnoreCase("STABLE");
    }


    /*
     * =========================================================
     * Overall health classification
     * =========================================================
     */

    private String calculateHealthStatus(
            Double score,
            DigitalMemoryResponse.SoilMemoryItem soil
    ) {

        if (score == null
                || soil == null) {

            return "INSUFFICIENT_DATA";
        }

        if (score >= 75) {
            return "HEALTHY";
        }

        if (score >= 60) {
            return "STABLE";
        }

        if (score >= 40) {
            return "ATTENTION_REQUIRED";
        }

        return "CRITICAL";
    }


    /*
     * =========================================================
     * Farmer-facing health summary
     * =========================================================
     */

    private String buildHealthSummary(
            String healthStatus,
            DigitalMemoryResponse.SoilMemoryItem soil,
            DigitalMemoryResponse.IrrigationTelemetryMemoryItem telemetry,
            DigitalMemoryResponse.NutrientBalanceMemoryItem balance,
            DigitalMemoryResponse.WeatherRiskMemoryItem risk,
            List<SoilRecoveryPlan> recoveryPlans
    ) {

        StringBuilder summary =
                new StringBuilder();


        summary.append(
                "Field health status: "
        );

        summary.append(
                healthStatus
        );


        if (soil != null) {

            summary.append(
                    ". Latest soil record"
                            + " is available"
            );

            if (soil.getPH() != null) {

                summary.append(
                        " with pH "
                                + soil.getPH()
                );
            }

            if (soil.getOrganicCarbon() != null) {

                summary.append(
                        " and organic carbon "
                                + soil.getOrganicCarbon()
                                + "%."
                );
            }
        }
        else {

            summary.append(
                    ". No usable soil record "
                            + "is currently available."
            );
        }


        if (telemetry != null
                && telemetry.getVwc30cm() != null) {

            summary.append(
                    " Latest 30 cm soil moisture "
                            + "reading is "
                            + telemetry.getVwc30cm()
                            + "%."
            );
        }


        if (balance != null) {

            summary.append(
                    " Nutrient balance records "
                            + "are available."
            );
        }


        if (risk != null) {

            summary.append(
                    " Latest recorded weather risk "
                            + "is "
                            + risk.getRiskType()
                            + " with score "
                            + risk.getRiskScore()
                            + "."
            );
        }


        if (recoveryPlans != null
                && !recoveryPlans.isEmpty()) {

            summary.append(
                    " A soil recovery roadmap "
                            + "is available for "
                            + recoveryPlans.size()
                            + " phase(s)."
            );
        }


        return summary.toString();
    }


    /*
     * =========================================================
     * Nutrient status helper
     * =========================================================
     */

    private String getStatus(
            DigitalMemoryResponse.NutrientBalanceMemoryItem balance,
            String nutrient
    ) {

        if (balance == null) {
            return null;
        }

        return switch (nutrient) {

            case "N" ->
                    calculateStatus(
                            balance.getNitrogenBalance()
                    );

            case "P" ->
                    calculateStatus(
                            balance.getPhosphorusBalance()
                    );

            case "K" ->
                    calculateStatus(
                            balance.getPotassiumBalance()
                    );

            case "S" ->
                    calculateStatus(
                            balance.getSulphurBalance()
                    );

            case "ZN" ->
                    calculateStatus(
                            balance.getZincBalance()
                    );

            default -> null;
        };
    }


    private String calculateStatus(
            Double value
    ) {

        if (value == null) {
            return null;
        }

        if (value > 0) {
            return "POSITIVE";
        }

        if (value == 0) {
            return "BALANCED";
        }

        return "NEGATIVE";
    }
}