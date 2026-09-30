package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendationExplanationResponse {

    private UUID id;

    private UUID farmId;

    private UUID conversationId;

    private UUID contextSnapshotId;

    private String decisionType;

    private String decision;

    private String why;

    private String contributingFactors;

    private String dataUsed;

    private Double confidence;

    private String uncertainty;

    private String dataStatus;

    private String source;

    private LocalDateTime createdAt;
}