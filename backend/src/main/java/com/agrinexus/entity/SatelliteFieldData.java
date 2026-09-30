package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "satellite_field_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatelliteFieldData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm to which this satellite data belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Cloud coverage reported for the satellite observation.
     */
    @Column(name = "cloud_coverage")
    private Double cloudCoverage;

    /*
     * NDVI value derived from satellite imagery.
     */
    private Double ndvi;

    /*
     * Soil moisture indicator derived from available
     * satellite/remote-sensing data.
     */
    @Column(name = "soil_moisture")
    private Double soilMoisture;

    /*
     * Vegetation health score/value.
     */
    @Column(name = "vegetation_health")
    private Double vegetationHealth;

    /*
     * Human-readable health status.
     */
    @Column(name = "health_status")
    private String healthStatus;

    /*
     * Recommendation generated from the observation.
     */
    private String recommendation;

    /*
     * Time at which this satellite field data was recorded.
     */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}