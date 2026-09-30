package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecastResponse {

    private UUID id;

    private UUID farmId;

    private LocalDateTime forecastFor;

    private Double temperature;

    private Double feelsLike;

    private Double humidity;

    private Double rainfall;

    private Double windSpeed;

    private String weatherCondition;

    private String weatherDescription;

    private LocalDateTime createdAt;
}