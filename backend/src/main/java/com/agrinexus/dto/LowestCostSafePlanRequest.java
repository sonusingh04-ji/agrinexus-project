package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowestCostSafePlanRequest {

    private UUID farmId;

    private UUID preferredCropId;

    private String targetSeason;

    /*
     * Optional farmer constraint.
     *
     * Examples:
     * LOW_COST
     * LOW_WATER
     * LOW_ENERGY
     * SHORT_DURATION
     * NONE
     */
    private String farmerConstraint;

    /*
     * Whether soil compatibility should be required.
     */
    private Boolean requireSoilCompatibility;

    /*
     * Current soil pH.
     *
     * Optional because it can be obtained from
     * the latest farm soil record.
     */
    private Double soilPh;
    private String soilType;
    /*
     * Available/acceptable water budget in mm.
     *
     * Optional.
     */
    private Double availableWaterMm;
}