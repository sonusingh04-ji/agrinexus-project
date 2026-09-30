package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRotationRequest {

    /*
     * Farm to evaluate.
     */
    private UUID farmId;

    /*
     * Previous crop.
     *
     * Optional.
     *
     * If omitted, CropHistory is used to find
     * the latest recorded crop.
     */
    private UUID previousCropId;

    /*
     * Target season for the next crop.
     *
     * Examples:
     * RABI
     * KHARIF
     * SUMMER
     */
    private String targetSeason;

    /*
     * Farmer preference/constraint.
     *
     * Examples:
     * LOW_WATER
     * LOW_INPUT_COST
     * SHORT_DURATION
     * NONE
     */
    private String farmerConstraint;
}