package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerScheduleResponse {

    /*
     * Unique schedule ID.
     */
    private UUID id;

    /*
     * Farm.
     */
    private UUID farmId;

    /*
     * Crop.
     */
    private UUID cropId;

    private String cropName;

    /*
     * Linked recommendation, when available.
     */
    private UUID recommendationId;

    /*
     * Crop/application stage.
     */
    private String stage;

    /*
     * Planned date.
     */
    private LocalDate plannedDate;

    /*
     * Scheduled activity.
     */
    private String actionType;

    /*
     * Fertilizer/product.
     */
    private String productName;

    /*
     * Nutrient amounts associated with this
     * scheduled activity.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Current schedule status.
     */
    private String status;

    /*
     * Weather context.
     */
    private String weatherCondition;

    /*
     * Whether reassessment is needed.
     */
    private Boolean reassessmentRequired;

    /*
     * Source/provenance.
     */
    private String source;

    /*
     * Data status.
     */
    private String dataStatus;

    /*
     * Why this activity is scheduled.
     */
    private String reason;

    /*
     * Farmer-facing recommendation.
     */
    private String recommendation;

    /*
     * Uncertainty/assumption note.
     */
    private String uncertaintyNote;

    /*
     * Additional notes.
     */
    private String notes;
}