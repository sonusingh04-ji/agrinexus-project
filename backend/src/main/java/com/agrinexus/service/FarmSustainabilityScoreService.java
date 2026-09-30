package com.agrinexus.service;

import com.agrinexus.dto.SmartIrrigationRequest;
import com.agrinexus.dto.SmartIrrigationResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.DiseasePestRisk;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FarmSustainabilityScore;
import com.agrinexus.entity.FertilizerRecommendation;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.NutrientBalance;
import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.DiseasePestRiskRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.FarmSustainabilityScoreRepository;
import com.agrinexus.repository.FertilizerRecommendationRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.NutrientBalanceRepository;
import com.agrinexus.repository.SatelliteObservationRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FarmSustainabilityScoreService {

    private static final double SOIL_WEIGHT = 0.25;
    private static final double WATER_WEIGHT = 0.25;
    private static final double ENERGY_WEIGHT = 0.15;
    private static final double CROP_WEIGHT = 0.20;
    private static final double FIELD_WEIGHT = 0.15;

    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-SUS-V1";

    private final FarmRepository farmRepository;

    private final SoilSampleRepository soilSampleRepository;

    private final CropHistoryRepository cropHistoryRepository;

    private final IrrigationTelemetryRepository irrigationTelemetryRepository;

    private final FertilizerRecommendationRepository fertilizerRecommendationRepository;

    private final NutrientBalanceRepository nutrientBalanceRepository;

    private final SatelliteObservationRepository satelliteObservationRepository;

    private final DiseasePestRiskRepository diseasePestRiskRepository;

    private final FarmSustainabilityScoreRepository scoreRepository;

    private final SmartIrrigationService smartIrrigationService;

    /*
     * -------------------------------------------------------------
     * Evaluate sustainability.
     * -------------------------------------------------------------
     */
    @Transactional
    public FarmSustainabilityScore evaluate(
            UUID farmId
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

        /*
         * ---------------------------------------------------------
         * Latest source records.
         * ---------------------------------------------------------
         */
        SoilSample soil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );

        CropHistory cropHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );

        IrrigationTelemetry telemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        FertilizerRecommendation fertilizer =
                fertilizerRecommendationRepository
                        .findFirstByFarmIdOrderByGeneratedAtDesc(
                                farmId
                        );

        NutrientBalance nutrient =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                farmId
                        );

        SatelliteObservation satellite =
                satelliteObservationRepository
                        .findFirstByFarmIdOrderByObservedAtDesc(
                                farmId
                        )
                        .orElse(null);

        DiseasePestRisk diseasePestRisk =
                diseasePestRiskRepository
                        .findByFarmIdOrderByEvaluatedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        /*
         * ---------------------------------------------------------
         * Member 2 irrigation decision.
         * ---------------------------------------------------------
         */
        SmartIrrigationResponse irrigation =
                buildSmartIrrigationResponse(
                        farmId,
                        cropHistory,
                        telemetry
                );

        /*
         * ---------------------------------------------------------
         * Calculate component scores.
         * ---------------------------------------------------------
         */
        double soilScore =
                calculateSoilScore(
                        soil,
                        nutrient
                );

        double waterScore =
                calculateWaterScore(
                        irrigation,
                        telemetry
                );

        double energyScore =
                calculateEnergyScore(
                        telemetry
                );

        double cropPracticeScore =
                calculateCropPracticeScore(
                        cropHistory,
                        satellite
                );

        double fieldPracticeScore =
                calculateFieldPracticeScore(
                        fertilizer,
                        nutrient,
                        diseasePestRisk
                );

        /*
         * ---------------------------------------------------------
         * Overall weighted score.
         * ---------------------------------------------------------
         */
        double overallScore =
                weightedScore(
                        soilScore,
                        waterScore,
                        energyScore,
                        cropPracticeScore,
                        fieldPracticeScore
                );

        String sustainabilityLevel =
                classifyLevel(
                        overallScore
                );

        /*
         * ---------------------------------------------------------
         * Explanations.
         * ---------------------------------------------------------
         */
        String soilExplanation =
                buildSoilExplanation(
                        soil,
                        nutrient,
                        soilScore
                );

        String waterExplanation =
                buildWaterExplanation(
                        irrigation,
                        telemetry,
                        waterScore
                );

        String energyExplanation =
                buildEnergyExplanation(
                        telemetry,
                        energyScore
                );

        String cropPracticeExplanation =
                buildCropPracticeExplanation(
                        cropHistory,
                        satellite,
                        cropPracticeScore
                );

        String fieldPracticeExplanation =
                buildFieldPracticeExplanation(
                        fertilizer,
                        nutrient,
                        diseasePestRisk,
                        fieldPracticeScore
                );

        String recommendation =
                buildRecommendation(
                        overallScore,
                        waterScore,
                        energyScore,
                        cropPracticeScore,
                        fieldPracticeScore
                );

        String dataUsed =
                buildDataUsed(
                        soil,
                        cropHistory,
                        telemetry,
                        fertilizer,
                        nutrient,
                        satellite,
                        diseasePestRisk,
                        irrigation
                );

        String uncertaintyNote =
                buildUncertaintyNote(
                        soil,
                        telemetry,
                        fertilizer,
                        satellite,
                        diseasePestRisk,
                        irrigation
                );

        String dataStatus =
                determineDataStatus(
                        soil,
                        telemetry,
                        fertilizer,
                        satellite,
                        diseasePestRisk,
                        irrigation
                );

        Double confidence =
                calculateConfidence(
                        soil,
                        cropHistory,
                        telemetry,
                        fertilizer,
                        nutrient,
                        satellite
                );

        /*
         * ---------------------------------------------------------
         * Build score.
         * ---------------------------------------------------------
         */
        FarmSustainabilityScore score =
                FarmSustainabilityScore.builder()
                        .farm(farm)

                        .crop(
                                cropHistory != null
                                        ? cropHistory.getCrop()
                                        : null
                        )

                        .overallScore(
                                round2(
                                        overallScore
                                )
                        )

                        .soilScore(
                                round2(
                                        soilScore
                                )
                        )

                        .waterScore(
                                round2(
                                        waterScore
                                )
                        )

                        .energyScore(
                                round2(
                                        energyScore
                                )
                        )

                        .cropPracticeScore(
                                round2(
                                        cropPracticeScore
                                )
                        )

                        .fieldPracticeScore(
                                round2(
                                        fieldPracticeScore
                                )
                        )

                        .sustainabilityLevel(
                                sustainabilityLevel
                        )

                        .soilExplanation(
                                soilExplanation
                        )

                        .waterExplanation(
                                waterExplanation
                        )

                        .energyExplanation(
                                energyExplanation
                        )

                        .cropPracticeExplanation(
                                cropPracticeExplanation
                        )

                        .fieldPracticeExplanation(
                                fieldPracticeExplanation
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
                                round2(
                                        confidence
                                )
                        )

                        .uncertaintyNote(
                                uncertaintyNote
                        )

                        .evaluatedAt(
                                LocalDateTime.now()
                        )

                        .validUntil(
                                LocalDateTime.now()
                                        .plusDays(7)
                        )

                        .build();

        return scoreRepository.save(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * Get latest score.
     * -------------------------------------------------------------
     */
    public FarmSustainabilityScore getLatest(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return scoreRepository
                .findFirstByFarmIdOrderByEvaluatedAtDesc(
                        farmId
                )
                .orElse(null);
    }

    /*
     * -------------------------------------------------------------
     * Get all scores.
     * -------------------------------------------------------------
     */
    public List<FarmSustainabilityScore> getByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return scoreRepository
                .findByFarmIdOrderByEvaluatedAtDesc(
                        farmId
                );
    }

    /*
     * -------------------------------------------------------------
     * Get score by ID.
     * -------------------------------------------------------------
     */
    public FarmSustainabilityScore getById(
            UUID id
    ) {

        return scoreRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sustainability score not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * -------------------------------------------------------------
     * Member 2 irrigation decision.
     * -------------------------------------------------------------
     */
    private SmartIrrigationResponse buildSmartIrrigationResponse(
            UUID farmId,
            CropHistory cropHistory,
            IrrigationTelemetry telemetry
    ) {

        SmartIrrigationRequest request =
                SmartIrrigationRequest.builder()
                        .farmId(
                                farmId
                        )

                        .cropId(
                                cropHistory != null
                                        && cropHistory.getCrop() != null
                                        ? cropHistory
                                        .getCrop()
                                        .getId()
                                        : null
                        )

                        .cropStage(
                                telemetry != null
                                        ? telemetry.getCropStage()
                                        : null
                        )

                        .build();

        return smartIrrigationService.evaluate(
                request
        );
    }

    /*
     * -------------------------------------------------------------
     * SOIL SCORE
     *
     * Score is based on:
     * - soil pH
     * - availability of nutrient information
     * - nutrient balance information
     * -------------------------------------------------------------
     */
    private double calculateSoilScore(
            SoilSample soil,
            NutrientBalance nutrient
    ) {

        if (soil == null) {

            return 50.0;
        }

        double score = 50.0;

        /*
         * pH scoring.
         *
         * Broad agricultural screening range.
         * Crop-specific calibration can be added later.
         */
        if (soil.getPH() != null) {

            double ph =
                    soil.getPH();

            if (ph >= 6.0
                    && ph <= 7.5) {

                score += 30.0;

            } else if (ph >= 5.5
                    && ph <= 8.0) {

                score += 20.0;

            } else {

                score += 5.0;
            }
        }

        /*
         * Soil nutrient test availability.
         */
        if (soil.getNitrogen() != null
                && soil.getPhosphorus() != null
                && soil.getPotassium() != null) {

            score += 15.0;

        } else {

            score += 5.0;
        }

        /*
         * Nutrient balance availability.
         */
        if (nutrient != null) {

            score += 5.0;
        }

        return clamp(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * WATER SCORE
     * -------------------------------------------------------------
     */
    private double calculateWaterScore(
            SmartIrrigationResponse irrigation,
            IrrigationTelemetry telemetry
    ) {

        double score = 50.0;

        if (irrigation != null) {

            String status =
                    irrigation.getFieldStatus();

            if ("SUFFICIENT".equalsIgnoreCase(
                    status
            )) {

                score += 35.0;

            } else if ("MONITOR".equalsIgnoreCase(
                    status
            )) {

                score += 20.0;

            } else if ("DEFICIENT".equalsIgnoreCase(
                    status
            )) {

                score += 5.0;
            }
        }

        /*
         * Active telemetry provides evidence that
         * water management can be evaluated.
         */
        if (telemetry != null) {

            score += 10.0;
        }

        /*
         * Solar-based pumping telemetry gives us
         * additional water-management information.
         */
        if (telemetry != null
                && telemetry.getSolarIrradianceWm2() != null) {

            score += 5.0;
        }

        return clamp(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * ENERGY SCORE
     * -------------------------------------------------------------
     */
    private double calculateEnergyScore(
            IrrigationTelemetry telemetry
    ) {

        if (telemetry == null) {

            return 50.0;
        }

        Double solar =
                telemetry.getSolarIrradianceWm2();

        Double solarPower =
                telemetry.getSolarPowerKw();

        if (solar == null
                && solarPower == null) {

            return 50.0;
        }

        double score = 50.0;

        /*
         * Strong solar availability supports
         * renewable energy potential.
         */
        if (solar != null) {

            if (solar >= 700) {

                score += 30.0;

            } else if (solar >= 500) {

                score += 25.0;

            } else if (solar >= 300) {

                score += 15.0;

            } else {

                score += 5.0;
            }
        }

        /*
         * Existing solar power output.
         */
        if (solarPower != null
                && solarPower > 0) {

            score += 20.0;
        }

        return clamp(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * CROP PRACTICE SCORE
     * -------------------------------------------------------------
     */
    private double calculateCropPracticeScore(
            CropHistory cropHistory,
            SatelliteObservation satellite
    ) {

        double score = 50.0;

        if (cropHistory != null) {

            score += 20.0;

            Crop crop =
                    cropHistory.getCrop();

            if (crop != null) {

                if (crop.getCommonRotation() != null
                        && !crop.getCommonRotation().isBlank()) {

                    score += 15.0;
                }

                if (crop.getWaterNeed() != null
                        && !crop.getWaterNeed().isBlank()) {

                    score += 5.0;
                }
            }
        }

        /*
         * Satellite information gives an additional
         * crop-condition signal.
         */
        if (satellite != null) {

            if ("HEALTHY".equalsIgnoreCase(
                    satellite.getCropCondition()
            )) {

                score += 10.0;

            } else if ("NORMAL".equalsIgnoreCase(
                    satellite.getCropCondition()
            )) {

                score += 7.0;

            } else if ("STRESSED".equalsIgnoreCase(
                    satellite.getCropCondition()
            )) {

                score += 2.0;
            }
        }

        return clamp(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * FIELD PRACTICE SCORE
     * -------------------------------------------------------------
     */
    private double calculateFieldPracticeScore(
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            DiseasePestRisk diseasePestRisk
    ) {

        double score = 50.0;

        /*
         * Soil-based fertilizer recommendation exists.
         */
        if (fertilizer != null) {

            score += 20.0;
        }

        /*
         * Nutrient monitoring exists.
         */
        if (nutrient != null) {

            score += 15.0;
        }

        /*
         * A lower disease/pest risk is treated as
         * favorable field-condition evidence.
         *
         * This does NOT mean the field is disease-free.
         */
        if (diseasePestRisk != null) {

            if ("LOW".equalsIgnoreCase(
                    diseasePestRisk.getRiskLevel()
            )) {

                score += 15.0;

            } else if ("MEDIUM".equalsIgnoreCase(
                    diseasePestRisk.getRiskLevel()
            )) {

                score += 8.0;

            } else {

                score += 2.0;
            }
        }

        return clamp(
                score
        );
    }

    /*
     * -------------------------------------------------------------
     * WEIGHTED OVERALL SCORE
     * -------------------------------------------------------------
     */
    private double weightedScore(
            double soil,
            double water,
            double energy,
            double crop,
            double field
    ) {

        return
                (
                        soil * SOIL_WEIGHT
                                + water * WATER_WEIGHT
                                + energy * ENERGY_WEIGHT
                                + crop * CROP_WEIGHT
                                + field * FIELD_WEIGHT
                );
    }

    /*
     * -------------------------------------------------------------
     * Sustainability level.
     * -------------------------------------------------------------
     */
    private String classifyLevel(
            double score
    ) {

        if (score >= 85.0) {

            return "EXCELLENT";
        }

        if (score >= 70.0) {

            return "GOOD";
        }

        if (score >= 55.0) {

            return "MODERATE";
        }

        if (score >= 40.0) {

            return "NEEDS_IMPROVEMENT";
        }

        return "CRITICAL";
    }

    /*
     * -------------------------------------------------------------
     * Soil explanation.
     * -------------------------------------------------------------
     */
    private String buildSoilExplanation(
            SoilSample soil,
            NutrientBalance nutrient,
            double score
    ) {

        if (soil == null) {

            return
                    "No recent soil sample is available, so the soil component uses a neutral fallback score.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Soil score="
        );

        result.append(
                round2(score)
        );

        result.append(
                ". "
        );

        if (soil.getPH() != null) {

            result.append(
                    "Measured pH="
            );

            result.append(
                    soil.getPH()
            );

            result.append(
                    ". "
            );
        }

        if (soil.getNitrogen() != null
                && soil.getPhosphorus() != null
                && soil.getPotassium() != null) {

            result.append(
                    "N, P and K measurements are available. "
            );
        }

        if (nutrient != null) {

            result.append(
                    "A nutrient-balance record is also available."
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Water explanation.
     * -------------------------------------------------------------
     */
    private String buildWaterExplanation(
            SmartIrrigationResponse irrigation,
            IrrigationTelemetry telemetry,
            double score
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Water score="
        );

        result.append(
                round2(score)
        );

        result.append(
                ". "
        );

        if (irrigation != null) {

            result.append(
                    "Smart irrigation status="
            );

            result.append(
                    safe(
                            irrigation.getFieldStatus()
                    )
            );

            result.append(
                    ". "
            );
        }

        if (telemetry != null
                && telemetry.getVwc30cm() != null) {

            result.append(
                    "Current VWC at 30 cm="
            );

            result.append(
                    telemetry.getVwc30cm()
            );

            result.append(
                    "%."
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Energy explanation.
     * -------------------------------------------------------------
     */
    private String buildEnergyExplanation(
            IrrigationTelemetry telemetry,
            double score
    ) {

        if (telemetry == null) {

            return
                    "Energy score uses a neutral fallback because no recent irrigation telemetry is available.";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Energy score="
        );

        result.append(
                round2(score)
        );

        result.append(
                ". "
        );

        if (telemetry.getSolarIrradianceWm2() != null) {

            result.append(
                    "Solar irradiance="
            );

            result.append(
                    telemetry.getSolarIrradianceWm2()
            );

            result.append(
                    " W/m². "
            );
        }

        if (telemetry.getSolarPowerKw() != null) {

            result.append(
                    "Solar power="
            );

            result.append(
                    telemetry.getSolarPowerKw()
            );

            result.append(
                    " kW."
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Crop-practice explanation.
     * -------------------------------------------------------------
     */
    private String buildCropPracticeExplanation(
            CropHistory cropHistory,
            SatelliteObservation satellite,
            double score
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Crop-practice score="
        );

        result.append(
                round2(score)
        );

        result.append(
                ". "
        );

        if (cropHistory != null
                && cropHistory.getCrop() != null) {

            result.append(
                    "Current recorded crop="
            );

            result.append(
                    cropHistory.getCrop().getName()
            );

            result.append(
                    ". "
            );

            if (cropHistory.getCrop().getCommonRotation() != null) {

                result.append(
                        "Configured crop rotation information is available."
                );
            }
        } else {

            result.append(
                    "No recent crop-history record is available."
            );
        }

        if (satellite != null) {

            result.append(
                    " Latest satellite crop condition="
            );

            result.append(
                    safe(
                            satellite.getCropCondition()
                    )
            );

            result.append(
                    "."
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Field-practice explanation.
     * -------------------------------------------------------------
     */
    private String buildFieldPracticeExplanation(
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            DiseasePestRisk diseasePestRisk,
            double score
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Field-practice score="
        );

        result.append(
                round2(score)
        );

        result.append(
                ". "
        );

        if (fertilizer != null) {

            result.append(
                    "A fertilizer recommendation record is available. "
            );
        }

        if (nutrient != null) {

            result.append(
                    "Nutrient monitoring is available. "
            );
        }

        if (diseasePestRisk != null) {

            result.append(
                    "Latest disease/pest environmental risk="
            );

            result.append(
                    safe(
                            diseasePestRisk.getRiskLevel()
                    )
            );

            result.append(
                    "."
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Recommendation.
     * -------------------------------------------------------------
     */
    private String buildRecommendation(
            double overall,
            double water,
            double energy,
            double crop,
            double field
    ) {

        if (water < 55.0) {

            return
                    "Prioritize efficient water management and follow the validated irrigation decision before pumping.";
        }

        if (energy < 55.0) {

            return
                    "Review solar-pumping efficiency and avoid pumping only because solar energy is available.";
        }

        if (crop < 55.0) {

            return
                    "Review crop planning, rotation information and crop-condition signals.";
        }

        if (field < 55.0) {

            return
                    "Improve field monitoring and validate fertilizer, nutrient and crop-health decisions.";
        }

        if (overall >= 85.0) {

            return
                    "Maintain the current sustainable practices and continue monitoring the farm for meaningful changes.";
        }

        if (overall >= 70.0) {

            return
                    "Farm sustainability is currently good. Continue monitoring soil, water, energy and crop conditions.";
        }

        if (overall >= 55.0) {

            return
                    "Farm sustainability is moderate. Focus on the lowest-scoring component first.";
        }

        return
                "Several sustainability components need improvement. Review the farm conditions and obtain local agricultural guidance.";
    }

    /*
     * -------------------------------------------------------------
     * Data used.
     * -------------------------------------------------------------
     */
    private String buildDataUsed(
            SoilSample soil,
            CropHistory cropHistory,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            SatelliteObservation satellite,
            DiseasePestRisk diseasePestRisk,
            SmartIrrigationResponse irrigation
    ) {

        List<String> sources =
                new ArrayList<>();

        if (soil != null) {

            sources.add(
                    "SoilSample"
            );
        }

        if (cropHistory != null) {

            sources.add(
                    "CropHistory"
            );
        }

        if (telemetry != null) {

            sources.add(
                    "IrrigationTelemetry"
            );
        }

        if (fertilizer != null) {

            sources.add(
                    "FertilizerRecommendation"
            );
        }

        if (nutrient != null) {

            sources.add(
                    "NutrientBalance"
            );
        }

        if (satellite != null) {

            sources.add(
                    "SatelliteObservation"
            );
        }

        if (diseasePestRisk != null) {

            sources.add(
                    "DiseasePestRisk"
            );
        }

        if (irrigation != null) {

            sources.add(
                    "SmartIrrigationDecision"
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
     * Uncertainty.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            SoilSample soil,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            SatelliteObservation satellite,
            DiseasePestRisk diseasePestRisk,
            SmartIrrigationResponse irrigation
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "This sustainability score is an AgriNexus decision-support score, not a certified sustainability standard."
        );

        result.append(
                " Component weights and thresholds are methodology version "
        );

        result.append(
                METHODOLOGY_VERSION
        );

        result.append(
                "."
        );

        if (soil == null) {

            result.append(
                    " Soil data is missing."
            );
        }

        if (telemetry == null) {

            result.append(
                    " Irrigation telemetry is missing."
            );
        }

        if (irrigation != null
                && "TEST".equalsIgnoreCase(
                irrigation.getDataStatus()
        )) {

            result.append(
                    " Smart irrigation data is synthetic test data."
            );
        }

        if (fertilizer != null
                && "TEST".equalsIgnoreCase(
                fertilizer.getDataStatus()
        )) {

            result.append(
                    " Fertilizer data is synthetic test data."
            );
        }

        if (satellite != null
                && "TEST".equalsIgnoreCase(
                satellite.getDataStatus()
        )) {

            result.append(
                    " Satellite observation is synthetic test data."
            );
        }

        if (diseasePestRisk != null) {

            result.append(
                    " Disease/pest information is an environmental screening signal and not a confirmed diagnosis."
            );
        }

        result.append(
                " Important farm decisions should be validated against current field observations and appropriate local agricultural guidance."
        );

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Overall data status.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            SoilSample soil,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            SatelliteObservation satellite,
            DiseasePestRisk diseasePestRisk,
            SmartIrrigationResponse irrigation
    ) {

        boolean test =
                false;

        if (telemetry != null
                && "TEST".equalsIgnoreCase(
                telemetry.getSensorStatus()
        )) {

            test = true;
        }

        if (telemetry != null
                && "TEST_SENSOR".equalsIgnoreCase(
                telemetry.getSource()
        )) {

            test = true;
        }

        if (fertilizer != null
                && "TEST".equalsIgnoreCase(
                fertilizer.getDataStatus()
        )) {

            test = true;
        }

        if (satellite != null
                && "TEST".equalsIgnoreCase(
                satellite.getDataStatus()
        )) {

            test = true;
        }

        if (diseasePestRisk != null
                && "TEST".equalsIgnoreCase(
                diseasePestRisk.getDataStatus()
        )) {

            test = true;
        }

        if (irrigation != null
                && "TEST".equalsIgnoreCase(
                irrigation.getDataStatus()
        )) {

            test = true;
        }

        if (test) {

            return "TEST";
        }

        if (soil != null
                || telemetry != null
                || fertilizer != null
                || satellite != null
                || diseasePestRisk != null
                || irrigation != null) {

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
            SoilSample soil,
            CropHistory cropHistory,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            SatelliteObservation satellite
    ) {

        int available =
                0;

        int total =
                6;

        if (soil != null) {

            available++;
        }

        if (cropHistory != null) {

            available++;
        }

        if (telemetry != null) {

            available++;
        }

        if (fertilizer != null) {

            available++;
        }

        if (nutrient != null) {

            available++;
        }

        if (satellite != null) {

            available++;
        }

        /*
         * Keep confidence bounded.
         */
        double confidence =
                0.50
                        + (
                        (double) available
                                / total
                )
                        * 0.45;

        return Math.min(
                0.95,
                confidence
        );
    }

    /*
     * -------------------------------------------------------------
     * Clamp.
     * -------------------------------------------------------------
     */
    private double clamp(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        100.0,
                        value
                )
        );
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
     * Safe.
     * -------------------------------------------------------------
     */
    private String safe(
            String value
    ) {

        return value == null
                ? "UNKNOWN"
                : value;
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
}