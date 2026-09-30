package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerRecommendationRequest {

    /*
     * Farm for which the recommendation is required.
     */
    private UUID farmId;

    /*
     * Crop for which fertilizer guidance is required.
     */
    private UUID cropId;

    /*
     * Current crop growth stage.
     *
     * Examples:
     * SOWING
     * VEGETATIVE
     * TILLERING
     * FLOWERING
     * GRAIN_FILLING
     * HARVEST
     */
    private String cropStage;

    /*
     * Field area.
     */
    private Double fieldArea;

    /*
     * ACRE / HECTARE
     */
    private String fieldAreaUnit;

    /*
     * Optional request-side note describing the
     * farmer's current fertilizer situation.
     */
    private String farmerNote;
}