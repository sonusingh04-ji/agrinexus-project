package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmSustainabilityScoreResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    /*
     * Final score: 0 - 100
     */
    private Double overallScore;

    /*
     * Component scores: 0 - 100
     */
    private Double soilScore;

    private Double waterScore;

    private Double energyScore;

    private Double cropPracticeScore;

    private Double fieldPracticeScore;

    /*
     * Overall classification.
     */
    private String sustainabilityLevel;

    /*
     * Component explanations.
     */
    private String soilExplanation;

    private String waterExplanation;

    private String energyExplanation;

    private String cropPracticeExplanation;

    private String fieldPracticeExplanation;

    /*
     * Main recommendation.
     */
    private String recommendation;

    /*
     * Data used for calculation.
     */
    private String dataUsed;

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