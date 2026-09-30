package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crop_rotation_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRotationDecision {

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
     * Previous crop used as the rotation starting point.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_crop_id")
    private Crop previousCrop;

    /*
     * Recommended top-ranked next crop.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_crop_id")
    private Crop recommendedCrop;

    /*
     * Season considered for the next crop.
     */
    @Column(length = 50)
    private String targetSeason;

    /*
     * Highest score among ranked candidates.
     */
    private Double recommendationScore;

    /*
     * Human-readable reason for the recommendation.
     */
    @Column(length = 5000)
    private String reason;

    /*
     * Explanation of the main factors used.
     */
    @Column(length = 5000)
    private String contributingFactors;

    /*
     * Ranked candidate crops.
     *
     * Stored as a serialized text snapshot for auditability.
     *
     * Example:
     * Wheat=82.5; Chickpea=78.0; Mustard=71.5
     */
    @Column(length = 10000)
    private String rankedCandidates;

    /*
     * Soil information used by the engine.
     */
    private Double soilPh;

    private Double soilNitrogen;

    private Double soilPhosphorus;

    private Double soilPotassium;

    /*
     * Current/available water context.
     */
    private Double currentMoisture;

    private Double recentRainfall;

    /*
     * Farmer constraint supplied by the caller.
     *
     * Example:
     * LOW_WATER
     * LOW_INPUT_COST
     * SHORT_DURATION
     * NONE
     */
    @Column(length = 100)
    private String farmerConstraint;

    /*
     * Data provenance.
     *
     * CALCULATED
     * TEST
     * MIXED
     */
    @Column(nullable = false, length = 30)
    private String dataStatus;

    /*
     * Methodology version.
     */
    @Column(nullable = false, length = 100)
    private String methodologyVersion;

    /*
     * Confidence in the ranking.
     */
    @Column(nullable = false)
    private Double confidence;

    /*
     * Uncertainty and limitations.
     */
    @Column(length = 5000)
    private String uncertaintyNote;

    /*
     * Evaluation timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    /*
     * Optional validity.
     */
    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {

        if (this.evaluatedAt == null) {
            this.evaluatedAt = LocalDateTime.now();
        }

        if (this.dataStatus == null) {
            this.dataStatus = "CALCULATED";
        }

        if (this.methodologyVersion == null) {
            this.methodologyVersion =
                    "AGRINEXUS-ROTATION-V1";
        }

        if (this.confidence == null) {
            this.confidence = 0.70;
        }
    }
}