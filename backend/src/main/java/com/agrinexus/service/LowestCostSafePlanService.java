package com.agrinexus.service;

import com.agrinexus.dto.LowestCostSafePlanRequest;
import com.agrinexus.dto.LowestCostSafePlanResponse;
import com.agrinexus.entity.AffordableCropOption;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.AffordableCropOptionRepository;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LowestCostSafePlanService {

    private static final String DEFAULT_CONSTRAINT = "LOW_COST";

    /*
     * Score budget
     *
     * Priority performance     = 60
     * Data completeness        = 10
     * Safety / compatibility   = 25
     * Preferred crop bonus     = 5
     * Total                    = 100
     *
     * These are scoring weights only.
     * No crop/farm/soil values are hardcoded here.
     */
    private static final double PRIORITY_SCORE_MAX = 60.0;
    private static final double DATA_SCORE_MAX = 10.0;
    private static final double SAFETY_SCORE_MAX = 25.0;
    private static final double PREFERRED_CROP_BONUS = 5.0;

    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final SoilSampleRepository soilSampleRepository;
    private final AffordableCropOptionRepository affordableCropOptionRepository;

    @Transactional(readOnly = true)
    public LowestCostSafePlanResponse evaluate(
            LowestCostSafePlanRequest request
    ) {

        validateRequest(request);

        /*
         * ---------------------------------------------------------
         * 1. FIND REAL FARM
         * ---------------------------------------------------------
         *
         * The farm ID comes from the frontend request.
         * No hardcoded/demo farm ID is used.
         */
        Farm farm =
                farmRepository.findById(
                        request.getFarmId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        /*
         * ---------------------------------------------------------
         * 2. NORMALIZE REQUEST
         * ---------------------------------------------------------
         */

        String targetSeason =
                normalizeSeason(
                        request.getTargetSeason()
                );

        List<String> farmerConstraints =
                normalizeConstraints(
                        request.getFarmerConstraint()
                );

        String farmerConstraint =
                String.join(
                        ",",
                        farmerConstraints
                );

        /*
         * ---------------------------------------------------------
         * 3. GET REAL LATEST SOIL SAMPLE
         * ---------------------------------------------------------
         *
         * Soil information is taken from the database.
         *
         * We never invent soil pH or soil type.
         */
        SoilSample latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                request.getFarmId()
                        );

        /*
         * ---------------------------------------------------------
         * 4. SOIL PH
         * ---------------------------------------------------------
         *
         * Priority:
         *
         * 1. Explicitly supplied field value
         * 2. Latest database soil sample
         * 3. null
         */
        Double soilPh =
                request.getSoilPh();

        if (soilPh == null
                && latestSoil != null) {

            soilPh =
                    latestSoil.getPH();
        }

        /*
         * ---------------------------------------------------------
         * 5. SOIL TYPE
         * ---------------------------------------------------------
         */

        String soilType =
                cleanString(
                        request.getSoilType()
                );

        if (soilType == null
                && latestSoil != null) {

            soilType =
                    cleanString(
                            latestSoil.getSoilType()
                    );
        }

        /*
         * ---------------------------------------------------------
         * 6. GET REAL CROP PLANNING DATA
         * ---------------------------------------------------------
         *
         * AffordableCropOption is the source used for:
         *
         * - cost
         * - water requirement
         * - energy/fuel cost
         * - fertilizer/pesticide cost
         * - duration
         * - agronomic safety tradeoff
         *
         * These records must exist in your database.
         *
         * No crop values are created here.
         */
        List<AffordableCropOption> allOptions =
                affordableCropOptionRepository.findAll();

        /*
         * ---------------------------------------------------------
         * 7. NO DATABASE PLANNING DATA
         * ---------------------------------------------------------
         */

        if (allOptions == null
                || allOptions.isEmpty()) {

            return buildInsufficientDataResponse(
                    farm,
                    targetSeason,
                    farmerConstraint,
                    soilPh,
                    soilType,
                    "No crop planning records are available in the database."
            );
        }

        /*
         * ---------------------------------------------------------
         * 8. SAFETY FILTER
         * ---------------------------------------------------------
         *
         * A crop must first pass:
         *
         * 1. Season
         * 2. Soil compatibility
         *    when required by farmer
         * 3. Water budget
         *    when supplied
         *
         * Only safe candidates are scored.
         */
        List<EvaluatedOption> evaluatedOptions =
                new ArrayList<>();

        for (AffordableCropOption option :
                allOptions) {

            /*
             * Ignore invalid database relations.
             *
             * We do not manufacture missing crop data.
             */
            if (option == null
                    || option.getCrop() == null) {

                continue;
            }

            Crop crop =
                    option.getCrop();

            /*
             * -----------------------------------------------------
             * SEASON
             * -----------------------------------------------------
             */
            boolean seasonCompatible =
                    isSeasonCompatible(
                            crop.getSeason(),
                            targetSeason
                    );

            /*
             * -----------------------------------------------------
             * SOIL
             * -----------------------------------------------------
             */
            boolean soilCompatible =
                    isSoilCompatible(
                            crop,
                            soilType,
                            soilPh
                    );

            /*
             * -----------------------------------------------------
             * WATER
             * -----------------------------------------------------
             */
            boolean waterCompatible =
                    isWaterCompatible(
                            option,
                            request.getAvailableWaterMm()
                    );

            /*
             * -----------------------------------------------------
             * HARD SAFETY GATES
             * -----------------------------------------------------
             */

            if (!seasonCompatible) {
                continue;
            }

            if (Boolean.TRUE.equals(
                    request.getRequireSoilCompatibility()
            )
                    && !soilCompatible) {

                continue;
            }

            if (!waterCompatible) {
                continue;
            }

            /*
             * Do not allow a crop with missing data to win a ranking
             * whose active priority depends on that missing data.
             */
            if (!hasRequiredRankingData(
                    option,
                    farmerConstraints
            )) {

                continue;
            }

            /*
             * -----------------------------------------------------
             * SAFE CANDIDATE
             * -----------------------------------------------------
             */
            evaluatedOptions.add(
                    new EvaluatedOption(
                            option,
                            crop,
                            0.0,
                            soilCompatible,
                            waterCompatible,
                            seasonCompatible
                    )
            );
        }

        /*
         * ---------------------------------------------------------
         * 9. NO SAFE CROP
         * ---------------------------------------------------------
         */

        if (evaluatedOptions.isEmpty()) {

            String reason =
                    buildNoCropReason(
                            targetSeason,
                            soilPh,
                            soilType,
                            request.getAvailableWaterMm(),
                            farmerConstraint
                    );

            return buildNoRecommendationResponse(
                    farm,
                    targetSeason,
                    farmerConstraint,
                    soilPh,
                    soilType,
                    reason
            );
        }

        /*
         * ---------------------------------------------------------
         * 10. CALCULATE RANGES
         * ---------------------------------------------------------
         *
         * Only SAFE crops are compared.
         */

        Double minCost =
                findMinimumCost(
                        evaluatedOptions
                );

        Double maxCost =
                findMaximumCost(
                        evaluatedOptions
                );

        Double minWater =
                findMinimumWater(
                        evaluatedOptions
                );

        Double maxWater =
                findMaximumWater(
                        evaluatedOptions
                );

        Double minEnergy =
                findMinimumEnergy(
                        evaluatedOptions
                );

        Double maxEnergy =
                findMaximumEnergy(
                        evaluatedOptions
                );

        Double minDuration =
                findMinimumDuration(
                        evaluatedOptions
                );

        Double maxDuration =
                findMaximumDuration(
                        evaluatedOptions
                );

        /*
         * ---------------------------------------------------------
         * 11. SCORE SAFE CROPS
         * ---------------------------------------------------------
         */

        List<EvaluatedOption> scoredOptions =
                new ArrayList<>();

        for (EvaluatedOption evaluated :
                evaluatedOptions) {

            double score =
                    calculateScore(
                            evaluated,
                            farmerConstraints,
                            minCost,
                            maxCost,
                            minWater,
                            maxWater,
                            minEnergy,
                            maxEnergy,
                            minDuration,
                            maxDuration
                    );

            /*
             * Preferred crop bonus.
             *
             * Only applied if the crop actually exists
             * in the database and passed safety screening.
             */
            if (request.getPreferredCropId() != null
                    && request.getPreferredCropId()
                    .equals(
                            evaluated.crop().getId()
                    )) {

                score +=
                        PREFERRED_CROP_BONUS;
            }

            /*
             * Keep score in valid range.
             */
            score =
                    Math.max(
                            0.0,
                            Math.min(
                                    100.0,
                                    score
                            )
                    );

            scoredOptions.add(
                    new EvaluatedOption(
                            evaluated.option(),
                            evaluated.crop(),
                            score,
                            evaluated.soilCompatible(),
                            evaluated.waterCompatible(),
                            evaluated.seasonCompatible()
                    )
            );
        }

        /*
         * ---------------------------------------------------------
         * 12. RANK CROPS
         * ---------------------------------------------------------
         *
         * Primary:
         *     highest recommendation score
         *
         * Secondary:
         *     lower total cost
         *
         * Tertiary:
         *     crop name
         *
         * Final:
         *     crop UUID
         *
         * This makes ranking deterministic.
         */
        scoredOptions.sort(
                Comparator
                        .comparingDouble(
                                EvaluatedOption::score
                        )
                        .reversed()

                        .thenComparing(
                                evaluated ->
                                        nullSafeCost(
                                                calculateTotalCost(
                                                        evaluated.option()
                                                )
                                        )
                        )

                        .thenComparing(
                                evaluated ->
                                        evaluated.crop()
                                                .getName() == null
                                                ? ""
                                                : evaluated.crop()
                                                .getName()
                                                .toLowerCase(
                                                        Locale.ROOT
                                                )
                        )

                        .thenComparing(
                                evaluated ->
                                        evaluated.crop().getId() == null
                                                ? ""
                                                : evaluated.crop()
                                                .getId()
                                                .toString()
                        )
        );

        /*
         * ---------------------------------------------------------
         * 13. BEST SAFE CROP
         * ---------------------------------------------------------
         */

        EvaluatedOption best =
                scoredOptions.get(0);

        /*
         * ---------------------------------------------------------
         * 14. BEST CROP DATA
         * ---------------------------------------------------------
         */

        AffordableCropOption bestOption =
                best.option();

        Crop bestCrop =
                best.crop();

        Double totalCost =
                calculateTotalCost(
                        bestOption
                );

        /*
         * ---------------------------------------------------------
         * 15. BUILD REASON
         * ---------------------------------------------------------
         */

        String reason =
                buildReason(
                        best,
                        targetSeason,
                        farmerConstraint,
                        request.getAvailableWaterMm(),
                        farmerConstraints,
                        minCost,
                        maxCost,
                        minWater,
                        maxWater,
                        minEnergy,
                        maxEnergy,
                        minDuration,
                        maxDuration
                );

        /*
         * ---------------------------------------------------------
         * 16. BUILD RECOMMENDATION
         * ---------------------------------------------------------
         */

        String recommendation =
                buildRecommendation(
                        best,
                        farmerConstraints
                );

        /*
         * ---------------------------------------------------------
         * 17. MAP RANKED OPTIONS
         * ---------------------------------------------------------
         */

        List<LowestCostSafePlanResponse.CropOption>
                rankedOptions =
                scoredOptions.stream()
                        .map(
                                this::toCropOption
                        )
                        .toList();

        /*
         * ---------------------------------------------------------
         * 18. FINAL RESPONSE
         * ---------------------------------------------------------
         */

        return LowestCostSafePlanResponse.builder()

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .targetSeason(
                        targetSeason
                )

                .farmerConstraint(
                        farmerConstraint
                )

                .soilPh(
                        soilPh
                )

                .soilType(
                        soilType
                )

                .recommendedCrop(
                        bestCrop.getName()
                )

                .recommendationScore(
                        round(
                                best.score()
                        )
                )

                .estimatedTotalCostInrPerHa(
                        totalCost
                )

                .estimatedInputCostInrPerHa(
                        bestOption
                                .getEstimatedInputCostInrPerHa()
                )

                .estimatedEnergyFuelCostInrPerHa(
                        bestOption
                                .getEstimatedEnergyFuelCostInrPerHa()
                )

                .estimatedFertilizerPesticideCostInrPerHa(
                        bestOption
                                .getEstimatedFertilizerPesticideCostInrPerHa()
                )

                .waterRequirementMmMin(
                        bestOption
                                .getWaterRequirementMmMin()
                )

                .waterRequirementMmMax(
                        bestOption
                                .getWaterRequirementMmMax()
                )

                .waterDemandClass(
                        bestOption
                                .getWaterDemandClass()
                )

                .cropDurationDaysMin(
                        bestOption
                                .getCropDurationDaysMin()
                )

                .cropDurationDaysMax(
                        bestOption
                                .getCropDurationDaysMax()
                )

                .safetyTradeoff(
                        bestOption
                                .getAgronomicSafetyTradeoff()
                )

                .reason(
                        reason
                )

                .recommendation(
                        recommendation
                )

                .dataStatus(
                        cleanString(
                                bestOption.getDataStatus()
                        )
                )

                .source(
                        cleanString(
                                bestOption.getSource()
                        )
                )

                .uncertaintyNote(
                        buildUncertaintyNote(
                                bestOption
                        )
                )

                .rankedOptions(
                        rankedOptions
                )

                .build();
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validateRequest(
            LowestCostSafePlanRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request is required"
            );
        }

        if (request.getFarmId() == null) {

            throw new IllegalArgumentException(
                    "farmId is required"
            );
        }

        if (isBlank(
                request.getTargetSeason()
        )) {

            throw new IllegalArgumentException(
                    "targetSeason is required"
            );
        }

        if (request.getAvailableWaterMm() != null
                && request.getAvailableWaterMm() < 0) {

            throw new IllegalArgumentException(
                    "availableWaterMm cannot be negative"
            );
        }

        if (request.getSoilPh() != null
                && (
                request.getSoilPh() < 0
                        || request.getSoilPh() > 14
        )) {

            throw new IllegalArgumentException(
                    "soilPh must be between 0 and 14"
            );
        }
    }

    /*
     * =========================================================
     * SEASON NORMALIZATION
     * =========================================================
     */

    private String normalizeSeason(
            String season
    ) {

        if (season == null) {
            return null;
        }

        String value =
                season
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        return switch (value) {

            case "RABI" ->
                    "RABI";

            case "KHARIF" ->
                    "KHARIF";

            case "SUMMER", "ZAID" ->
                    "SUMMER";

            default ->
                    value;
        };
    }

    private boolean isSeasonCompatible(
            String cropSeason,
            String targetSeason
    ) {

        if (isBlank(cropSeason)
                || isBlank(targetSeason)) {

            return false;
        }

        String normalizedTarget =
                normalizeSeason(
                        targetSeason
                );

        String normalizedCropSeason =
                cropSeason
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .replace(
                                "/",
                                ","
                        );

        String[] seasons =
                normalizedCropSeason.split(
                        ","
                );

        for (String season :
                seasons) {

            String clean =
                    normalizeSeason(
                            season
                    );

            if (normalizedTarget.equals(
                    clean
            )) {

                return true;
            }
        }

        return normalizedCropSeason.contains(
                normalizedTarget
        );
    }

    /*
     * =========================================================
     * SOIL COMPATIBILITY
     * =========================================================
     */

    private boolean isSoilCompatible(
            Crop crop,
            String soilType,
            Double soilPh
    ) {

        if (crop == null) {
            return false;
        }

        /*
         * Soil type
         */
        boolean soilTypeKnown =
                !isBlank(
                        soilType
                );

        boolean cropSoilKnown =
                !isBlank(
                        crop.getSoilType()
                );

        boolean soilTypeCompatible =
                false;

        if (soilTypeKnown
                && cropSoilKnown) {

            soilTypeCompatible =
                    matchesSoilType(
                            soilType,
                            crop.getSoilType()
                    );
        }

        /*
         * Soil pH
         */
        boolean cropPhRangeKnown =
                crop.getSoilPhMin() != null
                        && crop.getSoilPhMax() != null;

        boolean phCompatible =
                false;

        if (soilPh != null
                && cropPhRangeKnown) {

            phCompatible =
                    soilPh >= crop.getSoilPhMin()
                            && soilPh <= crop.getSoilPhMax();
        }

        /*
         * Both soil type and pH must be compatible.
         */
        return soilTypeCompatible
                && phCompatible;
    }

    private boolean matchesSoilType(
            String actualSoil,
            String requiredSoil
    ) {

        if (isBlank(actualSoil)
                || isBlank(requiredSoil)) {

            return false;
        }

        String actual =
                actualSoil
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();

        String[] requiredTypes =
                requiredSoil
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .split(",");

        for (String required :
                requiredTypes) {

            String normalizedRequired =
                    required.trim();

            if (normalizedRequired.isEmpty()) {
                continue;
            }

            if (actual.equals(
                    normalizedRequired
            )
                    || actual.contains(
                    normalizedRequired
            )
                    || normalizedRequired.contains(
                    actual
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * WATER COMPATIBILITY
     * =========================================================
     */

    private boolean isWaterCompatible(
            AffordableCropOption option,
            Double availableWaterMm
    ) {

        /*
         * If farmer did not provide a water budget,
         * don't reject crop based on unknown budget.
         */
        if (availableWaterMm == null) {

            return true;
        }

        Double minimumWater =
                option.getWaterRequirementMmMin();

        /*
         * Unknown water requirement means
         * we cannot claim that the crop fits.
         */
        if (minimumWater == null) {

            return false;
        }

        return minimumWater <= availableWaterMm;
    }

    /*
     * =========================================================
     * REQUIRED RANKING DATA
     * =========================================================
     *
     * A crop cannot win a ranking priority if its
     * required real data is missing.
     */

    private boolean hasRequiredRankingData(
            AffordableCropOption option,
            List<String> constraints
    ) {

        if (constraints == null
                || constraints.isEmpty()) {

            return calculateTotalCost(
                    option
            ) != null;
        }

        for (String constraint :
                constraints) {

            switch (constraint) {

                case "LOW_COST" -> {

                    if (calculateTotalCost(
                            option
                    ) == null) {

                        return false;
                    }
                }

                case "LOW_WATER" -> {

                    if (option.getWaterRequirementMmMin()
                            == null) {

                        return false;
                    }
                }

                case "LOW_ENERGY" -> {

                    if (option.getEstimatedEnergyFuelCostInrPerHa()
                            == null) {

                        return false;
                    }
                }

                case "SHORT_DURATION" -> {

                    if (option.getCropDurationDaysMin()
                            == null) {

                        return false;
                    }
                }

                case "NONE" -> {
                    // No ranking metric is required.
                }

                default -> {
                    // normalizeConstraints removes unsupported values.
                }
            }
        }

        return true;
    }

    /*
     * =========================================================
     * SCORE CALCULATION
     * =========================================================
     */

    private double calculateScore(
            EvaluatedOption evaluated,
            List<String> constraints,
            Double minCost,
            Double maxCost,
            Double minWater,
            Double maxWater,
            Double minEnergy,
            Double maxEnergy,
            Double minDuration,
            Double maxDuration
    ) {

        AffordableCropOption option =
                evaluated.option();

        List<String> activeConstraints =
                constraints == null
                        || constraints.isEmpty()
                        ? List.of(
                        DEFAULT_CONSTRAINT
                )
                        : constraints;

        /*
         * Equal weight for each user-selected priority.
         *
         * Example:
         *
         * LOW_COST
         * LOW_WATER
         *
         * 60 points total is split equally:
         *
         * 30 cost
         * 30 water
         */
        double pointsPerPriority =
                PRIORITY_SCORE_MAX
                        / activeConstraints.size();

        double priorityScore =
                0.0;

        for (String constraint :
                activeConstraints) {

            switch (constraint) {

                case "LOW_COST" -> {

                    Double cost =
                            calculateTotalCost(
                                    option
                            );

                    priorityScore +=
                            relativeLowerIsBetter(
                                    cost,
                                    minCost,
                                    maxCost
                            )
                                    * pointsPerPriority;
                }

                case "LOW_WATER" -> {

                    Double water =
                            option
                                    .getWaterRequirementMmMin();

                    priorityScore +=
                            relativeLowerIsBetter(
                                    water,
                                    minWater,
                                    maxWater
                            )
                                    * pointsPerPriority;
                }

                case "LOW_ENERGY" -> {

                    Double energy =
                            option
                                    .getEstimatedEnergyFuelCostInrPerHa();

                    priorityScore +=
                            relativeLowerIsBetter(
                                    energy,
                                    minEnergy,
                                    maxEnergy
                            )
                                    * pointsPerPriority;
                }

                case "SHORT_DURATION" -> {

                    Double duration =
                            option
                                    .getCropDurationDaysMin()
                                    == null
                                    ? null
                                    : option
                                    .getCropDurationDaysMin()
                                    .doubleValue();

                    priorityScore +=
                            relativeLowerIsBetter(
                                    duration,
                                    minDuration,
                                    maxDuration
                            )
                                    * pointsPerPriority;
                }

                case "NONE" -> {

                    priorityScore +=
                            0.5
                                    * pointsPerPriority;
                }

                default -> {
                    // Unsupported values are removed during normalization.
                }
            }
        }

        /*
         * Data completeness score
         */
        double dataScore =
                calculateDataCompletenessScore(
                        option
                );

        /*
         * Safety score
         */
        double safetyScore =
                calculateSafetyScore(
                        evaluated
                );

        /*
         * Total score
         */
        double finalScore =
                priorityScore
                        + dataScore
                        + safetyScore;

        return Math.min(
                100.0,
                finalScore
        );
    }

    /*
     * =========================================================
     * LOWER IS BETTER
     * =========================================================
     */

    private double relativeLowerIsBetter(
            Double value,
            Double minimum,
            Double maximum
    ) {

        if (value == null
                || minimum == null
                || maximum == null) {

            return 0.0;
        }

        /*
         * Only one unique value exists.
         * Every candidate gets full relative score.
         */
        if (maximum <= minimum) {

            return 1.0;
        }

        double normalized =
                (
                        maximum - value
                )
                        /
                        (
                                maximum - minimum
                        );

        return clamp01(
                normalized
        );
    }

    private double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    /*
     * =========================================================
     * DATA COMPLETENESS
     * =========================================================
     *
     * Only actual database fields are checked.
     * No values are guessed.
     */

    private double calculateDataCompletenessScore(
            AffordableCropOption option
    ) {

        if (option == null) {
            return 0.0;
        }

        int knownFields =
                0;

        int totalFields =
                7;

        /*
         * Cost component 1
         */
        if (option.getEstimatedInputCostInrPerHa()
                != null) {

            knownFields++;
        }

        /*
         * Cost component 2
         */
        if (option.getEstimatedEnergyFuelCostInrPerHa()
                != null) {

            knownFields++;
        }

        /*
         * Cost component 3
         */
        if (option.getEstimatedFertilizerPesticideCostInrPerHa()
                != null) {

            knownFields++;
        }

        /*
         * Water minimum
         */
        if (option.getWaterRequirementMmMin()
                != null) {

            knownFields++;
        }

        /*
         * Water maximum
         */
        if (option.getWaterRequirementMmMax()
                != null) {

            knownFields++;
        }

        /*
         * Duration minimum
         */
        if (option.getCropDurationDaysMin()
                != null) {

            knownFields++;
        }

        /*
         * Duration maximum
         */
        if (option.getCropDurationDaysMax()
                != null) {

            knownFields++;
        }

        return (
                (double) knownFields
                        / totalFields
        )
                * DATA_SCORE_MAX;
    }

    /*
     * =========================================================
     * SAFETY SCORE
     * =========================================================
     */

    private double calculateSafetyScore(
            EvaluatedOption evaluated
    ) {

        double score =
                0.0;

        /*
         * Season passed hard gate.
         */
        if (evaluated.seasonCompatible()) {

            score += 10.0;
        }

        /*
         * Water passed hard gate.
         */
        if (evaluated.waterCompatible()) {

            score += 5.0;
        }

        /*
         * Soil compatibility.
         */
        if (evaluated.soilCompatible()) {

            score += 10.0;
        }

        return Math.min(
                SAFETY_SCORE_MAX,
                score
        );
    }

    /*
     * =========================================================
     * COST RANGE
     * =========================================================
     */

    private Double findMinimumCost(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                calculateTotalCost(
                                        evaluated.option()
                                )
                )

                .filter(
                        this::isNonNegative
                )

                .min(
                        Double::compareTo
                )

                .orElse(null);
    }

    private Double findMaximumCost(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                calculateTotalCost(
                                        evaluated.option()
                                )
                )

                .filter(
                        this::isNonNegative
                )

                .max(
                        Double::compareTo
                )

                .orElse(null);
    }

    /*
     * =========================================================
     * WATER RANGE
     * =========================================================
     */

    private Double findMinimumWater(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getWaterRequirementMmMin()
                )

                .filter(
                        this::isNonNegative
                )

                .min(
                        Double::compareTo
                )

                .orElse(null);
    }

    private Double findMaximumWater(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getWaterRequirementMmMin()
                )

                .filter(
                        this::isNonNegative
                )

                .max(
                        Double::compareTo
                )

                .orElse(null);
    }

    /*
     * =========================================================
     * ENERGY RANGE
     * =========================================================
     */

    private Double findMinimumEnergy(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getEstimatedEnergyFuelCostInrPerHa()
                )

                .filter(
                        this::isNonNegative
                )

                .min(
                        Double::compareTo
                )

                .orElse(null);
    }

    private Double findMaximumEnergy(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getEstimatedEnergyFuelCostInrPerHa()
                )

                .filter(
                        this::isNonNegative
                )

                .max(
                        Double::compareTo
                )

                .orElse(null);
    }

    /*
     * =========================================================
     * DURATION RANGE
     * =========================================================
     */

    private Double findMinimumDuration(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getCropDurationDaysMin()
                )

                .filter(
                        value ->
                                value != null
                                        && value >= 0
                )

                .map(
                        Integer::doubleValue
                )

                .min(
                        Double::compareTo
                )

                .orElse(null);
    }

    private Double findMaximumDuration(
            List<EvaluatedOption> options
    ) {

        return options.stream()

                .map(
                        evaluated ->
                                evaluated
                                        .option()
                                        .getCropDurationDaysMin()
                )

                .filter(
                        value ->
                                value != null
                                        && value >= 0
                )

                .map(
                        Integer::doubleValue
                )

                .max(
                        Double::compareTo
                )

                .orElse(null);
    }

    private boolean isNonNegative(
            Double value
    ) {

        return value != null
                && value >= 0;
    }

    /*
     * =========================================================
     * TOTAL COST
     * =========================================================
     *
     * IMPORTANT:
     *
     * Missing cost components are NOT treated as zero.
     *
     * Total cost is considered known only when all three
     * stored cost components are present and non-negative.
     */

    private Double calculateTotalCost(
            AffordableCropOption option
    ) {

        if (option == null) {
            return null;
        }

        if (option.getEstimatedInputCostInrPerHa()
                == null

                || option.getEstimatedEnergyFuelCostInrPerHa()
                == null

                || option.getEstimatedFertilizerPesticideCostInrPerHa()
                == null) {

            return null;
        }

        if (option.getEstimatedInputCostInrPerHa()
                < 0

                || option.getEstimatedEnergyFuelCostInrPerHa()
                < 0

                || option.getEstimatedFertilizerPesticideCostInrPerHa()
                < 0) {

            return null;
        }

        double total =
                option.getEstimatedInputCostInrPerHa()

                        + option.getEstimatedEnergyFuelCostInrPerHa()

                        + option.getEstimatedFertilizerPesticideCostInrPerHa();

        return round(
                total
        );
    }

    private double nullSafeCost(
            Double value
    ) {

        return value == null
                ? Double.MAX_VALUE
                : value;
    }

    /*
     * =========================================================
     * RESPONSE MAPPING
     * =========================================================
     */

    private LowestCostSafePlanResponse.CropOption
    toCropOption(
            EvaluatedOption evaluated
    ) {

        AffordableCropOption option =
                evaluated.option();

        Crop crop =
                evaluated.crop();

        return LowestCostSafePlanResponse
                .CropOption
                .builder()

                .cropId(
                        crop.getId()
                )

                .cropName(
                        crop.getName()
                )

                .category(
                        crop.getCategory()
                )

                .season(
                        crop.getSeason()
                )

                .recommendationScore(
                        round(
                                evaluated.score()
                        )
                )

                .totalCostInrPerHa(
                        calculateTotalCost(
                                option
                        )
                )

                .inputCostInrPerHa(
                        option.getEstimatedInputCostInrPerHa()
                )

                .energyFuelCostInrPerHa(
                        option.getEstimatedEnergyFuelCostInrPerHa()
                )

                .fertilizerPesticideCostInrPerHa(
                        option.getEstimatedFertilizerPesticideCostInrPerHa()
                )

                .waterRequirementMmMin(
                        option.getWaterRequirementMmMin()
                )

                .waterRequirementMmMax(
                        option.getWaterRequirementMmMax()
                )

                .durationDaysMin(
                        option.getCropDurationDaysMin()
                )

                .durationDaysMax(
                        option.getCropDurationDaysMax()
                )

                .waterDemandClass(
                        option.getWaterDemandClass()
                )

                .soilCompatible(
                        evaluated.soilCompatible()
                )

                .waterCompatible(
                        evaluated.waterCompatible()
                )

                .seasonCompatible(
                        evaluated.seasonCompatible()
                )

                .safetyTradeoff(
                        option.getAgronomicSafetyTradeoff()
                )

                .dataStatus(
                        option.getDataStatus()
                )

                .build();
    }

    /*
     * =========================================================
     * REASON
     * =========================================================
     */

    private String buildReason(
            EvaluatedOption best,
            String targetSeason,
            String farmerConstraint,
            Double waterBudget,
            List<String> farmerConstraints,
            Double minCost,
            Double maxCost,
            Double minWater,
            Double maxWater,
            Double minEnergy,
            Double maxEnergy,
            Double minDuration,
            Double maxDuration
    ) {

        Crop crop =
                best.crop();

        AffordableCropOption option =
                best.option();

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                crop.getName()
        );

        reason.append(
                " passed the configured safety screening "
        );

        reason.append(
                "for target season "
        );

        reason.append(
                targetSeason
        );

        reason.append(
                ". "
        );

        if (best.soilCompatible()) {

            reason.append(
                    "Soil type and pH are compatible with the stored crop requirements. "
            );

        } else {

            reason.append(
                    "Complete soil compatibility was not verified from the available field data. "
            );
        }

        if (waterBudget != null) {

            reason.append(
                    "Available water budget="
            );

            reason.append(
                    waterBudget
            );

            reason.append(
                    " mm was applied. "
            );
        }

        reason.append(
                "Farmer constraint="
        );

        reason.append(
                farmerConstraint
        );

        reason.append(
                ". "
        );

        reason.append(
                "Recommendation score="
        );

        reason.append(
                round(best.score())
        );

        reason.append(
                "/100. "
        );

        reason.append(
                "Priority ranking uses real stored values: "
        );

        for (int i = 0;
             i < farmerConstraints.size();
             i++) {

            if (i > 0) {
                reason.append(
                        ", "
                );
            }

            reason.append(
                    farmerConstraints.get(i)
            );
        }

        reason.append(
                ". "
        );

        /*
         * LOW COST
         */
        if (farmerConstraints.contains(
                "LOW_COST"
        )) {

            reason.append(
                    "Total cost="
            );

            reason.append(
                    calculateTotalCost(
                            option
                    )
            );

            reason.append(
                    " INR/ha; safe-option range="
            );

            reason.append(
                    minCost
            );

            reason.append(
                    " to "
            );

            reason.append(
                    maxCost
            );

            reason.append(
                    " INR/ha. "
            );
        }

        /*
         * LOW WATER
         */
        if (farmerConstraints.contains(
                "LOW_WATER"
        )) {

            reason.append(
                    "Water requirement="
            );

            reason.append(
                    option.getWaterRequirementMmMin()
            );

            reason.append(
                    " mm; safe-option range="
            );

            reason.append(
                    minWater
            );

            reason.append(
                    " to "
            );

            reason.append(
                    maxWater
            );

            reason.append(
                    " mm. "
            );
        }

        /*
         * LOW ENERGY
         */
        if (farmerConstraints.contains(
                "LOW_ENERGY"
        )) {

            reason.append(
                    "Energy/fuel cost="
            );

            reason.append(
                    option.getEstimatedEnergyFuelCostInrPerHa()
            );

            reason.append(
                    " INR/ha; safe-option range="
            );

            reason.append(
                    minEnergy
            );

            reason.append(
                    " to "
            );

            reason.append(
                    maxEnergy
            );

            reason.append(
                    " INR/ha. "
            );
        }

        /*
         * SHORT DURATION
         */
        if (farmerConstraints.contains(
                "SHORT_DURATION"
        )) {

            reason.append(
                    "Minimum duration="
            );

            reason.append(
                    option.getCropDurationDaysMin()
            );

            reason.append(
                    " days; safe-option range="
            );

            reason.append(
                    minDuration
            );

            reason.append(
                    " to "
            );

            reason.append(
                    maxDuration
            );

            reason.append(
                    " days. "
            );
        }

        if (!isBlank(
                option.getAgronomicSafetyTradeoff()
        )) {

            reason.append(
                    option.getAgronomicSafetyTradeoff()
            );
        }

        return reason
                .toString()
                .trim();
    }

    /*
     * =========================================================
     * RECOMMENDATION TEXT
     * =========================================================
     */

    private String buildRecommendation(
            EvaluatedOption best,
            List<String> farmerConstraints
    ) {

        Crop crop =
                best.crop();

        AffordableCropOption option =
                best.option();

        StringBuilder recommendation =
                new StringBuilder();

        recommendation.append(
                "Consider "
        );

        recommendation.append(
                crop.getName()
        );

        recommendation.append(
                " based on the available "
                        + "farm, soil and crop planning data."
        );

        if (farmerConstraints.contains(
                "LOW_COST"
        )) {

            recommendation.append(
                    " Lower total cost was prioritized."
            );
        }

        if (farmerConstraints.contains(
                "LOW_WATER"
        )) {

            recommendation.append(
                    " Lower water demand was prioritized."
            );
        }

        if (farmerConstraints.contains(
                "LOW_ENERGY"
        )) {

            recommendation.append(
                    " Lower energy/fuel cost was prioritized."
            );
        }

        if (farmerConstraints.contains(
                "SHORT_DURATION"
        )) {

            recommendation.append(
                    " Shorter crop duration was prioritized."
            );
        }

        if (!isBlank(
                option.getRecommendationRule()
        )) {

            recommendation.append(
                    " "
            );

            recommendation.append(
                    option.getRecommendationRule()
            );
        }

        return recommendation.toString();
    }

    /*
     * =========================================================
     * NO CROP REASON
     * =========================================================
     */

    private String buildNoCropReason(
            String targetSeason,
            Double soilPh,
            String soilType,
            Double waterBudget,
            String farmerConstraint
    ) {

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                "No crop in the available database records "
                        + "passed the configured safety screening "
                        + "for target season "
        );

        reason.append(
                targetSeason
        );

        reason.append(
                ". "
        );

        if (soilPh == null) {

            reason.append(
                    "Current soil pH is unavailable. "
            );

        } else {

            reason.append(
                    "Current soil pH="
            );

            reason.append(
                    soilPh
            );

            reason.append(
                    ". "
            );
        }

        if (isBlank(
                soilType
        )) {

            reason.append(
                    "Current soil type is unavailable. "
            );

        } else {

            reason.append(
                    "Current soil type="
            );

            reason.append(
                    soilType
            );

            reason.append(
                    ". "
            );
        }

        if (waterBudget != null) {

            reason.append(
                    "Available water budget="
            );

            reason.append(
                    waterBudget
            );

            reason.append(
                    " mm was applied. "
            );
        }

        reason.append(
                "Farmer constraint="
        );

        reason.append(
                farmerConstraint
        );

        reason.append(
                ". "
        );

        reason.append(
                "No missing field or crop values were "
                        + "invented to produce a recommendation."
        );

        return reason.toString();
    }

    /*
     * =========================================================
     * INSUFFICIENT DATA RESPONSE
     * =========================================================
     */

    private LowestCostSafePlanResponse
    buildInsufficientDataResponse(
            Farm farm,
            String targetSeason,
            String farmerConstraint,
            Double soilPh,
            String soilType,
            String reason
    ) {

        return LowestCostSafePlanResponse
                .builder()

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .targetSeason(
                        targetSeason
                )

                .farmerConstraint(
                        farmerConstraint
                )

                .soilPh(
                        soilPh
                )

                .soilType(
                        soilType
                )

                .recommendedCrop(
                        null
                )

                .recommendationScore(
                        0.0
                )

                .reason(
                        reason
                )

                .recommendation(
                        "A crop recommendation cannot be generated "
                                + "until sufficient database data is available."
                )

                .dataStatus(
                        "INSUFFICIENT_DATA"
                )

                .source(
                        "database"
                )

                .uncertaintyNote(
                        "Required planning information is missing. "
                                + "No values were guessed."
                )

                .rankedOptions(
                        List.of()
                )

                .build();
    }

    /*
     * =========================================================
     * NO RECOMMENDATION RESPONSE
     * =========================================================
     */

    private LowestCostSafePlanResponse
    buildNoRecommendationResponse(
            Farm farm,
            String targetSeason,
            String farmerConstraint,
            Double soilPh,
            String soilType,
            String reason
    ) {

        return LowestCostSafePlanResponse
                .builder()

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .targetSeason(
                        targetSeason
                )

                .farmerConstraint(
                        farmerConstraint
                )

                .soilPh(
                        soilPh
                )

                .soilType(
                        soilType
                )

                .recommendedCrop(
                        null
                )

                .recommendationScore(
                        0.0
                )

                .reason(
                        reason
                )

                .recommendation(
                        "No crop can be recommended from the "
                                + "available verified data."
                )

                .dataStatus(
                        "INSUFFICIENT_DATA"
                )

                .source(
                        "database"
                )

                .uncertaintyNote(
                        "The available field and crop data did not "
                                + "support a safe recommendation."
                )

                .rankedOptions(
                        List.of()
                )

                .build();
    }

    /*
     * =========================================================
     * UNCERTAINTY
     * =========================================================
     */

    private String buildUncertaintyNote(
            AffordableCropOption option
    ) {

        if (option == null) {

            return "Crop planning data is unavailable.";
        }

        if (!isBlank(
                option.getDataStatus()
        )) {

            return option.getDataStatus();
        }

        return "Data verification status is not specified "
                + "in the crop planning record.";
    }

    /*
     * =========================================================
     * CONSTRAINT NORMALIZATION
     * =========================================================
     */

    private List<String> normalizeConstraints(
            String constraint
    ) {

        if (isBlank(
                constraint
        )) {

            return List.of(
                    DEFAULT_CONSTRAINT
            );
        }

        String normalized =
                constraint
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        String[] parts =
                normalized.split(
                        ","
                );

        List<String> result =
                new ArrayList<>();

        for (String part :
                parts) {

            String value =
                    part.trim();

            if (value.isEmpty()) {
                continue;
            }

            String normalizedValue =
                    switch (value) {

                        case "LOW_COST" ->
                                "LOW_COST";

                        case "LOW_WATER" ->
                                "LOW_WATER";

                        case "LOW_ENERGY" ->
                                "LOW_ENERGY";

                        case "SHORT_DURATION" ->
                                "SHORT_DURATION";

                        case "NONE" ->
                                "NONE";

                        default ->
                                null;
                    };

            if (normalizedValue != null
                    && !result.contains(
                    normalizedValue
            )) {

                result.add(
                        normalizedValue
                );
            }
        }

        if (result.isEmpty()) {

            return List.of(
                    DEFAULT_CONSTRAINT
            );
        }

        /*
         * NONE cannot be combined with another priority.
         */
        if (result.size() > 1
                && result.contains(
                "NONE"
        )) {

            result.remove(
                    "NONE"
            );
        }

        return result;
    }

    /*
     * =========================================================
     * STRING HELPERS
     * =========================================================
     */

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }

    private String cleanString(
            String value
    ) {

        if (isBlank(
                value
        )) {

            return null;
        }

        return value.trim();
    }

    private String safeLower(
            String value
    ) {

        return value == null
                ? ""
                : value.toLowerCase(
                Locale.ROOT
        );
    }

    /*
     * =========================================================
     * NUMBER HELPER
     * =========================================================
     */

    private Double round(
            Double value
    ) {

        if (value == null) {
            return null;
        }

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    /*
     * =========================================================
     * INTERNAL EVALUATION RECORD
     * =========================================================
     */

    private record EvaluatedOption(

            AffordableCropOption option,

            Crop crop,

            double score,

            boolean soilCompatible,

            boolean waterCompatible,

            boolean seasonCompatible

    ) {
    }
}