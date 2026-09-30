package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationTelemetryRequest {

    private UUID farmId;

    private String deviceId;

    private LocalDateTime recordedAt;

    private Double vwc15cm;

    private Double vwc30cm;

    private Double vwc60cm;

    private Integer pumpState;

    private Double flowRateLpm;

    private Double linePressureBar;

    private Double waterTableDepthM;

    private Double pumpRuntimeMinutes;

    private Double rainfallMm;

    private Double et0Mm;

    private Double etcMm;

    private Double solarIrradianceWm2;

    private Double solarPowerKw;

    private Double cloudCoverPercent;

    private Double storageLevelPercent;

    private Double batterySocPercent;

    private String cropStage;

    private Double kc;

    private String source;

    private String sensorStatus;

    private String payloadReference;

    private String notes;
}