package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RainfallHarvestingResponse {

    private UUID farmId;

    private Double forecastRainfallMm;

    private Double effectiveRainfallMm;

    private Double forecastConfidence;

    private Double currentMoisture;

    private Double fieldCapacity;

    private Double storageCapacityLiters;

    private Double currentStorageLiters;

    private Double estimatedCaptureLiters;

    private String decision;

    private String recommendation;

    private String reason;

    private String dataStatus;

    private LocalDateTime evaluatedAt;
}