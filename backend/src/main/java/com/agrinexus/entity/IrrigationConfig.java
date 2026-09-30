package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "irrigation_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm for which these irrigation parameters apply.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false, unique = true)
    private Farm farm;

    /*
     * ---------------------------------------------------------
     * Soil water parameters
     * ---------------------------------------------------------
     *
     * Stored as percentages of volumetric water content (VWC)
     * for the MVP unless the unit explicitly says otherwise.
     */

    /*
     * Field Capacity.
     */
    private Double fieldCapacity;

    /*
     * Permanent Wilting Point.
     */
    private Double wiltingPoint;

    /*
     * Maximum Allowable Depletion.
     *
     * Example:
     * 0.50 = 50%
     */
    private Double maxAllowableDepletion;

    /*
     * Readily Available Water fraction.
     *
     * Example:
     * 0.50 = 50%
     *
     * This can remain null when not available.
     */
    private Double readilyAvailableWater;

    /*
     * ---------------------------------------------------------
     * Root-zone information
     * ---------------------------------------------------------
     */

    /*
     * Effective root-zone depth in centimetres.
     */
    private Double rootZoneDepthCm;

    /*
     * ---------------------------------------------------------
     * Irrigation target
     * ---------------------------------------------------------
     */

    /*
     * Fraction of field capacity used as the completion target.
     *
     * Member 2 specification uses 95% of FC.
     *
     * Example:
     * 0.95
     */
    private Double targetFieldCapacityFraction;

    /*
     * ---------------------------------------------------------
     * Rainfall / forecast configuration
     * ---------------------------------------------------------
     */

    /*
     * Number of forecast days used by the decision engine.
     *
     * Valid practical values:
     * 1–7
     */
    private Integer forecastHorizonDays;

    /*
     * Minimum forecast confidence considered suitable
     * for automatic decision support.
     *
     * Example:
     * 0.70 = 70%
     */
    private Double minimumForecastConfidence;

    /*
     * ---------------------------------------------------------
     * Crop-water calculation configuration
     * ---------------------------------------------------------
     */

    /*
     * ET calculation method.
     *
     * Examples:
     * PENMAN_MONTEITH
     * SIMPLIFIED
     * API_PROVIDED
     */
    @Column(length = 50)
    private String et0Method;

    /*
     * Whether the farm uses sensor measurements
     * as the primary soil-moisture source.
     */
    private Boolean sensorBased;

    /*
     * ---------------------------------------------------------
     * Data provenance
     * ---------------------------------------------------------
     */

    /*
     * Examples:
     * SOIL_LAB
     * FARMER_ENTERED
     * REGIONAL_DEFAULT
     * RESEARCH_REFERENCE
     */
    @Column(length = 50)
    private String source;

    /*
     * MEASURED
     * ESTIMATED
     * DEFAULT
     * MIXED
     */
    @Column(length = 30)
    private String dataStatus;

    /*
     * Explain why these values were selected.
     */
    @Column(length = 2500)
    private String notes;
}