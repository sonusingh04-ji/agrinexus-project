package com.agrinexus.mapper;

import com.agrinexus.dto.FarmerAlertResponse;
import com.agrinexus.entity.FarmerAlert;

public class FarmerAlertMapper {

    private FarmerAlertMapper() {
        // Utility class
    }

    public static FarmerAlertResponse toResponse(
            FarmerAlert alert
    ) {

        return FarmerAlertResponse.builder()
                .id(alert.getId())
                .farmId(
                        alert.getFarm() != null
                                ? alert.getFarm().getId()
                                : null
                )
                .riskLevel(alert.getRiskLevel())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .recommendation(alert.getRecommendation())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .validUntil(alert.getValidUntil())
                .build();
    }
}