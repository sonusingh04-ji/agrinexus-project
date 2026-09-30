package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseasePestRiskResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    private String riskCategory;

    private String riskName;

    private String riskLevel;

    private Double riskScore;

    private String reason;

    private String recommendation;

    private String scoutingAdvice;

    private String contributingFactors;

    private String source;

    private String dataStatus;

    private String uncertaintyNote;

    private Boolean forecastBased;

    private LocalDateTime evaluatedAt;

    private LocalDateTime validUntil;
}