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
public class RiskAnalyticsResponse {

    private UUID farmId;

    private String farmName;

    /*
     * Current/latest risk information
     */
    private Double currentRiskScore;

    private RiskLevel currentRiskLevel;

    /*
     * Overall historical statistics
     */
    private Double averageRiskScore;

    private Double highestRiskScore;

    private Double lowestRiskScore;

    private Integer totalAnalyses;

    /*
     * Risk level statistics
     */
    private Long criticalRiskCount;

    private Long highRiskCount;

    private Long mediumRiskCount;

    private Long lowRiskCount;

    /*
     * Historical data for charts and graphs
     */
    private List<RiskHistoryPoint> riskHistory;

    /*
     * Last analysis time
     */
    private LocalDateTime lastAnalyzedAt;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskHistoryPoint {

        private UUID analysisId;

        private Double riskScore;

        private RiskLevel riskLevel;

        private LocalDateTime analyzedAt;
    }
}