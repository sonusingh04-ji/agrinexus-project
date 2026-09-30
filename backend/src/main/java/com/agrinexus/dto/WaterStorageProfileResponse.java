package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaterStorageProfileResponse {

    private UUID id;

    private UUID farmId;

    private Double catchmentAreaM2;

    private Double slopePercent;

    private Double runoffCoefficient;

    private Double infiltrationRateMmPerHour;

    private Double storageCapacityLiters;

    private Double currentStorageLiters;

    private Double storageSafetyLimitPercent;

    private String storageType;

    private String source;

    private String dataStatus;

    private String notes;
}