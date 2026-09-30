package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerInputResponse {

    /*
     * Unique input record ID.
     */
    private UUID id;

    /*
     * Farm information.
     */
    private UUID farmId;

    /*
     * Associated crop.
     */
    private UUID cropId;

    private String cropName;

    /*
     * Application date.
     */
    private LocalDate applicationDate;

    /*
     * Fertilizer/product information.
     */
    private String productName;

    private String inputType;

    /*
     * Product quantity.
     */
    private Double quantity;

    private String quantityUnit;

    /*
     * Nutrient amounts supplied by the input.
     */
    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    /*
     * Data provenance.
     */
    private String source;

    /*
     * Additional notes.
     */
    private String notes;
}