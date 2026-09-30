package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScientistReviewResponse {

    private UUID id;

    private UUID farmId;

    private UUID feedbackId;

    private UUID reviewerId;

    private String reviewerName;

    private String reviewDecision;

    private String reviewComments;

    private Boolean validatedForLearning;

    private String evidenceReference;

    private String status;

    private LocalDateTime reviewedAt;

    private String notes;
}