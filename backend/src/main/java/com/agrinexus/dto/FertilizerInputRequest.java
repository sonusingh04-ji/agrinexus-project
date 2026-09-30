package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerInputRequest {

    /*
     * Farm where the input was applied.
     */
    private UUID farmId;

    /*
     * Crop associated with the application.
     */
    private UUID cropId;

    /*
     * Date of application.
     */
    private LocalDate applicationDate;

    /*
     * Fertilizer/product name.
     */
    private String productName;

    /*
     * CHEMICAL / ORGANIC / BIOLOGICAL / MANURE
     */
    private String inputType;

    /*
     * Quantity of the product applied.
     */
    private Double quantity;

    /*
     * kg / kg-acre / litre / etc.
     */
    private String quantityUnit;

    /*
     * Nutrient amounts supplied by this input.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Source/provenance.
     *
     * Examples:
     * FARMER_ENTERED
     * IMPORTED
     * RECOMMENDATION
     */
    private String source;

    /*
     * Optional notes.
     */
    private String notes;
}