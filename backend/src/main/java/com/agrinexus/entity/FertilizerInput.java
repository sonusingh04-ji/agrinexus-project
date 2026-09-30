package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fertilizer_inputs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerInput {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm receiving the fertilizer/input.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Optional crop associated with this application.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Date of application.
     */
    @Column(nullable = false)
    private LocalDate applicationDate;

    /*
     * Product/fertilizer name.
     */
    @Column(nullable = false, length = 200)
    private String productName;

    /*
     * Product type.
     *
     * Examples:
     * CHEMICAL
     * ORGANIC
     * BIOLOGICAL
     * MANURE
     */
    @Column(length = 50)
    private String inputType;

    /*
     * Applied product quantity.
     */
    private Double quantity;

    /*
     * Unit of quantity.
     *
     * Example:
     * kg
     * kg/acre
     * litre
     */
    @Column(length = 30)
    private String quantityUnit;

    /*
     * Nutrient content supplied by this input.
     *
     * These are nutrient amounts, not soil-test concentrations.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Source/provenance of the record.
     *
     * Examples:
     * FARMER_ENTERED
     * RECOMMENDATION
     * IMPORTED
     */
    @Column(length = 50)
    private String source;

    /*
     * Additional information.
     */
    @Column(length = 1000)
    private String notes;
}