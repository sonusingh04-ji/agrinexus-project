package com.agrinexus.service;

import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.CropRotationDecision;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.NutrientBalance;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.CropRotationDecisionRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.NutrientBalanceRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CropRotationService {

    /*
     * -------------------------------------------------------------
     * Methodology version.
     * -------------------------------------------------------------
     */
    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-ROTATION-V2";

    /*
     * -------------------------------------------------------------
     * Rotation scoring weights.
     *
     * Total = 100%
     * -------------------------------------------------------------
     */
    private static final double SEASON_WEIGHT = 0.20;

    private static final double ROTATION_WEIGHT = 0.25;

    private static final double SOIL_WEIGHT = 0.20;

    private static final double WATER_WEIGHT = 0.15;

    private static final double NUTRIENT_WEIGHT = 0.10;

    private static final double FARMER_CONSTRAINT_WEIGHT = 0.10;

    private final FarmRepository farmRepository;

    private final CropRepository cropRepository;

    private final CropHistoryRepository cropHistoryRepository;

    private final SoilSampleRepository soilSampleRepository;

    private final IrrigationTelemetryRepository irrigationTelemetryRepository;

    private final NutrientBalanceRepository nutrientBalanceRepository;

    private final CropRotationDecisionRepository decisionRepository;

    /*
     * -------------------------------------------------------------
     * Main crop-rotation evaluation.
     *
     * Inputs:
     * farmId
     * previousCropId - optional
     * targetSeason   - optional
     * farmerConstraint - optional
     * -------------------------------------------------------------
     */
    @Transactional
    public CropRotationDecision evaluate(
            UUID farmId,
            UUID previousCropId,
            String targetSeason,
            String farmerConstraint
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
         * Determine previous crop.
         *
         * Priority:
         * 1. Explicit previousCropId
         * 2. Latest crop history
         * ---------------------------------------------------------
         */
        Crop previousCrop = null;

        if (previousCropId != null) {

            previousCrop =
                    cropRepository.findById(
                            previousCropId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Previous crop not found with ID: "
                                            + previousCropId
                            )
                    );
        }

        CropHistory latestHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );

        if (previousCrop == null
                && latestHistory != null) {

            previousCrop =
                    latestHistory.getCrop();
        }

        /*
         * ---------------------------------------------------------
         * Determine target season.
         * ---------------------------------------------------------
         */
        String normalizedSeason =
                normalizeSeason(
                        targetSeason
                );

        /*
         * ---------------------------------------------------------
         * Latest available field data.
         * ---------------------------------------------------------
         */
        SoilSample soil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );

        IrrigationTelemetry telemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        NutrientBalance nutrient =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                farmId
                        );

        /*
         * ---------------------------------------------------------
         * Build candidate list.
         * ---------------------------------------------------------
         */
        List<Crop> allCrops =
                cropRepository.findAll();

        List<CropCandidate> candidates =
                new ArrayList<>();

        for (Crop candidate : allCrops) {

            if (candidate == null) {
                continue;
            }

            /*
             * Do not recommend the exact previous crop as
             * the first rotation choice.
             */
            if (previousCrop != null
                    && previousCrop.getId() != null
                    && candidate.getId() != null
                    && previousCrop.getId().equals(
                    candidate.getId()
            )) {

                continue;
            }

            CropCandidate evaluated =
                    evaluateCandidate(
                            candidate,
                            previousCrop,
                            normalizedSeason,
                            farmerConstraint,
                            soil,
                            telemetry,
                            nutrient
                    );

            candidates.add(
                    evaluated
            );
        }

        /*
         * ---------------------------------------------------------
         * Highest score first.
         *
         * If two candidates have the same score, sort by name
         * so the output stays deterministic.
         * ---------------------------------------------------------
         */
        candidates.sort(
                Comparator
                        .comparingDouble(
                                CropCandidate::score
                        )
                        .reversed()
                        .thenComparing(
                                candidate ->
                                        safe(
                                                candidate.crop()
                                                        .getName()
                                        )
                        )
        );

        /*
         * ---------------------------------------------------------
         * Top recommendation.
         * ---------------------------------------------------------
         */
        CropCandidate topCandidate =
                candidates
                        .stream()
                        .findFirst()
                        .orElse(null);

        /*
         * ---------------------------------------------------------
         * No candidates.
         * ---------------------------------------------------------
         */
        if (topCandidate == null) {

            return decisionRepository.save(
                    CropRotationDecision.builder()
                            .farm(farm)
                            .previousCrop(previousCrop)
                            .recommendedCrop(null)
                            .targetSeason(normalizedSeason)
                            .recommendationScore(0.0)
                            .reason(
                                    "No suitable candidate crop could be evaluated from the currently available crop records."
                            )
                            .contributingFactors(
                                    "Candidate crop information is missing or insufficient."
                            )
                            .rankedCandidates("")
                            .soilPh(
                                    soil != null
                                            ? soil.getPH()
                                            : null
                            )
                            .soilNitrogen(
                                    soil != null
                                            ? soil.getNitrogen()
                                            : null
                            )
                            .soilPhosphorus(
                                    soil != null
                                            ? soil.getPhosphorus()
                                            : null
                            )
                            .soilPotassium(
                                    soil != null
                                            ? soil.getPotassium()
                                            : null
                            )
                            .currentMoisture(
                                    telemetry != null
                                            ? telemetry.getVwc30cm()
                                            : null
                            )
                            .recentRainfall(null)
                            .farmerConstraint(
                                    normalizeConstraint(
                                            farmerConstraint
                                    )
                            )
                            .dataStatus(
                                    determineDataStatus(
                                            soil,
                                            telemetry,
                                            nutrient
                                    )
                            )
                            .methodologyVersion(
                                    METHODOLOGY_VERSION
                            )
                            .confidence(0.50)
                            .uncertaintyNote(
                                    buildUncertaintyNote(
                                            soil,
                                            telemetry,
                                            nutrient
                                    )
                            )
                            .evaluatedAt(
                                    LocalDateTime.now()
                            )
                            .validUntil(
                                    LocalDateTime.now()
                                            .plusDays(14)
                            )
                            .build()
            );
        }

        /*
         * ---------------------------------------------------------
         * Build ranked candidate text.
         * ---------------------------------------------------------
         */
        String rankedCandidates =
                buildRankedCandidates(
                        candidates
                );

        /*
         * ---------------------------------------------------------
         * Recommendation explanation.
         * ---------------------------------------------------------
         */
        String reason =
                buildRecommendationReason(
                        topCandidate,
                        previousCrop,
                        normalizedSeason,
                        farmerConstraint
                );

        String contributingFactors =
                buildContributingFactors(
                        topCandidate
                );

        /*
         * ---------------------------------------------------------
         * Confidence.
         * ---------------------------------------------------------
         */
        double confidence =
                calculateConfidence(
                        soil,
                        telemetry,
                        nutrient,
                        candidates.size()
                );

        /*
         * ---------------------------------------------------------
         * Data provenance.
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        soil,
                        telemetry,
                        nutrient
                );

        /*
         * ---------------------------------------------------------
         * Build and persist decision.
         * ---------------------------------------------------------
         */
        CropRotationDecision decision =
                CropRotationDecision.builder()

                        .farm(farm)

                        .previousCrop(previousCrop)

                        .recommendedCrop(
                                topCandidate.crop()
                        )

                        .targetSeason(
                                normalizedSeason
                        )

                        .recommendationScore(
                                round2(
                                        topCandidate.score()
                                )
                        )

                        .reason(
                                reason
                        )

                        .contributingFactors(
                                contributingFactors
                        )

                        .rankedCandidates(
                                rankedCandidates
                        )

                        .soilPh(
                                soil != null
                                        ? soil.getPH()
                                        : null
                        )

                        .soilNitrogen(
                                soil != null
                                        ? soil.getNitrogen()
                                        : null
                        )

                        .soilPhosphorus(
                                soil != null
                                        ? soil.getPhosphorus()
                                        : null
                        )

                        .soilPotassium(
                                soil != null
                                        ? soil.getPotassium()
                                        : null
                        )

                        .currentMoisture(
                                telemetry != null
                                        ? telemetry.getVwc30cm()
                                        : null
                        )

                        /*
                         * No reliable rainfall field has been
                         * verified in the current CropRotation
                         * input model, so do not fabricate it.
                         */
                        .recentRainfall(null)

                        .farmerConstraint(
                                normalizeConstraint(
                                        farmerConstraint
                                )
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
                                buildUncertaintyNote(
                                        soil,
                                        telemetry,
                                        nutrient
                                )
                        )

                        .evaluatedAt(
                                LocalDateTime.now()
                        )

                        .validUntil(
                                LocalDateTime.now()
                                        .plusDays(14)
                        )

                        .build();

        return decisionRepository.save(
                decision
        );
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 1
     *
     * Evaluate one candidate using six independent factors.
     * -------------------------------------------------------------
     */
    private CropCandidate evaluateCandidate(
            Crop candidate,
            Crop previousCrop,
            String targetSeason,
            String farmerConstraint,
            SoilSample soil,
            IrrigationTelemetry telemetry,
            NutrientBalance nutrient
    ) {

        List<String> reasons =
                new ArrayList<>();

        /*
         * Season compatibility.
         */
        double seasonScore =
                seasonScore(
                        candidate,
                        targetSeason,
                        reasons
                );

        /*
         * Previous-crop rotation.
         */
        double rotationScore =
                rotationScore(
                        candidate,
                        previousCrop,
                        reasons
                );

        /*
         * Soil compatibility.
         */
        double soilScore =
                soilScore(
                        candidate,
                        soil,
                        reasons
                );

        /*
         * Water suitability.
         */
        double waterScore =
                waterScore(
                        candidate,
                        telemetry,
                        farmerConstraint,
                        reasons
                );

        /*
         * Nutrient diversification.
         */
        double nutrientScore =
                nutrientScore(
                        candidate,
                        nutrient,
                        reasons
                );

        /*
         * Farmer constraint.
         */
        double farmerScore =
                farmerScore(
                        candidate,
                        farmerConstraint,
                        reasons
                );

        /*
         * ---------------------------------------------------------
         * Weighted calculation.
         *
         * IMPORTANT:
         * Each component is already 0-100.
         * ---------------------------------------------------------
         */
        double finalScore =
                (
                        seasonScore
                                * SEASON_WEIGHT
                                + rotationScore
                                * ROTATION_WEIGHT
                                + soilScore
                                * SOIL_WEIGHT
                                + waterScore
                                * WATER_WEIGHT
                                + nutrientScore
                                * NUTRIENT_WEIGHT
                                + farmerScore
                                * FARMER_CONSTRAINT_WEIGHT
                );

        return new CropCandidate(
                candidate,
                clamp(
                        finalScore
                ),
                reasons
        );
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 2
     *
     * Season score.
     * -------------------------------------------------------------
     */
    private double seasonScore(
            Crop candidate,
            String targetSeason,
            List<String> reasons
    ) {

        if (candidate.getSeason() == null
                || candidate.getSeason().isBlank()
                || targetSeason == null
                || targetSeason.isBlank()) {

            reasons.add(
                    "Season information is unavailable"
            );

            return 50.0;
        }

        boolean compatible =
                isSeasonCompatible(
                        candidate.getSeason(),
                        targetSeason
                );

        if (compatible) {

            reasons.add(
                    "Season is compatible"
            );

            return 100.0;
        }

        reasons.add(
                "Season is not aligned"
        );

        return 20.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 3
     *
     * Previous-crop rotation score.
     * -------------------------------------------------------------
     */
    private double rotationScore(
            Crop candidate,
            Crop previousCrop,
            List<String> reasons
    ) {

        if (previousCrop == null) {

            reasons.add(
                    "Previous crop is unavailable"
            );

            return 50.0;
        }

        if (candidate.getCommonRotation() == null
                || candidate.getCommonRotation().isBlank()) {

            reasons.add(
                    "Configured rotation information is unavailable"
            );

            return 50.0;
        }

        String rotationText =
                candidate.getCommonRotation()
                        .toLowerCase(
                                Locale.ROOT
                        );

        String previousName =
                safe(
                        previousCrop.getName()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        String candidateName =
                safe(
                        candidate.getName()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * Check both:
         *
         * Rice-Wheat
         *
         * Wheat-Rice
         */
        String forward =
                previousName
                        + "-"
                        + candidateName;

        String reverse =
                candidateName
                        + "-"
                        + previousName;

        /*
         * Also support rotation text where crops are
         * separated by spaces or other punctuation.
         */
        boolean explicitMatch =
                rotationText.contains(
                        forward
                )
                        || rotationText.contains(
                        reverse
                );

        if (explicitMatch) {

            reasons.add(
                    "Configured crop rotation matches the previous crop"
            );

            return 100.0;
        }

        /*
         * No match is not automatically a rejection.
         */
        reasons.add(
                "No explicit previous-crop rotation match was found"
        );

        return 50.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 4
     *
     * Soil pH compatibility.
     * -------------------------------------------------------------
     */
    private double soilScore(
            Crop candidate,
            SoilSample soil,
            List<String> reasons
    ) {

        if (soil == null
                || soil.getPH() == null) {

            reasons.add(
                    "Soil pH information is unavailable"
            );

            return 50.0;
        }

        if (candidate.getSoilPhMin() == null
                || candidate.getSoilPhMax() == null) {

            reasons.add(
                    "Crop-specific soil pH range is unavailable"
            );

            return 50.0;
        }

        double ph =
                soil.getPH();

        double min =
                candidate.getSoilPhMin();

        double max =
                candidate.getSoilPhMax();

        /*
         * Perfect range match.
         */
        if (ph >= min
                && ph <= max) {

            reasons.add(
                    "Soil pH is within crop range"
            );

            return 100.0;
        }

        /*
         * Distance from suitable range.
         */
        double distance;

        if (ph < min) {

            distance =
                    min - ph;

        } else {

            distance =
                    ph - max;
        }

        /*
         * Close to the range.
         */
        if (distance <= 0.5) {

            reasons.add(
                    "Soil pH is slightly outside the crop range"
            );

            return 65.0;
        }

        /*
         * Moderately outside.
         */
        if (distance <= 1.0) {

            reasons.add(
                    "Soil pH is moderately outside the crop range"
            );

            return 40.0;
        }

        /*
         * Far outside.
         */
        reasons.add(
                "Soil pH is substantially outside the crop range"
        );

        return 15.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 5
     *
     * Water suitability.
     * -------------------------------------------------------------
     */
    private double waterScore(
            Crop candidate,
            IrrigationTelemetry telemetry,
            String farmerConstraint,
            List<String> reasons
    ) {

        String waterNeed =
                safe(
                        candidate.getWaterNeed()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        boolean lowWater =
                waterNeed.contains(
                        "low"
                );

        boolean highWater =
                waterNeed.contains(
                        "high"
                )
                        || waterNeed.contains(
                        "very high"
                );

        boolean moderateWater =
                waterNeed.contains(
                        "moderate"
                );

        /*
         * ---------------------------------------------------------
         * Explicit low-water preference.
         * ---------------------------------------------------------
         */
        if (farmerConstraintMatchesLowWater(
                farmerConstraint
        )) {

            if (lowWater) {

                reasons.add(
                        "Low-water requirement matches the farmer constraint"
                );

                return 100.0;
            }

            if (highWater) {

                reasons.add(
                        "High-water requirement conflicts with the farmer constraint"
                );

                return 20.0;
            }

            if (moderateWater) {

                reasons.add(
                        "Moderate-water requirement is acceptable but not ideal for the farmer constraint"
                );

                return 60.0;
            }
        }

        /*
         * ---------------------------------------------------------
         * Current root-zone moisture.
         *
         * These are broad screening values, not a crop-specific
         * soil-water calibration.
         * ---------------------------------------------------------
         */
        if (telemetry != null
                && telemetry.getVwc30cm() != null) {

            double moisture =
                    telemetry.getVwc30cm();

            if (moisture >= 20.0
                    && moisture <= 40.0) {

                if (lowWater) {

                    reasons.add(
                            "Low-water crop and current root-zone moisture are favorable"
                    );

                    return 90.0;
                }

                if (highWater) {

                    reasons.add(
                            "Current moisture is available, but the crop has a high water requirement"
                    );

                    return 65.0;
                }

                reasons.add(
                        "Current root-zone moisture is in the screening range"
                );

                return 80.0;
            }

            if (moisture < 20.0) {

                reasons.add(
                        "Current root-zone moisture is low"
                );

                if (highWater) {

                    return 30.0;
                }

                return 55.0;
            }

            reasons.add(
                    "Current root-zone moisture is high"
            );

            if (highWater) {

                return 60.0;
            }

            return 75.0;
        }

        /*
         * ---------------------------------------------------------
         * No current moisture information.
         * ---------------------------------------------------------
         */
        if (lowWater) {

            reasons.add(
                    "Crop has a low-water requirement"
            );

            return 80.0;
        }

        if (highWater) {

            reasons.add(
                    "Crop has a high water requirement"
            );

            return 40.0;
        }

        reasons.add(
                "Crop water profile is available but current moisture is not"
        );

        return 60.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 6
     *
     * Nutrient diversification.
     * -------------------------------------------------------------
     */
    private double nutrientScore(
            Crop candidate,
            NutrientBalance nutrient,
            List<String> reasons
    ) {

        if (nutrient == null) {

            reasons.add(
                    "Nutrient balance information is unavailable"
            );

            return 50.0;
        }

        String category =
                safe(
                        candidate.getCategory()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * Pulse / legume is treated as a diversification signal.
         *
         * It is NOT being interpreted as a guarantee of nitrogen
         * recovery or a guaranteed soil-health improvement.
         */
        if (category.contains("pulse")
                || category.contains("legume")) {

            reasons.add(
                    "Pulse/legume category provides a crop-diversification signal"
            );

            return 90.0;
        }

        reasons.add(
                "Nutrient-balance information is available"
        );

        return 70.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 7
     *
     * Farmer constraint score.
     * -------------------------------------------------------------
     */
    private double farmerScore(
            Crop candidate,
            String farmerConstraint,
            List<String> reasons
    ) {

        if (farmerConstraint == null
                || farmerConstraint.isBlank()
                || "NONE".equalsIgnoreCase(
                farmerConstraint.trim()
        )) {

            reasons.add(
                    "No additional farmer constraint was supplied"
            );

            return 70.0;
        }

        String constraint =
                farmerConstraint
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        String waterNeed =
                safe(
                        candidate.getWaterNeed()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        String category =
                safe(
                        candidate.getCategory()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * ---------------------------------------------------------
         * LOW WATER
         * ---------------------------------------------------------
         */
        if ("LOW_WATER".equals(
                constraint
        )) {

            if (waterNeed.contains(
                    "low"
            )) {

                reasons.add(
                        "Low-water farmer preference matched"
                );

                return 100.0;
            }

            if (waterNeed.contains(
                    "very high"
            )
                    || waterNeed.contains(
                    "high"
            )) {

                reasons.add(
                        "High-water crop conflicts with farmer preference"
                );

                return 20.0;
            }

            reasons.add(
                    "Crop has an intermediate water profile"
            );

            return 60.0;
        }

        /*
         * ---------------------------------------------------------
         * LOW INPUT COST
         *
         * This is only a category-level preference signal.
         * It does NOT calculate actual cost.
         * ---------------------------------------------------------
         */
        if ("LOW_INPUT_COST".equals(
                constraint
        )) {

            if (category.contains(
                    "pulse"
            )
                    || category.contains(
                    "legume"
            )
                    || category.contains(
                    "millet"
            )
                    || category.contains(
                    "cereal"
            )) {

                reasons.add(
                        "Crop category considered under low-input preference"
                );

                return 85.0;
            }

            reasons.add(
                    "No strong low-input category signal found"
            );

            return 60.0;
        }

        /*
         * ---------------------------------------------------------
         * SHORT DURATION
         *
         * The current Crop model does not have a numeric duration
         * field, so this cannot be a scientifically exact duration
         * score yet.
         * ---------------------------------------------------------
         */
        if ("SHORT_DURATION".equals(
                constraint
        )) {

            if (candidate.getSowingWindow() != null
                    && !candidate.getSowingWindow().isBlank()) {

                reasons.add(
                        "Crop has a defined sowing-window reference"
                );

                return 75.0;
            }

            reasons.add(
                    "Crop duration information is limited"
            );

            return 50.0;
        }

        /*
         * ---------------------------------------------------------
         * Unknown constraint.
         * ---------------------------------------------------------
         */
        reasons.add(
                "Farmer constraint is not recognized by the current methodology"
        );

        return 50.0;
    }

    /*
     * -------------------------------------------------------------
     * UPDATED PART 8
     *
     * Low-water helper retained separately so the main scoring
     * methods remain readable.
     * -------------------------------------------------------------
     */
    private boolean farmerConstraintMatchesLowWater(
            String farmerConstraint
    ) {

        return farmerConstraint != null
                && "LOW_WATER".equalsIgnoreCase(
                farmerConstraint.trim()
        );
    }

    /*
     * -------------------------------------------------------------
     * Season normalization.
     * -------------------------------------------------------------
     */
    private String normalizeSeason(
            String season
    ) {

        if (season != null
                && !season.isBlank()) {

            String value =
                    season.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );

            if (value.contains(
                    "RABI"
            )) {

                return "RABI";
            }

            if (value.contains(
                    "KHARIF"
            )) {

                return "KHARIF";
            }

            if (value.contains(
                    "SUMMER"
            )
                    || value.contains(
                    "ZAID"
            )) {

                return "SUMMER";
            }

            return value;
        }

        int month =
                LocalDate.now()
                        .getMonthValue();

        if (month >= 10
                || month <= 3) {

            return "RABI";
        }

        if (month >= 6
                && month <= 9) {

            return "KHARIF";
        }

        return "SUMMER";
    }

    /*
     * -------------------------------------------------------------
     * Season compatibility.
     * -------------------------------------------------------------
     */
    private boolean isSeasonCompatible(
            String cropSeason,
            String targetSeason
    ) {

        if (cropSeason == null
                || cropSeason.isBlank()
                || targetSeason == null
                || targetSeason.isBlank()) {

            return true;
        }

        String crop =
                cropSeason.toUpperCase(
                        Locale.ROOT
                );

        String target =
                targetSeason.toUpperCase(
                        Locale.ROOT
                );

        if ("RABI".equals(
                target
        )) {

            return crop.contains(
                    "RABI"
            )
                    || crop.contains(
                    "LONG-DURATION"
            );
        }

        if ("KHARIF".equals(
                target
        )) {

            return crop.contains(
                    "KHARIF"
            )
                    || crop.contains(
                    "LONG-DURATION"
            );
        }

        if ("SUMMER".equals(
                target
        )) {

            return crop.contains(
                    "SUMMER"
            )
                    || crop.contains(
                    "SPRING"
            )
                    || crop.contains(
                    "ZAID"
            )
                    || crop.contains(
                    "LONG-DURATION"
            );
        }

        return true;
    }

    /*
     * -------------------------------------------------------------
     * Ranked-candidate snapshot.
     * -------------------------------------------------------------
     */
    private String buildRankedCandidates(
            List<CropCandidate> candidates
    ) {

        StringBuilder result =
                new StringBuilder();

        int limit =
                Math.min(
                        10,
                        candidates.size()
                );

        for (int i = 0; i < limit; i++) {

            CropCandidate candidate =
                    candidates.get(i);

            if (i > 0) {

                result.append(
                        "; "
                );
            }

            result.append(
                    i + 1
            );

            result.append(
                    "."
            );

            result.append(
                    safe(
                            candidate.crop()
                                    .getName()
                    )
            );

            result.append(
                    "="
            );

            result.append(
                    round2(
                            candidate.score()
                    )
            );

            result.append(
                    "/100"
            );
        }

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Main recommendation reason.
     * -------------------------------------------------------------
     */
    private String buildRecommendationReason(
            CropCandidate topCandidate,
            Crop previousCrop,
            String targetSeason,
            String farmerConstraint
    ) {

        String cropName =
                safe(
                        topCandidate.crop()
                                .getName()
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                cropName
        );

        result.append(
                " received the highest rotation suitability score of "
        );

        result.append(
                round2(
                        topCandidate.score()
                )
        );

        result.append(
                "/100"
        );

        if (previousCrop != null) {

            result.append(
                    " after "
            );

            result.append(
                    safe(
                            previousCrop.getName()
                    )
            );

            result.append(
                    "."
            );

        } else {

            result.append(
                    "."
            );
        }

        result.append(
                " The ranking considers the target season, previous-crop rotation compatibility, soil compatibility, water suitability, nutrient diversification and available farmer constraints."
        );

        if (farmerConstraint != null
                && !farmerConstraint.isBlank()) {

            result.append(
                    " Farmer preference: "
            );

            result.append(
                    normalizeConstraint(
                            farmerConstraint
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
     * Top-candidate factors.
     * -------------------------------------------------------------
     */
    private String buildContributingFactors(
            CropCandidate candidate
    ) {

        return String.join(
                "; ",
                candidate.reasons()
        );
    }

    /*
     * -------------------------------------------------------------
     * Confidence.
     * -------------------------------------------------------------
     */
    private double calculateConfidence(
            SoilSample soil,
            IrrigationTelemetry telemetry,
            NutrientBalance nutrient,
            int candidateCount
    ) {

        int available =
                0;

        int total =
                3;

        if (soil != null) {
            available++;
        }

        if (telemetry != null) {
            available++;
        }

        if (nutrient != null) {
            available++;
        }

        /*
         * Base confidence depends on how much current
         * field information is actually available.
         */
        double confidence =
                0.55
                        + (
                        (
                                (double) available
                                        / total
                        )
                                * 0.25
                );

        /*
         * More candidates means the ranking has a broader
         * comparison set.
         */
        if (candidateCount >= 5) {

            confidence += 0.10;
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
            SoilSample soil,
            IrrigationTelemetry telemetry,
            NutrientBalance nutrient
    ) {

        boolean test =
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                );

        if (test) {

            return "TEST";
        }

        if (soil != null
                || telemetry != null
                || nutrient != null) {

            return "MIXED";
        }

        return "UNKNOWN";
    }

    /*
     * -------------------------------------------------------------
     * Uncertainty.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            SoilSample soil,
            IrrigationTelemetry telemetry,
            NutrientBalance nutrient
    ) {

        StringBuilder result =
                new StringBuilder();

        result.append(
                "This crop-rotation ranking is AgriNexus decision support and not a guaranteed yield prediction."
        );

        result.append(
                " Candidate scores are normalized weighted rule-based scores under methodology version "
        );

        result.append(
                METHODOLOGY_VERSION
        );

        result.append(
                "."
        );

        result.append(
                " The current rotation engine uses the crop metadata and current field records available in the backend."
        );

        if (soil == null) {

            result.append(
                    " No recent soil sample is available."
            );
        }

        if (telemetry == null) {

            result.append(
                    " No recent irrigation telemetry is available."
            );
        }

        if (nutrient == null) {

            result.append(
                    " No recent nutrient-balance record is available."
            );
        }

        /*
         * Historical dataset reminder.
         */
        result.append(
                " Historical crop/yield datasets should be explicitly imported, mapped and validated before being used as a predictive evidence layer."
        );

        result.append(
                " Important crop-selection decisions should be validated using current local agricultural guidance."
        );

        return result.toString();
    }

    /*
     * -------------------------------------------------------------
     * Normalize farmer constraint.
     * -------------------------------------------------------------
     */
    private String normalizeConstraint(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "NONE";
        }

        return value
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    /*
     * -------------------------------------------------------------
     * Safe string.
     * -------------------------------------------------------------
     */
    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    /*
     * -------------------------------------------------------------
     * Clamp score.
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
     * Round to 2 decimals.
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
     * Latest decision for a farm.
     * -------------------------------------------------------------
     */
    public CropRotationDecision getLatest(
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
     * Historical decisions.
     * -------------------------------------------------------------
     */
    public List<CropRotationDecision> getByFarm(
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
    public CropRotationDecision getById(
            UUID id
    ) {

        return decisionRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Crop rotation decision not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * -------------------------------------------------------------
     * Verify farm.
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
     * Candidate record.
     * -------------------------------------------------------------
     */
    private record CropCandidate(
            Crop crop,
            double score,
            List<String> reasons
    ) {
    }
}