package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farm_sustainability_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmSustainabilityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm being evaluated.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop considered during the assessment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Final sustainability score.
     *
     * Range:
     * 0 - 100
     */
    @Column(nullable = false)
    private Double overallScore;

    /*
     * Score components.
     *
     * Each component is normalized to 0 - 100.
     */
    @Column(nullable = false)
    private Double soilScore;

    @Column(nullable = false)
    private Double waterScore;

    @Column(nullable = false)
    private Double energyScore;

    @Column(nullable = false)
    private Double cropPracticeScore;

    @Column(nullable = false)
    private Double fieldPracticeScore;

    /*
     * Human-readable overall classification.
     *
     * EXCELLENT
     * GOOD
     * MODERATE
     * NEEDS_IMPROVEMENT
     * CRITICAL
     */
    @Column(nullable = false, length = 40)
    private String sustainabilityLevel;

    /*
     * Soil explanation.
     */
    @Column(length = 5000)
    private String soilExplanation;

    /*
     * Water explanation.
     */
    @Column(length = 5000)
    private String waterExplanation;

    /*
     * Energy explanation.
     */
    @Column(length = 5000)
    private String energyExplanation;

    /*
     * Crop-practice explanation.
     */
    @Column(length = 5000)
    private String cropPracticeExplanation;

    /*
     * Field-practice explanation.
     */
    @Column(length = 5000)
    private String fieldPracticeExplanation;

    /*
     * Recommended next action.
     */
    @Column(length = 5000)
    private String recommendation;

    /*
     * Data sources used to calculate the score.
     *
     * Example:
     * SoilSample, IrrigationTelemetry, CropHistory,
     * SatelliteObservation, FertilizerRecommendation
     */
    @Column(length = 5000)
    private String dataUsed;

    /*
     * Provenance.
     *
     * CALCULATED
     * TEST
     * MIXED
     */
    @Column(nullable = false, length = 30)
    private String dataStatus;

    /*
     * Calculation methodology/version.
     *
     * Useful for scientist auditability when
     * scoring rules change in future versions.
     */
    @Column(length = 100)
    private String methodologyVersion;

    /*
     * Overall confidence in the score.
     *
     * Range:
     * 0.0 - 1.0
     */
    @Column(nullable = false)
    private Double confidence;

    /*
     * Important limitations.
     */
    @Column(length = 5000)
    private String uncertaintyNote;

    /*
     * Time at which score was calculated.
     */
    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    /*
     * Optional validity period.
     *
     * Sustainability score becomes stale when
     * farm conditions materially change.
     */
    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {

        if (this.evaluatedAt == null) {

            this.evaluatedAt =
                    LocalDateTime.now();
        }

        if (this.dataStatus == null) {

            this.dataStatus =
                    "CALCULATED";
        }

        if (this.confidence == null) {

            this.confidence =
                    0.70;
        }
    }
}