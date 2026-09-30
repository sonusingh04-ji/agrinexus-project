package com.agrinexus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class WeatherRecordRequest {

    @NotNull(message = "Farm ID is required")
    private UUID farmId;

    private LocalDateTime recordedAt;

    private Double temperature;

    private Double feelsLike;

    private Integer humidity;

    private Double rainfall;

    private Double windSpeed;

    private Integer windDirection;

    private Double pressure;

    private String weatherCondition;

    private String weatherDescription;
}