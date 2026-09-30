package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartIrrigationResponse {

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    private String cropStage;

    private Double currentVwc;

    private Double fieldCapacity;

    private Double wiltingPoint;

    private Double mad;

    private Double depletionLevel;

    private Double et0;

    private Double kc;

    private Double etc;

    private Double forecastRain7d;

    private Double forecastConfidence;

    private String fieldStatus;

    private String actionItem;

    private String dataStatus;

    private String uncertaintyNote;

    private LocalDateTime evaluatedAt;
}