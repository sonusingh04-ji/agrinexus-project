package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farm_decision_simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmDecisionSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm being simulated.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop currently associated with the simulation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Scenario selected by the caller.
     *
     * Examples:
     *
     * IRRIGATE_NOW
     * WAIT_FOR_RAIN
     * DELAY_OPERATION
     */
    @Column(nullable = false, length = 50)
    private String scenario;

    /*
     * Alternative scenario used for comparison.
     */
    @Column(nullable = false, length = 50)
    private String comparisonScenario;

    /*
     * Simulation conclusion.
     *
     * SCENARIO_A_FAVORED
     * SCENARIO_B_FAVORED
     * TOO_CLOSE_TO_CALL
     * INSUFFICIENT_DATA
     */
    @Column(nullable = false, length = 50)
    private String conclusion;

    /*
     * Current soil moisture / VWC.
     */
    private Double currentMoisture;

    /*
     * Forecast rainfall considered by the simulation.
     */
    private Double forecastRainfall;

    /*
     * Forecast confidence.
     */
    private Double forecastConfidence;

    /*
     * Estimated water difference between scenarios.
     *
     * Unit should match the water model used by the
     * simulation engine.
     *
     * This field must not be populated with invented values.
     */
    private Double waterDifference;

    /*
     * Estimated energy/cost difference.
     *
     * Optional because actual pump power, duration and
     * tariff/fuel information may not always be available.
     */
    private Double energyCostDifference;

    /*
     * Human-readable timing comparison.
     */
    @Column(length = 2000)
    private String timingDifference;

    /*
     * Scenario A explanation.
     */
    @Column(length = 5000)
    private String scenarioAExplanation;

    /*
     * Scenario B explanation.
     */
    @Column(length = 5000)
    private String scenarioBExplanation;

    /*
     * Main recommendation.
     */
    @Column(length = 5000)
    private String recommendation;

    /*
     * Key assumptions used by the simulation.
     */
    @Column(length = 8000)
    private String assumptions;

    /*
     * Data sources used.
     */
    @Column(length = 5000)
    private String dataUsed;

    /*
     * Data provenance.
     *
     * TEST
     * CALCULATED
     * MIXED
     */
    @Column(nullable = false, length = 30)
    private String dataStatus;

    /*
     * Simulation methodology version.
     */
    @Column(nullable = false, length = 100)
    private String methodologyVersion;

    /*
     * Confidence in the simulation comparison.
     */
    @Column(nullable = false)
    private Double confidence;

    /*
     * Uncertainty / limitations.
     */
    @Column(length = 8000)
    private String uncertaintyNote;

    /*
     * Simulation timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime simulatedAt;

    /*
     * Optional validity.
     */
    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {

        if (this.simulatedAt == null) {
            this.simulatedAt = LocalDateTime.now();
        }

        if (this.dataStatus == null) {
            this.dataStatus = "CALCULATED";
        }

        if (this.methodologyVersion == null) {
            this.methodologyVersion =
                    "AGRINEXUS-SIMULATION-V1";
        }

        if (this.confidence == null) {
            this.confidence = 0.70;
        }
    }
}