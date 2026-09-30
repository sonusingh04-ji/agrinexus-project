package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIFeedbackResponse {

    private UUID id;

    private UUID farmId;

    private UUID conversationId;

    private UUID explanationId;

    private UUID userId;

    private String feedbackType;

    private Integer rating;

    private String observation;

    private String observedOutcome;

    private Boolean recommendationFollowed;

    private Boolean evidenceProvided;

    private String status;

    private String source;

    private String notes;

    private LocalDateTime createdAt;
}