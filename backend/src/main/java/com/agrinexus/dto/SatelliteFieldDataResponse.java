package com.agrinexus.dto;

import com.agrinexus.enums.FieldHealthStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatelliteFieldDataResponse {

    private UUID id;

    private UUID farmId;

    private Double ndvi;

    private Double vegetationHealth;

    private Double soilMoisture;

    private Double cloudCoverage;

    private FieldHealthStatus healthStatus;

    private String recommendation;

    private LocalDateTime recordedAt;
}