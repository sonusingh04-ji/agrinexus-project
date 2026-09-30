package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilRecoveryPlanResponse {

    /*
     * Unique recovery-plan ID.
     */
    private UUID id;

    /*
     * Farm.
     */
    private UUID farmId;

    /*
     * Optional crop associated with the recovery phase.
     */
    private UUID cropId;

    private String cropName;

    /*
     * Recovery season/sequence.
     */
    private Integer seasonNumber;

    /*
     * Human-readable season.
     */
    private String season;

    /*
     * Recovery phase dates.
     */
    private LocalDate startDate;

    private LocalDate endDate;

    /*
     * Soil-health indicator being tracked.
     */
    private String focusArea;

    /*
     * Baseline → current → target.
     */
    private Double baselineValue;

    private Double targetValue;

    private Double currentValue;

    private String unit;

    /*
     * Current trend.
     */
    private String trend;

    /*
     * Recommended management action.
     */
    private String actionType;

    /*
     * Farmer-facing recommendation.
     */
    private String recommendation;

    /*
     * Explainable reason.
     */
    private String reason;

    /*
     * Recovery-plan status.
     */
    private String status;

    /*
     * Data provenance.
     */
    private String dataStatus;

    /*
     * Source/reference.
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