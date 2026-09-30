package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerScheduleRequest {

    /*
     * Farm for which the schedule is required.
     */
    private UUID farmId;

    /*
     * Crop associated with the schedule.
     */
    private UUID cropId;

    /*
     * Optional existing fertilizer recommendation.
     */
    private UUID recommendationId;

    /*
     * Crop/application stage.
     *
     * Examples:
     * PRE_SOWING
     * BASAL
     * VEGETATIVE
     * TILLERING
     * FLOWERING
     * GRAIN_FILLING
     * REASSESSMENT
     */
    private String stage;

    /*
     * Planned date.
     */
    private LocalDate plannedDate;

    /*
     * Scheduled activity.
     *
     * Examples:
     * FERTILIZER_APPLICATION
     * REASSESSMENT
     * SOIL_TEST
     * REVIEW
     */
    private String actionType;

    /*
     * Fertilizer/product name.
     */
    private String productName;

    /*
     * Nutrient values associated with the scheduled activity.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Current schedule status.
     *
     * Examples:
     * PLANNED
     * DUE
     * COMPLETED
     * DELAYED
     * CANCELLED
     * REASSESS
     */
    private String status;

    /*
     * Weather condition used while deciding timing.
     */
    private String weatherCondition;

    /*
     * Whether the farmer should reassess conditions
     * before carrying out the activity.
     */
    private Boolean reassessmentRequired;

    /*
     * Source of guidance.
     */
    private String source;

    /*
     * Uncertainty/assumption information.
     */
    private String uncertaintyNote;

    /*
     * Additional notes.
     */
    private String notes;
}