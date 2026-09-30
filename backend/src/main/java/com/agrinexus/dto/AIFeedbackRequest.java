package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIFeedbackRequest {

    private UUID farmId;

    private UUID conversationId;

    private UUID explanationId;

    private UUID userId;

    /*
     * HELPFUL
     * NOT_HELPFUL
     * CORRECT
     * INCORRECT
     * OUTCOME_REPORTED
     * OTHER
     */
    private String feedbackType;

    /*
     * Optional 1–5 rating.
     */
    private Integer rating;

    /*
     * Farmer's observation.
     */
    private String observation;

    /*
     * What actually happened.
     */
    private String observedOutcome;

    /*
     * Whether the farmer followed the recommendation.
     */
    private Boolean recommendationFollowed;

    /*
     * Whether supporting evidence was supplied.
     */
    private Boolean evidenceProvided;

    private String notes;
}