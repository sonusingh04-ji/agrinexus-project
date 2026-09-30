package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationConfigResponse {

    private UUID id;

    private UUID farmId;

    private Double fieldCapacity;

    private Double wiltingPoint;

    private Double maxAllowableDepletion;

    private Double readilyAvailableWater;

    private Double rootZoneDepthCm;

    private Double targetFieldCapacityFraction;

    private Integer forecastHorizonDays;

    private Double minimumForecastConfidence;

    private String et0Method;

    private Boolean sensorBased;

    private String source;

    private String dataStatus;

    private String notes;
}