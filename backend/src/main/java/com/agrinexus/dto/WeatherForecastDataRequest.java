package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecastDataRequest {

    private UUID farmId;

    private LocalDate forecastDate;

    private Double rainfallMm;

    private Double precipitationProbability;

    private Double forecastConfidence;

    private Double et0Mm;

    private Double solarIrradianceWm2;

    private Double temperatureMax;

    private Double temperatureMin;

    private Double windSpeed;

    private String source;

    private String dataStatus;

    private String notes;
}