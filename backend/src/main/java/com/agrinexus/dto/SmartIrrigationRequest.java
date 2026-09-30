package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartIrrigationRequest {

    private UUID farmId;

    private UUID cropId;

    private String cropStage;

    private Double currentVwc;

    private Double et0;

    private Double kc;

    private Double forecastRain7d;

    private Double forecastConfidence;

    private Double recentRainfall;

    private Double fieldCapacity;

    private Double wiltingPoint;

    private Double mad;

    private Double rootDepthCm;
}