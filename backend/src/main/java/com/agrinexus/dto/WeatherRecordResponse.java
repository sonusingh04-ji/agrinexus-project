package com.agrinexus.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class WeatherRecordResponse {
    private UUID id;
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