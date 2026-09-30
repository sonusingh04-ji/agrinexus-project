package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm from which the feedback originated.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Related AI conversation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AIConversation conversation;

    /*
     * Related recommendation explanation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "explanation_id")
    private AIRecommendationExplanation explanation;

    /*
     * Farmer/user who submitted the feedback.
     */
    private UUID userId;

    /*
     * Feedback type.
     *
     * HELPFUL
     * NOT_HELPFUL
     * CORRECT
     * INCORRECT
     * OUTCOME_REPORTED
     * OTHER
     */
    @Column(length = 40)
    private String feedbackType;

    /*
     * Optional rating from 1 to 5.
     */
    private Integer rating;

    /*
     * Farmer observation.
     */
    @Column(length = 5000)
    private String observation;

    /*
     * What actually happened after the recommendation.
     */
    @Column(length = 5000)
    private String observedOutcome;

    /*
     * Whether the farmer followed the recommendation.
     */
    private Boolean recommendationFollowed;

    /*
     * Whether supporting evidence was attached/provided.
     */
    private Boolean evidenceProvided;

    /*
     * Current validation status.
     *
     * SUBMITTED
     * UNDER_REVIEW
     * VALIDATED
     * REJECTED
     * NEEDS_MORE_INFORMATION
     */
    @Column(nullable = false, length = 40)
    private String status;

    /*
     * Source of feedback.
     *
     * FARMER
     * SYSTEM
     * OTHER
     */
    @Column(length = 30)
    private String source;

    @Column(length = 3000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}