package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TillageAdvisorRequest {

    /*
     * Farm for which the tillage decision
     * is required.
     */
    private UUID farmId;

    /*
     * Previous crop.
     *
     * Explicitly supplied when the farmer wants
     * to evaluate the crop immediately preceding
     * the upcoming crop.
     */
    private UUID previousCropId;


    /*
     * Upcoming crop that the farmer plans to grow.
     */
    private UUID upcomingCropId;

    /*
     * Current soil moisture percentage.
     *
     * If omitted, the service can use the
     * latest available field-monitoring value.
     */
    private Double soilMoisture;

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
     * Whether crop residue is currently
     * present in the field.
     */
    private Boolean residuePresent;

    /*
     * Type of crop residue.
     *
     * Example:
     * Rice straw
     * Wheat straw
     * Maize stalk
     */
    private String residueType;

    /*
     * Current weather condition.
     */
    private String weatherCondition;
}