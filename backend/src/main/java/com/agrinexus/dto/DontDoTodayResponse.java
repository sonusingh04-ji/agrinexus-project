package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DontDoTodayResponse {

    private UUID farmId;

    private String action;

    private String decision;

    private String title;

    private String message;

    private String reason;

    private Double forecastRainfall;

    private Double forecastConfidence;

    private Double currentMoisture;

    private Double fieldCapacity;

    private Double temperature;

    private Double windSpeed;

    private Double solarIrradiance;

    private String dataStatus;

    private LocalDateTime evaluatedAt;
}