package com.agrinexus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecastRequest {

    @NotNull(message = "Farm ID is required")
    private UUID farmId;

    @NotNull(message = "Forecast date and time is required")
    private LocalDateTime forecastFor;

    private Double temperature;

    private Double feelsLike;

    private Double humidity;

    private Double rainfall;

    private Double windSpeed;

    private String weatherCondition;

    private String weatherDescription;
}