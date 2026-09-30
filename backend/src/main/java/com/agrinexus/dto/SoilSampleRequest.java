package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilSampleRequest {

    /*
     * Farm to which the soil sample belongs.
     */
    private UUID farmId;

    /*
     * Date of soil sample collection/testing.
     */
    private LocalDate sampleDate;

    /*
     * Soil type / texture.
     *
     * Example:
     * Loam
     * Clay-loam
     * Sandy-loam
     */
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
     * Micronutrients.
     */
    private Double zinc;

    private Double sulphur;

    private Double iron;

    private Double manganese;

    private Double copper;

    /*
     * Measurement source.
     *
     * Example:
     * LABORATORY
     * FARMER_ENTERED
     * ESTIMATED
     */
    private String source;

    /*
     * Additional notes.
     */
    private String notes;
}