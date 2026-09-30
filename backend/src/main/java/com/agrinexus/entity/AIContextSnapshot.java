package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_context_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIContextSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Conversation associated with this context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AIConversation conversation;

    /*
     * Message for which this context was assembled.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private AIMessage message;

    /*
     * Farm being analyzed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

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
     * Member 4 — Disease/Pest Risk record.
     */
    private UUID diseasePestRiskId;

    /*
     * Member 4 — Satellite / Remote-Sensing observation.
     */
    private UUID satelliteObservationId;

    /*
     * Decision-engine outputs.
     *
     * These are snapshots so the AI preserves the
     * information exactly as it existed at capture time.
     */
    @Column(length = 5000)
    private String decisionSummary;

    @Column(length = 5000)
    private String irrigationDecision;

    @Column(length = 5000)
    private String cropDecision;

    @Column(length = 5000)
    private String nutrientDecision;

    @Column(length = 5000)
    private String weatherRiskDecision;

    /*
     * Member 4 — Disease/Pest decision.
     */
    @Column(length = 5000)
    private String diseasePestRiskDecision;

    /*
     * Member 4 — Satellite Monitoring decision.
     */
    @Column(length = 5000)
    private String satelliteDecision;

    /*
     * Field operation decision.
     */
    @Column(length = 5000)
    private String fieldOperationDecision;

    /*
     * =========================================================
     * Member 6 — Long-Term Field Intelligence
     * =========================================================
     */

    /*
     * Field Health Passport summary.
     */
    @Column(length = 5000)
    private String fieldHealthPassportDecision;

    /*
     * Soil Test Trend / Reminder summary.
     */
    @Column(length = 5000)
    private String soilTestTrendDecision;

    /*
     * Lowest-Cost Safe Plan summary.
     */
    @Column(length = 5000)
    private String lowestCostPlanDecision;

    /*
     * Village Cooperative summary.
     */
    @Column(length = 5000)
    private String cooperativeDecision;

    /*
     * Complete farm context.
     */
    @Column(length = 10000)
    private String farmContext;

    /*
     * Uncertainty and provenance context.
     */
    @Column(length = 5000)
    private String uncertaintyContext;

    /*
     * Context provenance.
     *
     * MEASURED
     * FORECAST
     * CALCULATED
     * ESTIMATED
     * TEST
     * MIXED
     */
    @Column(length = 30)
    private String dataStatus;

    /*
     * Timestamp when the context was captured.
     */
    @Column(nullable = false)
    private LocalDateTime capturedAt;
}