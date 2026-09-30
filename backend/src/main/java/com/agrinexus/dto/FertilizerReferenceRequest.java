package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerReferenceRequest {

    /*
     * Crop to which this reference applies.
     */
    private UUID cropId;

    /*
     * State / district / region.
     *
     * Examples:
     * Uttar Pradesh
     * Mathura
     * All India
     */
    private String region;

    /*
     * Optional crop variety or production condition.
     */
    private String varietyOrCondition;

    /*
     * Crop growth/application stage.
     *
     * Examples:
     * BASAL
     * PRE_SOWING
     * SOWING
     * VEGETATIVE
     * FLOWERING
     * TOP_DRESSING
     */
    private String applicationStage;

    /*
     * Reference nutrient values supplied by the source.
     *
     * IMPORTANT:
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
    private String unit;

    /*
     * Organization responsible for the reference.
     *
     * Examples:
     * ICAR
     * ICAR-IARI
     * State Agricultural University
     */
    private String sourceOrganization;

    /*
     * Name of the source document/advisory.
     */
    private String sourceDocument;

    /*
     * Official source URL.
     */
    private String sourceUrl;

    /*
     * Date on which the reference was verified.
     */
    private LocalDate verifiedDate;

    /*
     * Evidence/data status.
     *
     * Recommended lifecycle:
     *
     * REVIEW_REQUIRED
     * VERIFIED
     * RETIRED
     */
    private String dataStatus;

    /*
     * Additional provenance / interpretation notes.
     */
    private String notes;
}