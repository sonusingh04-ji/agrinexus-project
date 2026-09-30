package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmDecisionSimulationResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    /*
     * Scenario A.
     */
    private String scenario;

    /*
     * Scenario B.
     */
    private String comparisonScenario;

    /*
     * Final comparison result.
     */
    private String conclusion;

    /*
     * Current field context.
     */
    private Double currentMoisture;

    private Double forecastRainfall;

    private Double forecastConfidence;

    /*
     * Scenario water comparison.
     *
     * This is a comparison index in V1, not a claimed
     * litre-saving value.
     */
    private Double waterDifference;

    /*
     * Exact energy/cost difference is nullable when
     * validated cost inputs are unavailable.
     */
    private Double energyCostDifference;

    /*
     * Timing comparison.
     */
    private String timingDifference;

    /*
     * Scenario explanations.
     */
    private String scenarioAExplanation;

    private String scenarioBExplanation;

    /*
     * Final recommendation.
     */
    private String recommendation;

    /*
     * Assumptions used.
     */
    private String assumptions;

    /*
     * Data sources.
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
     * Simulation timestamps.
     */
    private LocalDateTime simulatedAt;

    private LocalDateTime validUntil;
}