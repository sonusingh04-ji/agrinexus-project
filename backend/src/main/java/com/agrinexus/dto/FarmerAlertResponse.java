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
public class FarmerAlertResponse {

    private UUID id;

    private UUID farmId;

    private RiskLevel riskLevel;

    private String title;

    private String message;

    private String recommendation;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime validUntil;
}