package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Crop name
     * Example: Wheat, Rice, Maize, Mustard
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 100)
    private String category;

    /*
     * Main growing season
     * Example: Rabi, Kharif, Summer
     */
    @Column(length = 50)
    private String season;

    /*
     * Recommended sowing period
     * Example: "1st week to end of November"
     */
    @Column(length = 500)
    private String sowingWindow;

    /*
     * Suitable temperature range at sowing/growth stage.
     */
    private Double idealTemperatureMin;

    private Double idealTemperatureMax;

    /*
     * Suitable soil type.
     * Example: Loam, Clay-loam, Sandy-loam
     */
    @Column(length = 500)
    private String soilType;

    /*
     * Suitable soil pH range.
     */
    private Double soilPhMin;

    private Double soilPhMax;

    /*
     * Recommended seed rate.
     * Stored as descriptive text because the source uses
     * different units/ranges depending on crop and timing.
     */
    @Column(length = 300)
    private String seedRate;

    /*
     * Row-to-row / plant-to-plant spacing.
     */
    @Column(length = 300)
    private String rowSpacing;

    /*
     * Recommended sowing depth.
     */
    @Column(length = 200)
    private String sowingDepth;

    /*
     * General crop water requirement.
     */
    @Column(length = 500)
    private String waterNeed;

    /*
     * Crop stages that are particularly sensitive
     * to water stress.
     */
    @Column(length = 1000)
    private String criticalWaterSensitiveStages;

    /*
     * Common crop rotations.
     * Example: Rice-Wheat, Rice-Chickpea
     */
    @Column(length = 500)
    private String commonRotation;
}