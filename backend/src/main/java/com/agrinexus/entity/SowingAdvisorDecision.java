package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sowing_advisor_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowingAdvisorDecision {

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
     * Crop being evaluated for sowing.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /*
     * Final sowing decision.
     *
     * SOW_NOW
     * WAIT
     * UNSUITABLE
     * INSUFFICIENT_DATA
     */
    @Column(nullable = false, length = 40)
    private String decision;

    /*
     * Recommended sowing window.
     *
     * Example:
     * NOW
     * WAIT_FOR_MOISTURE
     * WAIT_FOR_WEATHER
     * OUTSIDE_CROP_WINDOW
     */
    @Column(length = 100)
    private String recommendedWindow;

    /*
     * Crop season.
     *
     * Example:
     * Rabi
     * Kharif
     * Summer
     */
    @Column(length = 50)
    private String season;

    /*
     * Current soil moisture information used
     * for the decision, when available.
     *
     * This can be VWC or the project's configured
     * moisture representation.
     */
    private Double currentMoisture;

    /*
     * Current temperature.
     */
    private Double currentTemperature;

    /*
     * Current humidity.
     */
    private Integer currentHumidity;

    /*
     * Recent rainfall.
     */
    private Double recentRainfall;

    /*
     * Upcoming forecast rainfall.
     */
    private Double forecastRainfall;

    /*
     * Forecast confidence.
     */
    private Double forecastConfidence;

    /*
     * Crop's configured ideal sowing temperature.
     */
    private Double idealTemperatureMin;

    private Double idealTemperatureMax;

    /*
     * Soil pH observed for the field.
     */
    private Double soilPh;

    /*
     * Crop's suitable soil pH range.
     */
    private Double cropSoilPhMin;

    private Double cropSoilPhMax;

    /*
     * Human-readable reason.
     */
    @Column(length = 5000)
    private String reason;

    /*
     * Factors that contributed to the decision.
     */
    @Column(length = 5000)
    private String contributingFactors;

    /*
     * Farmer-facing action.
     */
    @Column(length = 5000)
    private String recommendation;

    /*
     * Data used.
     */
    @Column(length = 5000)
    private String dataUsed;

    /*
     * Data provenance.
     *
     * CALCULATED
     * TEST
     * MIXED
     * OFFICIAL
     */
    @Column(nullable = false, length = 30)
    private String dataStatus;

    /*
     * Rule/model version for auditability.
     */
    @Column(nullable = false, length = 100)
    private String methodologyVersion;

    /*
     * Decision confidence.
     *
     * 0.0 - 1.0
     */
    @Column(nullable = false)
    private Double confidence;

    /*
     * Limitations / uncertainty.
     */
    @Column(length = 5000)
    private String uncertaintyNote;

    /*
     * When the decision was evaluated.
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

            this.evaluatedAt =
                    LocalDateTime.now();
        }

        if (this.dataStatus == null) {

            this.dataStatus =
                    "CALCULATED";
        }

        if (this.methodologyVersion == null) {

            this.methodologyVersion =
                    "AGRINEXUS-SOWING-V1";
        }

        if (this.confidence == null) {

            this.confidence =
                    0.70;
        }
    }
}