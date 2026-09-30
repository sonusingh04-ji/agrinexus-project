package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerRecommendationResponse {

    /*
     * Unique recommendation ID.
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
     * Current crop stage.
     */
    private String cropStage;

    /*
     * Field area used for the recommendation.
     */
    private Double fieldArea;

    private String fieldAreaUnit;

    /*
     * Recommended nutrient amounts.
     *
     * These are recommendation outputs/ranges,
     * not guaranteed prescriptions.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Recommendation status.
     */
    private String status;

    /*
     * Main recommendation shown to farmer.
     */
    private String recommendation;

    /*
     * Explainable reasoning.
     */
    private String reason;

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
     * Agronomic/source reference.
     */
    private String source;

    /*
     * Important when some required information
     * is unavailable.
     */
    private String uncertaintyNote;

    /*
     * Time when recommendation was generated.
     */
    private LocalDateTime generatedAt;

    /*
     * Additional assumptions / farmer notes.
     */
    private String notes;
}