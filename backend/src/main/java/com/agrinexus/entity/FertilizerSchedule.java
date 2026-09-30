package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fertilizer_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm for which the schedule is created.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop associated with the schedule.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /*
     * Optional link to the fertilizer recommendation
     * from which this schedule was generated.
     *
     * IMPORTANT:
     * The Java field is named fertilizerRecommendation
     * to avoid conflict with the String recommendation
     * field below.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private FertilizerRecommendation fertilizerRecommendation;

    /*
     * Growth stage at which this application/reassessment
     * is intended.
     *
     * Examples:
     * PRE_SOWING
     * BASAL
     * VEGETATIVE
     * TILLERING
     * FLOWERING
     * GRAIN_FILLING
     * REASSESSMENT
     */
    @Column(nullable = false, length = 50)
    private String stage;

    /*
     * Planned date for the activity.
     */
    @Column(nullable = false)
    private LocalDate plannedDate;

    /*
     * Type of scheduled activity.
     *
     * Examples:
     * FERTILIZER_APPLICATION
     * REASSESSMENT
     * SOIL_TEST
     * REVIEW
     */
    @Column(nullable = false, length = 50)
    private String actionType;

    /*
     * Product to be used when the action is
     * a fertilizer application.
     */
    @Column(length = 200)
    private String productName;

    /*
     * Planned nutrient amount, when supported
     * by validated guidance.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Data status.
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
     * Current status of the scheduled action.
     *
     * Examples:
     * PLANNED
     * DUE
     * COMPLETED
     * DELAYED
     * CANCELLED
     * REASSESS
     */
    @Column(nullable = false, length = 30)
    private String status;

    /*
     * Weather-related scheduling note.
     */
    @Column(length = 1500)
    private String weatherCondition;

    /*
     * Reason why this date/stage was selected.
     */
    @Column(length = 2500)
    private String reason;

    /*
     * General instruction to the farmer.
     *
     * This is intentionally a String and is separate
     * from the fertilizerRecommendation entity.
     */
    @Column(length = 2500)
    private String recommendation;

    /*
     * Whether reassessment is required before
     * the scheduled application.
     */
    private Boolean reassessmentRequired;

    /*
     * Source of agronomic guidance.
     */
    @Column(length = 300)
    private String source;

    /*
     * Assumptions / uncertainty.
     */
    @Column(length = 2000)
    private String uncertaintyNote;

    /*
     * Additional notes.
     */
    @Column(length = 3000)
    private String notes;
}