package com.agrinexus.mapper;

import com.agrinexus.dto.WeatherRiskRequest;
import com.agrinexus.dto.WeatherRiskResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRisk;

public class WeatherRiskMapper {

    private WeatherRiskMapper() {
        // Prevent object creation
    }

    public static WeatherRisk toEntity(
            WeatherRiskRequest request,
            Farm farm
    ) {
        if (request == null) {
            return null;
        }

        return WeatherRisk.builder()
                .farm(farm)
                .riskType(request.getRiskType())
                .riskLevel(request.getRiskLevel())
                .title(request.getTitle())
                .description(request.getDescription())
                .recommendation(request.getRecommendation())
                .riskScore(request.getRiskScore())
                .validUntil(request.getValidUntil())
                .build();
    }

    public static WeatherRiskResponse toResponse(
            WeatherRisk weatherRisk
    ) {
        if (weatherRisk == null) {
            return null;
        }

        return WeatherRiskResponse.builder()
                .id(weatherRisk.getId())
                .farmId(weatherRisk.getFarm().getId())
                .riskType(weatherRisk.getRiskType())
                .riskLevel(weatherRisk.getRiskLevel())
                .title(weatherRisk.getTitle())
                .description(weatherRisk.getDescription())
                .recommendation(weatherRisk.getRecommendation())
                .riskScore(weatherRisk.getRiskScore())
                .detectedAt(weatherRisk.getDetectedAt())
                .validUntil(weatherRisk.getValidUntil())
                .build();
    }
}