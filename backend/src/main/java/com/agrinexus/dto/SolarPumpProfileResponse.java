package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarPumpProfileResponse {

    private UUID id;

    private UUID farmId;

    private Double panelAreaM2;

    private Double panelEfficiencyPercent;

    private Double ratedSolarPowerKw;

    private Double pumpRatedPowerKw;

    private Double pumpEfficiencyPercent;

    private Double pumpHeadMeters;

    private Double pumpRatedFlowLpm;

    private Double batteryCapacityKwh;

    private Double batterySocPercent;

    private Double tankCapacityLiters;

    private Double currentTankLevelLiters;

    private Double minimumSolarIrradianceWm2;

    private String source;

    private String dataStatus;

    private String notes;
}