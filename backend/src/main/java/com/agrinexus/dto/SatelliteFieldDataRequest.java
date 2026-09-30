package com.agrinexus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SatelliteFieldDataRequest {

    @NotNull(message = "Farm ID is required")
    private UUID farmId;

    @NotNull(message = "NDVI value is required")
    @Min(value = -1, message = "NDVI cannot be less than -1")
    @Max(value = 1, message = "NDVI cannot be greater than 1")
    private Double ndvi;

    @Min(value = 0, message = "Vegetation health cannot be negative")
    @Max(value = 100, message = "Vegetation health cannot exceed 100")
    private Double vegetationHealth;

    @Min(value = 0, message = "Soil moisture cannot be negative")
    @Max(value = 100, message = "Soil moisture cannot exceed 100")
    private Double soilMoisture;

    @Min(value = 0, message = "Cloud coverage cannot be negative")
    @Max(value = 100, message = "Cloud coverage cannot exceed 100")
    private Double cloudCoverage;
}