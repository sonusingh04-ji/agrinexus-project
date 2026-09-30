package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendationExplanationRequest {

    private UUID farmId;

    private UUID conversationId;

    private UUID contextSnapshotId;

    /*
     * Examples:
     * IRRIGATION
     * CROP
     * FERTILIZER
     * TILLAGE
     * SOWING
     * WEATHER_RISK
     * SOIL
     */
    private String decisionType;

    /*
     * Decision returned by the underlying decision engine.
     */
    private String decision;

    /*
     * Main explanation.
     */
    private String why;

    /*
     * Important factors behind the decision.
     */
    private String contributingFactors;

    /*
     * Data used for the recommendation.
     */
    private String dataUsed;

    /*
     * Confidence:
     * 0.0 - 1.0
     */
    private Double confidence;

    /*
     * Uncertainty / limitations.
     */
    private String uncertainty;

    /*
     * MEASURED
     * FORECAST
     * CALCULATED
     * ESTIMATED
     * TEST
     * MIXED
     */
    private String dataStatus;

    /*
     * MEMBER_1
     * MEMBER_2
     * MEMBER_3
     * MEMBER_4
     * CENTRAL_DECISION_ENGINE
     */
    private String source;
}