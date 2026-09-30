package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "disease_pest_risks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseasePestRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Example:
     * FUNGAL_DISEASE
     * INSECT_PEST
     * BACTERIAL_DISEASE
     * VIRAL_DISEASE
     * GENERAL_PEST
     */
    @Column(nullable = false, length = 60)
    private String riskCategory;

    /*
     * Human-readable risk name.
     *
     * Example:
     * "Fungal disease risk"
     * "Aphid risk"
     */
    @Column(nullable = false, length = 200)
    private String riskName;

    /*
     * LOW
     * MEDIUM
     * HIGH
     */
    @Column(nullable = false, length = 30)
    private String riskLevel;

    /*
     * 0–100
     */
    @Column(nullable = false)
    private Double riskScore;

    /*
     * Why the risk was raised.
     */
    @Column(length = 5000)
    private String reason;

    /*
     * Practical farmer-facing action.
     *
     * Example:
     * "Inspect leaves and stems for early symptoms."
     */
    @Column(length = 5000)
    private String recommendation;

    /*
     * Scouting action.
     */
    @Column(length = 5000)
    private String scoutingAdvice;

    /*
     * Data used for this assessment.
     */
    @Column(length = 5000)
    private String contributingFactors;

    /*
     * Source/provenance.
     *
     * Example:
     * RULE_ENGINE
     * ICAR_NPSS
     * NIPHM_IPM
     * TEST
     */
    @Column(length = 100)
    private String source;

    /*
     * OFFICIAL
     * CALCULATED
     * TEST
     * MIXED
     */
    @Column(length = 50)
    private String dataStatus;

    /*
     * Limitations / uncertainty.
     */
    @Column(length = 5000)
    private String uncertaintyNote;

    /*
     * Whether this result is based on
     * current or forecast conditions.
     */
    @Column(nullable = false)
    private Boolean forecastBased = false;

    private LocalDateTime evaluatedAt;

    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {

        if (this.evaluatedAt == null) {
            this.evaluatedAt = LocalDateTime.now();
        }

        if (this.forecastBased == null) {
            this.forecastBased = false;
        }
    }
}