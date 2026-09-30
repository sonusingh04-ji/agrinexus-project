package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "satellite_observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatelliteObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm associated with this satellite observation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Optional crop associated with the observation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Geographic coordinates used for the observation.
     */
    private Double latitude;

    private Double longitude;

    /*
     * NDVI - Normalized Difference Vegetation Index.
     */
    private Double ndvi;

    /*
     * NDWI - Normalized Difference Water Index.
     */
    private Double ndwi;

    /*
     * Percentage of cloud cover.
     */
    @Column(name = "cloud_cover_percent")
    private Double cloudCoverPercent;

    /*
     * Percentage change compared with a previous
     * comparable observation.
     */
    @Column(name = "change_percent")
    private Double changePercent;

    /*
     * Interpretation of the change.
     */
    @Column(name = "change_status")
    private String changeStatus;

    /*
     * Current crop condition derived from observation.
     */
    @Column(name = "crop_condition")
    private String cropCondition;

    /*
     * Detailed interpretation of satellite indicators.
     */
    private String interpretation;

    /*
     * Recommendation generated from satellite analysis.
     */
    private String recommendation;

    /*
     * Confidence score associated with the observation.
     */
    private Double confidence;

    /*
     * Actual time at which satellite observation was made.
     */
    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    /*
     * Source of the data.
     *
     * Example:
     * Copernicus Data Space
     */
    @Column(name = "data_source")
    private String dataSource;

    /*
     * Satellite mission/source.
     *
     * Example:
     * Sentinel-2
     */
    @Column(name = "satellite_source")
    private String satelliteSource;

    /*
     * Satellite scene/product identifier.
     */
    @Column(name = "scene_id")
    private String sceneId;

    /*
     * Spatial resolution of the source data in meters.
     */
    @Column(name = "spatial_resolution_meters")
    private Double spatialResolutionMeters;

    /*
     * Current processing/data status.
     *
     * Examples:
     * OBSERVED
     * PROCESSED
     * FAILED
     * UNAVAILABLE
     */
    @Column(name = "data_status")
    private String dataStatus;

    /*
     * Indicates whether the result is based on a forecast
     * rather than an actual satellite observation.
     *
     * For our real satellite module this should normally
     * remain false.
     */
    @Column(name = "forecast_based")
    private Boolean forecastBased;

    /*
     * Explanation of uncertainty or limitations.
     */
    @Column(name = "uncertainty_note")
    private String uncertaintyNote;
}