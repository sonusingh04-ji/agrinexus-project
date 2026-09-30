package com.agrinexus.mapper;

import com.agrinexus.dto.CombinedRiskAnalysisResponse;
import com.agrinexus.entity.CombinedRiskAnalysis;

public class CombinedRiskAnalysisMapper {

    private CombinedRiskAnalysisMapper() {
        // Utility class
    }

    public static CombinedRiskAnalysisResponse toResponse(
            CombinedRiskAnalysis analysis
    ) {

        return CombinedRiskAnalysisResponse.builder()
                .id(analysis.getId())
                .farmId(
                        analysis.getFarm() != null
                                ? analysis.getFarm().getId()
                                : null
                )
                .combinedRiskScore(
                        analysis.getCombinedRiskScore()
                )
                .riskLevel(
                        analysis.getRiskLevel()
                )
                .title(
                        analysis.getTitle()
                )
                .analysis(
                        analysis.getAnalysis()
                )
                .recommendation(
                        analysis.getRecommendation()
                )
                .ndvi(
                        analysis.getNdvi()
                )
                .soilMoisture(
                        analysis.getSoilMoisture()
                )
                .temperature(
                        analysis.getTemperature()
                )
                .rainfall(
                        analysis.getRainfall()
                )
                .windSpeed(
                        analysis.getWindSpeed()
                )
                .analyzedAt(
                        analysis.getAnalyzedAt()
                )
                .build();
    }
}