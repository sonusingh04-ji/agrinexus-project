package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICopilotResponse {

    private UUID conversationId;

    private UUID messageId;

    private UUID contextSnapshotId;

    private UUID explanationId;

    private UUID farmId;

    private String question;

    private String answer;

    private String why;

    private String confidenceLevel;

    private Double confidence;

    private String uncertainty;

    private String dataStatus;

    private String language;

    private String channel;

    private Boolean decisionContextUsed;

    private Boolean requiresReview;

    private LocalDateTime answeredAt;
}