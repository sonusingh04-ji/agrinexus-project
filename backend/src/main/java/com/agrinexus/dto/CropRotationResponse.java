package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRotationResponse {

    /*
     * Rotation decision ID.
     */
    private UUID id;

    /*
     * Farm.
     */
    private UUID farmId;

    /*
     * Previous crop.
     */
    private UUID previousCropId;

    private String previousCropName;

    /*
     * Recommended next crop.
     */
    private UUID recommendedCropId;

    private String recommendedCropName;

    /*
     * Target season.
     */
    private String targetSeason;

    /*
     * Recommendation score.
     *
     * Range:
     * 0 - 100
     */
    private Double recommendationScore;

    /*
     * Main explanation.
     */
    private String reason;

    /*
     * Factors contributing to ranking.
     */
    private String contributingFactors;

    /*
     * Ranked candidates.
     */
    private String rankedCandidates;

    /*
     * Soil information.
     */
    private Double soilPh;

    private Double soilNitrogen;

    private Double soilPhosphorus;

    private Double soilPotassium;

    /*
     * Water context.
     */
    private Double currentMoisture;

    private Double recentRainfall;

    /*
     * Farmer constraint.
     */
    private String farmerConstraint;

    /*
     * Provenance.
     */
    private String dataStatus;

    /*
     * Methodology version.
     */
    private String methodologyVersion;

    /*
     * Confidence.
     */
    private Double confidence;

    /*
     * Uncertainty.
     */
    private String uncertaintyNote;

    /*
     * Evaluation timestamp.
     */
    private LocalDateTime evaluatedAt;

    /*
     * Validity period.
     */
    private LocalDateTime validUntil;
}