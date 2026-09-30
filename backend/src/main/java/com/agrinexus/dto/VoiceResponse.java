package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceResponse {

    private UUID farmId;

    private UUID conversationId;

    private UUID messageId;

    private UUID contextSnapshotId;

    private String language;

    private String transcript;

    private String answer;

    private String spokenText;

    private String confidenceLevel;

    private Double confidence;

    private Boolean decisionContextUsed;

    private Boolean requiresReview;

    private String dataStatus;

    /*
     * Indicates whether the current implementation
     * produced actual audio or only prepared text
     * for a TTS provider.
     */
    private Boolean audioGenerated;

    private LocalDateTime respondedAt;
}