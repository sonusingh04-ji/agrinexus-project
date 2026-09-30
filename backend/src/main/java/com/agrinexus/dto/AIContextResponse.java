package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIContextResponse {

    private UUID id;

    private UUID farmId;

    private UUID conversationId;

    private UUID messageId;

    /*
     * Source record identifiers.
     */
    private UUID soilSampleId;

    private UUID cropHistoryId;

    private UUID cropId;

    private UUID weatherRecordId;

    private UUID weatherForecastId;

    private UUID irrigationTelemetryId;

    private UUID fertilizerRecommendationId;

    private UUID nutrientBalanceId;

    private UUID weatherRiskId;

    /*
     * Member 4 — Disease/Pest Risk.
     */
    private UUID diseasePestRiskId;

    /*
     * Member 4 — Satellite / Remote-Sensing observation.
     */
    private UUID satelliteObservationId;

    /*
     * Decision-engine outputs.
     */
    private String decisionSummary;

    private String irrigationDecision;

    private String cropDecision;

    private String nutrientDecision;

    private String weatherRiskDecision;

    private String diseasePestRiskDecision;

    private String satelliteDecision;

    private String fieldOperationDecision;

    /*
     * =========================================================
     * Member 6 — Long-Term Field Intelligence
     * =========================================================
     */
    private String fieldHealthPassportDecision;

    private String soilTestTrendDecision;

    private String lowestCostPlanDecision;

    private String cooperativeDecision;

    /*
     * Complete farm context.
     */
    private String farmContext;

    private String uncertaintyContext;

    private String dataStatus;

    private LocalDateTime capturedAt;
}