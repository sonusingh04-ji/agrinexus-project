package com.agrinexus.dto;

import com.agrinexus.enums.RiskLevel;
import com.agrinexus.enums.RiskType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class WeatherRiskResponse {
    private UUID id;
    private UUID farmId;
    private RiskType riskType;
    private RiskLevel riskLevel;
    private String title;
    private String description;
    private String recommendation;
    private Double riskScore;
    private LocalDateTime detectedAt;
    private LocalDateTime validUntil;
}