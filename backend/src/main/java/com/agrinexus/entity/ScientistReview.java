package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scientist_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScientistReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm associated with the reviewed recommendation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Feedback being reviewed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private AIFeedback feedback;

    /*
     * Reviewer identifier.
     *
     * Kept as UUID so the existing User model can be
     * connected later after the project's expert-role
     * structure is finalized.
     */
    private UUID reviewerId;

    /*
     * Reviewer name/identifier for demonstration
     * environments where an account may not yet exist.
     */
    @Column(length = 200)
    private String reviewerName;

    /*
     * Review decision.
     *
     * ACCEPT
     * ACCEPT_WITH_REVISION
     * REJECT
     * NEEDS_MORE_DATA
     */
    @Column(nullable = false, length = 40)
    private String reviewDecision;

    /*
     * Expert/scientist comments.
     */
    @Column(length = 8000)
    private String reviewComments;

    /*
     * Whether the feedback is considered suitable
     * for future model/rule improvement.
     */
    private Boolean validatedForLearning;

    /*
     * Evidence/reference supporting the review.
     */
    @Column(length = 5000)
    private String evidenceReference;

    /*
     * Review status.
     *
     * COMPLETED
     * NEEDS_MORE_INFORMATION
     */
    @Column(nullable = false, length = 40)
    private String status;

    /*
     * Review timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime reviewedAt;

    @Column(length = 3000)
    private String notes;
}