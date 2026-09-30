package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "soil_samples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilSample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm to which this soil sample belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Date on which the soil sample was collected/tested.
     */
    private LocalDate sampleDate;

    /*
     * Type of soil.
     * Example: Loam, Clay-loam, Sandy-loam
     */
    @Column(length = 200)
    private String soilType;

    /*
     * Soil pH.
     */
    private Double pH;

    /*
     * Electrical conductivity.
     */
    private Double electricalConductivity;

    /*
     * Organic carbon percentage.
     */
    private Double organicCarbon;

    /*
     * Macronutrients.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    /*
     * Important micronutrients.
     */
    private Double zinc;

    private Double sulphur;

    /*
     * Optional additional micronutrients.
     */
    private Double iron;

    private Double manganese;

    private Double copper;

    /*
     * Source of the measurement.
     *
     * Example:
     * LABORATORY
     * FARMER_ENTERED
     * ESTIMATED
     */
    @Column(length = 50)
    private String source;

    /*
     * Additional interpretation / remarks.
     */
    @Column(length = 1000)
    private String notes;
}