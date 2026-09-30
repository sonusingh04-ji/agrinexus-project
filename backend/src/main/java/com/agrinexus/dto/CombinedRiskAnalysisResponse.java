package com.agrinexus.dto;

import com.agrinexus.enums.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedRiskAnalysisResponse {

    private UUID id;

    private UUID farmId;

    private Double combinedRiskScore;

    private RiskLevel riskLevel;

    private String title;

    private String analysis;

    private String recommendation;

    private Double ndvi;

    private Double soilMoisture;

    private Double temperature;

    private Double rainfall;

    private Double windSpeed;

    private LocalDateTime analyzedAt;
}