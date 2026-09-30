package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "soil_recovery_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilRecoveryPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm for which the recovery roadmap is created.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop associated with the planned recovery period.
     *
     * Optional because a recovery plan can also
     * cover a broader multi-season period.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Recovery plan sequence / season number.
     *
     * Example:
     * 1 = first recovery season
     * 2 = second recovery season
     */
    private Integer seasonNumber;

    /*
     * Planned season.
     *
     * Examples:
     * Kharif 2026
     * Rabi 2026-27
     */
    @Column(length = 50)
    private String season;

    /*
     * Start and end of this recovery phase.
     */
    private LocalDate startDate;

    private LocalDate endDate;

    /*
     * Main soil-health focus.
     *
     * Examples:
     * ORGANIC_CARBON
     * NITROGEN_BALANCE
     * PHOSPHORUS_BALANCE
     * POTASSIUM_BALANCE
     * SOIL_PH
     * NUTRIENT_BALANCE
     * GENERAL_SOIL_HEALTH
     */
    @Column(length = 100)
    private String focusArea;

    /*
     * Baseline value being tracked.
     */
    private Double baselineValue;

    /*
     * Target value for the selected indicator.
     */
    private Double targetValue;

    /*
     * Unit of the tracked indicator.
     *
     * Examples:
     * %
     * mg/kg
     * pH
     * kg/ha
     */
    @Column(length = 30)
    private String unit;

    /*
     * Latest observed/calculated value.
     */
    private Double currentValue;

    /*
     * Trend classification.
     *
     * Examples:
     * IMPROVING
     * STABLE
     * DECLINING
     * INSUFFICIENT_DATA
     */
    @Column(length = 40)
    private String trend;

    /*
     * Recommended management action.
     *
     * Examples:
     * CROP_ROTATION
     * RESIDUE_RETENTION
     * ORGANIC_INPUT
     * NUTRIENT_REBALANCING
     * SOIL_TEST
     * REDUCE_UNNECESSARY_INPUT
     */
    @Column(length = 100)
    private String actionType;

    /*
     * Farmer-facing action recommendation.
     */
    @Column(length = 3000)
    private String recommendation;

    /*
     * Explanation for the recommendation.
     */
    @Column(length = 3000)
    private String reason;

    /*
     * Current plan status.
     *
     * Examples:
     * PLANNED
     * ACTIVE
     * COMPLETED
     * REVIEW_REQUIRED
     * PAUSED
     */
    @Column(nullable = false, length = 40)
    private String status;

    /*
     * Data provenance.
     *
     * Examples:
     * MEASURED
     * CALCULATED
     * ESTIMATED
     * MIXED
     */
    @Column(length = 30)
    private String dataStatus;

    /*
     * Source of agronomic guidance.
     */
    @Column(length = 300)
    private String source;

    /*
     * Explanation of assumptions/uncertainty.
     */
    @Column(length = 2500)
    private String uncertaintyNote;

    /*
     * Additional notes.
     */
    @Column(length = 3000)
    private String notes;
}