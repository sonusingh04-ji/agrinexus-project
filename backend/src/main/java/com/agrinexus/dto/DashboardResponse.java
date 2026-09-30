package com.agrinexus.dto;

import com.agrinexus.enums.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private UUID farmId;

    private String farmName;

    private String location;

    private Double latitude;

    private Double longitude;

    // Latest weather information
    private Double temperature;

    private Double rainfall;

    private Double windSpeed;

    private String weatherCondition;

    // Latest satellite information
    private Double ndvi;

    private Double vegetationHealth;

    private Double soilMoisture;

    private String fieldHealthStatus;

    // Latest combined risk
    private Double combinedRiskScore;

    private RiskLevel riskLevel;

    private String riskTitle;

    private String recommendation;

    // Active alerts
    private Integer activeRiskCount;

    private List<String> activeRiskTitles;

    private LocalDateTime lastUpdated;
}