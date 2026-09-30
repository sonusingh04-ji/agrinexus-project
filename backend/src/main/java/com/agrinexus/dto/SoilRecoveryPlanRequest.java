package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilRecoveryPlanRequest {

    /*
     * Farm for which the recovery plan is being created.
     */
    private UUID farmId;

    /*
     * Optional crop associated with this recovery phase.
     */
    private UUID cropId;

    /*
     * Recovery season/sequence.
     *
     * Example:
     * 1 = first season
     * 2 = second season
     * 3 = third season
     */
    private Integer seasonNumber;

    /*
     * Human-readable season.
     *
     * Examples:
     * Kharif 2026
     * Rabi 2026-27
     */
    private String season;

    /*
     * Recovery phase dates.
     */
    private LocalDate startDate;

    private LocalDate endDate;

    /*
     * Main soil-health indicator being tracked.
     *
     * Examples:
     * ORGANIC_CARBON
     * NITROGEN_BALANCE
     * PHOSPHORUS_BALANCE
     * POTASSIUM_BALANCE
     * SOIL_PH
     * NUTRIENT_BALANCE
     * GENERAL_SOIL_HEALTH
     */
    private String focusArea;

    /*
     * Baseline measurement/value.
     */
    private Double baselineValue;

    /*
     * Desired target value.
     */
    private Double targetValue;

    /*
     * Unit of the tracked indicator.
     *
     * Examples:
     * %
     * mg/kg
     * pH
     * kg/ha
     */
    private String unit;

    /*
     * Latest/current value.
     *
     * This can be omitted when there is not yet
     * enough measured information.
     */
    private Double currentValue;

    /*
     * Trend.
     *
     * Examples:
     * IMPROVING
     * STABLE
     * DECLINING
     * INSUFFICIENT_DATA
     */
    private String trend;

    /*
     * Management action.
     *
     * Examples:
     * CROP_ROTATION
     * RESIDUE_RETENTION
     * ORGANIC_INPUT
     * NUTRIENT_REBALANCING
     * SOIL_TEST
     * REDUCE_UNNECESSARY_INPUT
     */
    private String actionType;

    /*
     * Farmer-facing recommendation.
     */
    private String recommendation;

    /*
     * Explanation for the recommendation.
     */
    private String reason;

    /*
     * Plan status.
     *
     * Examples:
     * PLANNED
     * ACTIVE
     * COMPLETED
     * REVIEW_REQUIRED
     * PAUSED
     */
    private String status;

    /*
     * Data provenance.
     *
     * Examples:
     * MEASURED
     * CALCULATED
     * ESTIMATED
     * MIXED
     */
    private String dataStatus;

    /*
     * Source of agronomic guidance/data.
     */
    private String source;

    /*
     * Assumptions and uncertainty.
     */
    private String uncertaintyNote;

    /*
     * Additional notes.
     */
    private String notes;
}