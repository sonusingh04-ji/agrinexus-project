package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarPumpResponse {

    private UUID farmId;

    private String decision;

    private String recommendedWindow;

    private Double solarIrradiance;

    private Double solarPowerKw;

    private Double pumpPowerKw;

    private Double currentMoisture;

    private Double batterySoc;

    private Double tankLevel;

    private String recommendation;

    private String reason;

    private String dataStatus;

    private LocalDateTime evaluatedAt;
}