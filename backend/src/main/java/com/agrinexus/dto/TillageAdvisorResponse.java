package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TillageAdvisorResponse {

    private UUID farmId;

    private UUID upcomingCropId;

    private String upcomingCropName;

    private String previousCrop;

    private String decision;

    private String recommendation;

    private String reason;

    private Double soilMoisture;

    private String soilType;

    private Boolean residuePresent;

    private String residueType;

    private String weatherCondition;

    /*
     * Type of tillage suggested.
     *
     * Examples:
     * FULL_TILLAGE
     * MINIMUM_TILLAGE
     * ZERO_TILLAGE
     * WAIT
     */
    private String tillageType;
}