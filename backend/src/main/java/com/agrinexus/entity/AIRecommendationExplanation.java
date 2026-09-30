package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendation_explanations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendationExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm associated with the recommendation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * AI conversation in which the recommendation was explained.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AIConversation conversation;

    /*
     * Context snapshot used to generate the explanation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_snapshot_id")
    private AIContextSnapshot contextSnapshot;

    /*
     * Source decision type.
     *
     * IRRIGATION
     * CROP
     * FERTILIZER
     * TILLAGE
     * SOWING
     * WEATHER_RISK
     * SOIL
     */
    @Column(length = 50)
    private String decisionType;

    /*
     * Decision produced by the underlying decision engine.
     */
    @Column(length = 200)
    private String decision;

    /*
     * Main farmer-facing explanation.
     */
    @Column(length = 5000)
    private String why;

    /*
     * Important contributing factors.
     */
    @Column(length = 8000)
    private String contributingFactors;

    /*
     * Data used by the explanation.
     */
    @Column(length = 10000)
    private String dataUsed;

    /*
     * Confidence score.
     *
     * 0.0 - 1.0
     */
    private Double confidence;

    /*
     * Uncertainty statement.
     */
    @Column(length = 5000)
    private String uncertainty;

    /*
     * Provenance status.
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
     * Source of the underlying decision.
     *
     * MEMBER_1
     * MEMBER_2
     * MEMBER_3
     * MEMBER_4
     * CENTRAL_DECISION_ENGINE
     */
    @Column(length = 50)
    private String source;

    /*
     * Timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}