package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScientistReviewRequest {

    private UUID farmId;

    private UUID feedbackId;

    private UUID reviewerId;

    private String reviewerName;

    /*
     * ACCEPT
     * ACCEPT_WITH_REVISION
     * REJECT
     * NEEDS_MORE_DATA
     */
    private String reviewDecision;

    private String reviewComments;

    /*
     * Only accepted reviews should normally
     * become learning/research signals.
     */
    private Boolean validatedForLearning;

    private String evidenceReference;

    private String notes;
}