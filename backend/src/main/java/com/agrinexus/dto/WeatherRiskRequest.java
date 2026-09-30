package com.agrinexus.dto;

import com.agrinexus.enums.RiskLevel;
import com.agrinexus.enums.RiskType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
public class WeatherRiskRequest {
    @NotNull(message = "Farm ID is required")
    private UUID farmId;
    @NotNull(message = "Risk type is required")
    private RiskType riskType;
    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;
    private String title;
    private String description;
    private String recommendation;
    private Double riskScore;
    private LocalDateTime validUntil;
}