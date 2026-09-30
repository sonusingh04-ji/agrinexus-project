package com.agrinexus.service;

import com.agrinexus.dto.SoilRecoveryPlanRequest;
import com.agrinexus.dto.SoilRecoveryPlanResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.NutrientBalance;
import com.agrinexus.entity.SoilRecoveryPlan;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.NutrientBalanceRepository;
import com.agrinexus.repository.SoilRecoveryPlanRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoilRecoveryPlanService {

    private final SoilRecoveryPlanRepository soilRecoveryPlanRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final SoilSampleRepository soilSampleRepository;
    private final NutrientBalanceRepository nutrientBalanceRepository;

    /*
     * Create a soil recovery plan.
     */
    public SoilRecoveryPlanResponse createPlan(
            SoilRecoveryPlanRequest request
    ) {

        validateRequest(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        Crop crop = null;

        if (request.getCropId() != null) {

            crop =
                    cropRepository.findById(request.getCropId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }

        /*
         * Get latest measured soil information when available.
         */
        SoilSample latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                request.getFarmId()
                        );

        /*
         * Get latest calculated nutrient balance when available.
         */
        NutrientBalance latestBalance =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                request.getFarmId()
                        );

        /*
         * Current value priority:
         *
         * 1. Explicit request value
         * 2. Latest measured/calculated value according
         *    to the selected focus area
         * 3. null if not available
         */
        Double currentValue =
                request.getCurrentValue() != null
                        ? request.getCurrentValue()
                        : deriveCurrentValue(
                        request.getFocusArea(),
                        latestSoil,
                        latestBalance
                );

        /*
         * Determine trend only when enough values exist.
         */
        String trend =
                request.getTrend();

        if (trend == null || trend.isBlank()) {

            trend =
                    determineTrend(
                            request.getBaselineValue(),
                            currentValue,
                            request.getTargetValue()
                    );
        }

        /*
         * If the caller did not supply a management action,
         * create a conservative explainable action based on
         * the focus area and trend.
         */
        String actionType =
                request.getActionType();

        if (actionType == null
                || actionType.isBlank()) {

            actionType =
                    determineAction(
                            request.getFocusArea(),
                            trend
                    );
        }

        /*
         * If status is not supplied, derive a reasonable
         * plan status.
         */
        String status =
                request.getStatus();

        if (status == null
                || status.isBlank()) {

            status =
                    "PLANNED";
        }

        /*
         * Build recommendation if not explicitly supplied.
         */
        String recommendation =
                request.getRecommendation();

        if (recommendation == null
                || recommendation.isBlank()) {

            recommendation =
                    buildRecommendation(
                            request.getFocusArea(),
                            trend,
                            actionType
                    );
        }

        /*
         * Build explanation if not supplied.
         */
        String reason =
                request.getReason();

        if (reason == null
                || reason.isBlank()) {

            reason =
                    buildReason(
                            request.getFocusArea(),
                            request.getBaselineValue(),
                            currentValue,
                            request.getTargetValue(),
                            trend
                    );
        }

        /*
         * Determine provenance.
         */
        String dataStatus =
                request.getDataStatus();

        if (dataStatus == null
                || dataStatus.isBlank()) {

            dataStatus =
                    determineDataStatus(
                            request,
                            latestSoil,
                            latestBalance
                    );
        }

        SoilRecoveryPlan plan =
                SoilRecoveryPlan.builder()

                        .farm(farm)

                        .crop(crop)

                        .seasonNumber(
                                request.getSeasonNumber()
                        )

                        .season(
                                request.getSeason()
                        )

                        .startDate(
                                request.getStartDate()
                        )

                        .endDate(
                                request.getEndDate()
                        )

                        .focusArea(
                                request.getFocusArea()
                        )

                        .baselineValue(
                                request.getBaselineValue()
                        )

                        .targetValue(
                                request.getTargetValue()
                        )

                        .unit(
                                request.getUnit()
                        )

                        .currentValue(
                                currentValue
                        )

                        .trend(
                                trend
                        )

                        .actionType(
                                actionType
                        )

                        .recommendation(
                                recommendation
                        )

                        .reason(
                                reason
                        )

                        .status(
                                status
                        )

                        .dataStatus(
                                dataStatus
                        )

                        .source(
                                request.getSource()
                        )

                        .uncertaintyNote(
                                buildUncertaintyNote(
                                        request,
                                        currentValue
                                )
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();

        SoilRecoveryPlan savedPlan =
                soilRecoveryPlanRepository.save(plan);

        return toResponse(savedPlan);
    }

    /*
     * Get plan by ID.
     */
    public SoilRecoveryPlanResponse getPlanById(
            UUID planId
    ) {

        SoilRecoveryPlan plan =
                soilRecoveryPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Soil recovery plan not found with ID: "
                                                + planId
                                )
                        );

        return toResponse(plan);
    }

    /*
     * Get all recovery plans for a farm.
     */
    public List<SoilRecoveryPlanResponse> getFarmPlans(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        return soilRecoveryPlanRepository
                .findByFarmIdOrderBySeasonNumberAsc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get plans associated with a crop.
     */
    public List<SoilRecoveryPlanResponse> getCropPlans(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(farmId);
        verifyCropExists(cropId);

        return soilRecoveryPlanRepository
                .findByFarmIdAndCropIdOrderBySeasonNumberAsc(
                        farmId,
                        cropId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get plans by status.
     */
    public List<SoilRecoveryPlanResponse> getPlansByStatus(
            UUID farmId,
            String status
    ) {

        verifyFarmExists(farmId);

        if (status == null || status.isBlank()) {

            throw new RuntimeException(
                    "Status is required"
            );
        }

        return soilRecoveryPlanRepository
                .findByFarmIdAndStatusIgnoreCaseOrderBySeasonNumberAsc(
                        farmId,
                        status
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get plans by focus area.
     */
    public List<SoilRecoveryPlanResponse> getPlansByFocusArea(
            UUID farmId,
            String focusArea
    ) {

        verifyFarmExists(farmId);

        if (focusArea == null || focusArea.isBlank()) {

            throw new RuntimeException(
                    "Focus area is required"
            );
        }

        return soilRecoveryPlanRepository
                .findByFarmIdAndFocusAreaIgnoreCaseOrderBySeasonNumberAsc(
                        farmId,
                        focusArea
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get plans by trend.
     */
    public List<SoilRecoveryPlanResponse> getPlansByTrend(
            UUID farmId,
            String trend
    ) {

        verifyFarmExists(farmId);

        if (trend == null || trend.isBlank()) {

            throw new RuntimeException(
                    "Trend is required"
            );
        }

        return soilRecoveryPlanRepository
                .findByFarmIdAndTrendIgnoreCaseOrderBySeasonNumberAsc(
                        farmId,
                        trend
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get latest plan for farm.
     */
    public SoilRecoveryPlanResponse getLatestFarmPlan(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        SoilRecoveryPlan plan =
                soilRecoveryPlanRepository
                        .findFirstByFarmIdOrderBySeasonNumberDesc(
                                farmId
                        );

        if (plan == null) {
            return null;
        }

        return toResponse(plan);
    }

    /*
     * Get latest plan for a crop.
     */
    public SoilRecoveryPlanResponse getLatestCropPlan(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(farmId);
        verifyCropExists(cropId);

        SoilRecoveryPlan plan =
                soilRecoveryPlanRepository
                        .findFirstByFarmIdAndCropIdOrderBySeasonNumberDesc(
                                farmId,
                                cropId
                        );

        if (plan == null) {
            return null;
        }

        return toResponse(plan);
    }

    /*
     * Get active plans.
     */
    public List<SoilRecoveryPlanResponse> getActivePlans(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        return soilRecoveryPlanRepository
                .findByFarmIdAndStatusIgnoreCase(
                        farmId,
                        "ACTIVE"
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Update a recovery plan.
     */
    public SoilRecoveryPlanResponse updatePlan(
            UUID planId,
            SoilRecoveryPlanRequest request
    ) {

        validateRequest(request);

        SoilRecoveryPlan plan =
                soilRecoveryPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Soil recovery plan not found with ID: "
                                                + planId
                                )
                        );

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        Crop crop = null;

        if (request.getCropId() != null) {

            crop =
                    cropRepository.findById(request.getCropId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }

        SoilSample latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                request.getFarmId()
                        );

        NutrientBalance latestBalance =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                request.getFarmId()
                        );

        Double currentValue =
                request.getCurrentValue() != null
                        ? request.getCurrentValue()
                        : deriveCurrentValue(
                        request.getFocusArea(),
                        latestSoil,
                        latestBalance
                );

        String trend =
                request.getTrend();

        if (trend == null || trend.isBlank()) {

            trend =
                    determineTrend(
                            request.getBaselineValue(),
                            currentValue,
                            request.getTargetValue()
                    );
        }

        String actionType =
                request.getActionType();

        if (actionType == null
                || actionType.isBlank()) {

            actionType =
                    determineAction(
                            request.getFocusArea(),
                            trend
                    );
        }

        String recommendation =
                request.getRecommendation();

        if (recommendation == null
                || recommendation.isBlank()) {

            recommendation =
                    buildRecommendation(
                            request.getFocusArea(),
                            trend,
                            actionType
                    );
        }

        String reason =
                request.getReason();

        if (reason == null
                || reason.isBlank()) {

            reason =
                    buildReason(
                            request.getFocusArea(),
                            request.getBaselineValue(),
                            currentValue,
                            request.getTargetValue(),
                            trend
                    );
        }

        String dataStatus =
                request.getDataStatus();

        if (dataStatus == null
                || dataStatus.isBlank()) {

            dataStatus =
                    determineDataStatus(
                            request,
                            latestSoil,
                            latestBalance
                    );
        }

        plan.setFarm(farm);
        plan.setCrop(crop);

        plan.setSeasonNumber(
                request.getSeasonNumber()
        );

        plan.setSeason(
                request.getSeason()
        );

        plan.setStartDate(
                request.getStartDate()
        );

        plan.setEndDate(
                request.getEndDate()
        );

        plan.setFocusArea(
                request.getFocusArea()
        );

        plan.setBaselineValue(
                request.getBaselineValue()
        );

        plan.setTargetValue(
                request.getTargetValue()
        );

        plan.setUnit(
                request.getUnit()
        );

        plan.setCurrentValue(
                currentValue
        );

        plan.setTrend(
                trend
        );

        plan.setActionType(
                actionType
        );

        plan.setRecommendation(
                recommendation
        );

        plan.setReason(
                reason
        );

        plan.setStatus(
                request.getStatus() != null
                        && !request.getStatus().isBlank()
                        ? request.getStatus()
                        : "PLANNED"
        );

        plan.setDataStatus(
                dataStatus
        );

        plan.setSource(
                request.getSource()
        );

        plan.setUncertaintyNote(
                buildUncertaintyNote(
                        request,
                        currentValue
                )
        );

        plan.setNotes(
                request.getNotes()
        );

        SoilRecoveryPlan updatedPlan =
                soilRecoveryPlanRepository.save(plan);

        return toResponse(updatedPlan);
    }

    /*
     * Delete recovery plan.
     */
    public void deletePlan(
            UUID planId
    ) {

        if (!soilRecoveryPlanRepository.existsById(planId)) {

            throw new RuntimeException(
                    "Soil recovery plan not found with ID: "
                            + planId
            );
        }

        soilRecoveryPlanRepository.deleteById(
                planId
        );
    }

    /*
     * Derive current value from the latest available
     * measured/calculated data.
     */
    private Double deriveCurrentValue(
            String focusArea,
            SoilSample latestSoil,
            NutrientBalance latestBalance
    ) {

        if (focusArea == null) {
            return null;
        }

        switch (focusArea.toUpperCase()) {

            case "ORGANIC_CARBON":
                return latestSoil != null
                        ? latestSoil.getOrganicCarbon()
                        : null;

            case "SOIL_PH":
                return latestSoil != null
                        ? latestSoil.getPH()
                        : null;

            case "NITROGEN_BALANCE":
                return latestBalance != null
                        ? latestBalance.getNitrogenBalance()
                        : null;

            case "PHOSPHORUS_BALANCE":
                return latestBalance != null
                        ? latestBalance.getPhosphorusBalance()
                        : null;

            case "POTASSIUM_BALANCE":
                return latestBalance != null
                        ? latestBalance.getPotassiumBalance()
                        : null;

            default:
                return null;
        }
    }

    /*
     * Determine trend when baseline/current/target
     * information is available.
     */
    private String determineTrend(
            Double baselineValue,
            Double currentValue,
            Double targetValue
    ) {

        if (baselineValue == null
                || currentValue == null) {

            return "INSUFFICIENT_DATA";
        }

        double tolerance =
                Math.max(
                        Math.abs(baselineValue) * 0.05,
                        0.000001
                );

        if (currentValue > baselineValue + tolerance) {

            return "IMPROVING";
        }

        if (currentValue < baselineValue - tolerance) {

            return "DECLINING";
        }

        return "STABLE";
    }

    /*
     * Determine a conservative management action.
     */
    private String determineAction(
            String focusArea,
            String trend
    ) {

        if ("INSUFFICIENT_DATA".equalsIgnoreCase(trend)) {

            return "SOIL_TEST";
        }

        if ("DECLINING".equalsIgnoreCase(trend)) {

            if (focusArea != null
                    && focusArea.equalsIgnoreCase(
                    "NUTRIENT_BALANCE"
            )) {

                return "NUTRIENT_REBALANCING";
            }

            if (focusArea != null
                    && focusArea.equalsIgnoreCase(
                    "ORGANIC_CARBON"
            )) {

                return "RESIDUE_RETENTION";
            }

            return "CROP_ROTATION";
        }

        if ("STABLE".equalsIgnoreCase(trend)) {

            return "MONITOR";
        }

        return "MAINTAIN_CURRENT_PRACTICE";
    }

    /*
     * Build a farmer-facing recommendation.
     */
    private String buildRecommendation(
            String focusArea,
            String trend,
            String actionType
    ) {

        if ("INSUFFICIENT_DATA".equalsIgnoreCase(trend)) {

            return "Obtain or update the relevant soil measurement "
                    + "before making a stronger recovery decision.";
        }

        if ("DECLINING".equalsIgnoreCase(trend)) {

            if ("RESIDUE_RETENTION".equalsIgnoreCase(
                    actionType
            )) {

                return "Consider retaining suitable crop residue "
                        + "and review soil-management practices.";
            }

            if ("NUTRIENT_REBALANCING".equalsIgnoreCase(
                    actionType
            )) {

                return "Review the nutrient budget and avoid "
                        + "unnecessary repeated nutrient application.";
            }

            if ("CROP_ROTATION".equalsIgnoreCase(
                    actionType
            )) {

                return "Consider a suitable crop rotation and "
                        + "review the field's multi-season soil trend.";
            }

            return "Review soil-management practices and update "
                    + "the recovery plan based on validated guidance.";
        }

        if ("IMPROVING".equalsIgnoreCase(trend)) {

            return "Continue the current improvement strategy "
                    + "and monitor the soil indicator over future seasons.";
        }

        return "Continue monitoring the selected soil-health "
                + "indicator and reassess at the next planned review.";
    }

    /*
     * Build an explanation for the farmer.
     */
    private String buildReason(
            String focusArea,
            Double baseline,
            Double current,
            Double target,
            String trend
    ) {

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                "Recovery focus: "
        );

        reason.append(
                focusArea != null
                        ? focusArea
                        : "not specified"
        );

        reason.append(". ");

        if (baseline != null) {

            reason.append(
                    "Baseline value: "
            );

            reason.append(
                    baseline
            );

            reason.append(". ");
        }

        if (current != null) {

            reason.append(
                    "Current value: "
            );

            reason.append(
                    current
            );

            reason.append(". ");
        }

        if (target != null) {

            reason.append(
                    "Target value: "
            );

            reason.append(
                    target
            );

            reason.append(". ");
        }

        reason.append(
                "Trend: "
        );

        reason.append(
                trend
        );

        reason.append(".");

        return reason.toString();
    }

    /*
     * Data provenance.
     */
    private String determineDataStatus(
            SoilRecoveryPlanRequest request,
            SoilSample latestSoil,
            NutrientBalance latestBalance
    ) {

        boolean hasMeasuredSoil =
                latestSoil != null;

        boolean hasCalculatedBalance =
                latestBalance != null;

        boolean hasExplicitValues =
                request.getCurrentValue() != null
                        || request.getBaselineValue() != null;

        if (hasMeasuredSoil
                && hasCalculatedBalance
                && hasExplicitValues) {

            return "MIXED";
        }

        if (hasMeasuredSoil
                || hasExplicitValues) {

            return "MEASURED";
        }

        if (hasCalculatedBalance) {

            return "CALCULATED";
        }

        return "ESTIMATED";
    }

    /*
     * Explain uncertainty.
     */
    private String buildUncertaintyNote(
            SoilRecoveryPlanRequest request,
            Double currentValue
    ) {

        if (currentValue == null) {

            return "Current soil indicator could not be derived "
                    + "from available records. Additional measurement "
                    + "is required before making a stronger recovery claim.";
        }

        if (request.getTargetValue() == null) {

            return "No explicit recovery target was supplied. "
                    + "The plan should be reviewed against validated "
                    + "crop- and soil-specific guidance.";
        }

        return "Recovery values describe the selected indicator "
                + "and should be interpreted using measured data, "
                + "validated agronomic guidance and multi-season trends.";
    }

    /*
     * Convert entity to response.
     */
    private SoilRecoveryPlanResponse toResponse(
            SoilRecoveryPlan plan
    ) {

        return SoilRecoveryPlanResponse.builder()

                .id(
                        plan.getId()
                )

                .farmId(
                        plan.getFarm() != null
                                ? plan.getFarm().getId()
                                : null
                )

                .cropId(
                        plan.getCrop() != null
                                ? plan.getCrop().getId()
                                : null
                )

                .cropName(
                        plan.getCrop() != null
                                ? plan.getCrop().getName()
                                : null
                )

                .seasonNumber(
                        plan.getSeasonNumber()
                )

                .season(
                        plan.getSeason()
                )

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

                .targetValue(
                        plan.getTargetValue()
                )

                .currentValue(
                        plan.getCurrentValue()
                )

                .unit(
                        plan.getUnit()
                )

                .trend(
                        plan.getTrend()
                )

                .actionType(
                        plan.getActionType()
                )

                .recommendation(
                        plan.getRecommendation()
                )

                .reason(
                        plan.getReason()
                )

                .status(
                        plan.getStatus()
                )

                .dataStatus(
                        plan.getDataStatus()
                )

                .source(
                        plan.getSource()
                )

                .uncertaintyNote(
                        plan.getUncertaintyNote()
                )

                .notes(
                        plan.getNotes()
                )

                .build();
    }

    /*
     * Validate request.
     */
    private void validateRequest(
            SoilRecoveryPlanRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }

        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getFocusArea() == null
                || request.getFocusArea().isBlank()) {

            throw new RuntimeException(
                    "Focus area is required"
            );
        }

        if (request.getSeasonNumber() != null
                && request.getSeasonNumber() <= 0) {

            throw new RuntimeException(
                    "Season number must be greater than zero"
            );
        }

        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate()
                .isAfter(request.getEndDate())) {

            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        if (request.getBaselineValue() != null
                && request.getTargetValue() != null
                && request.getUnit() == null
                || request.getUnit() != null
                && request.getUnit().isBlank()) {

            throw new RuntimeException(
                    "Unit is required when baseline and target values are supplied"
            );
        }
    }

    /*
     * Verify farm exists.
     */
    private void verifyFarmExists(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    /*
     * Verify crop exists.
     */
    private void verifyCropExists(
            UUID cropId
    ) {

        if (!cropRepository.existsById(cropId)) {

            throw new RuntimeException(
                    "Crop not found with ID: "
                            + cropId
            );
        }
    }
}