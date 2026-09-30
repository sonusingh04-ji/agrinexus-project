package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilIntelligenceResponse {

    /*
     * Farm.
     */
    private UUID farmId;

    /*
     * Soil sample used for this analysis.
     */
    private UUID soilSampleId;

    private LocalDate sampleDate;

    /*
     * Source / provenance.
     *
     * Examples:
     * LABORATORY
     * SOIL_TEST
     * FARMER_ENTERED
     * ESTIMATED
     */
    private String source;

    /*
     * Data status.
     *
     * MEASURED
     * ESTIMATED
     * MIXED
     * UNKNOWN
     */
    private String dataStatus;

    /*
     * Soil information.
     */
    private String soilType;

    private Double pH;

    private Double electricalConductivity;

    private Double organicCarbon;

    /*
     * Major nutrients.
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
     * Number of populated measurements.
     */
    private Integer measuredParameterCount;

    private Integer missingParameterCount;

    /*
     * Completeness percentage of the supported
     * soil parameters.
     */
    private Double completenessScore;

    /*
     * General interpretation of the dataset.
     *
     * This is a data-quality/coverage interpretation,
     * not a universal fertility grade.
     */
    private String soilDataStatus;

    /*
     * Crop-specific pH interpretation when cropId
     * is supplied and the crop has a configured pH range.
     */
    private String cropPhInterpretation;

    private UUID cropId;

    private String cropName;

    private Double cropSoilPhMin;

    private Double cropSoilPhMax;

    /*
     * Parameters for which measurements are available.
     */
    private List<String> availableParameters;

    /*
     * Parameters for which measurements are missing.
     */
    private List<String> missingParameters;

    /*
     * Safe findings supported by the available data.
     */
    private List<String> findings;

    /*
     * Explanation shown to the farmer / UI.
     */
    private String explanation;

    /*
     * Important limitations.
     */
    private String uncertaintyNote;
}