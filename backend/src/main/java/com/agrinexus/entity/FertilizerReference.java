package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "fertilizer_references",
        indexes = {
                @Index(name = "idx_fert_ref_crop", columnList = "crop_id"),
                @Index(name = "idx_fert_ref_region", columnList = "region"),
                @Index(name = "idx_fert_ref_status", columnList = "data_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Crop to which this reference applies.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /*
     * State / region / district for which the reference is applicable.
     *
     * Examples:
     * Uttar Pradesh
     * Mathura
     * All India
     */
    @Column(length = 150)
    private String region;

    /*
     * Optional crop variety or production condition.
     */
    @Column(length = 200)
    private String varietyOrCondition;

    /*
     * Crop growth/application stage.
     *
     * Examples:
     * BASAL
     * VEGETATIVE
     * FLOWERING
     * TOP_DRESSING
     */
    @Column(length = 100)
    private String applicationStage;

    /*
     * Reference nutrient requirement.
     *
     * These are reference values, NOT measured soil values.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Original unit supplied by the authoritative source.
     *
     * Example:
     * kg/ha
     */
    @Column(nullable = false, length = 50)
    private String unit;

    /*
     * Organization responsible for the reference.
     *
     * Example:
     * ICAR
     * ICAR-IARI
     * State Agricultural University
     */
    @Column(nullable = false, length = 200)
    private String sourceOrganization;

    /*
     * Name of the source document/advisory.
     */
    @Column(nullable = false, length = 500)
    private String sourceDocument;

    /*
     * Official source URL.
     */
    @Column(length = 1000)
    private String sourceUrl;

    /*
     * Date on which the source/reference was verified.
     */
    private LocalDate verifiedDate;

    /*
     * Evidence/data status.
     *
     * VERIFIED
     * REVIEW_REQUIRED
     * RETIRED
     */
    @Column(nullable = false, length = 50)
    private String dataStatus;

    /*
     * Additional provenance/interpretation notes.
     */
    @Column(length = 2000)
    private String notes;
}