package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fertilizer_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm for which the recommendation was generated.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop for which fertilizer guidance is being generated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Crop growth stage.
     *
     * Examples:
     * SOWING
     * VEGETATIVE
     * TILLERING
     * FLOWERING
     * GRAIN_FILLING
     * HARVEST
     */
    @Column(length = 50)
    private String cropStage;

    /*
     * Field area used for recommendation.
     */
    private Double fieldArea;

    /*
     * Unit of field area.
     *
     * Example:
     * ACRE
     * HECTARE
     */
    @Column(length = 30)
    private String fieldAreaUnit;

    /*
     * Recommended nutrient amounts.
     *
     * These must come from validated guidance or
     * explicitly labeled estimates.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Recommendation status.
     *
     * Examples:
     * READY
     * INSUFFICIENT_DATA
     * REVIEW_REQUIRED
     */
    @Column(length = 40)
    private String status;

    /*
     * Human-readable recommendation.
     */
    @Column(length = 2000)
    private String recommendation;

    /*
     * Explainable reason.
     */
    @Column(length = 3000)
    private String reason;

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
     * Source of agronomic guidance/data.
     */
    @Column(length = 300)
    private String source;

    /*
     * Confidence/uncertainty note.
     */
    @Column(length = 1000)
    private String uncertaintyNote;

    /*
     * Time when recommendation was generated.
     */
    @Column(nullable = false)
    private LocalDateTime generatedAt;

    /*
     * Additional assumptions or notes.
     */
    @Column(length = 3000)
    private String notes;
}