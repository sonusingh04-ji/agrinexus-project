package com.agrinexus.service;

import com.agrinexus.entity.AIFeedback;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.ScientistReview;
import com.agrinexus.repository.AIFeedbackRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.ScientistReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScientistReviewService {

    private final ScientistReviewRepository reviewRepository;
    private final AIFeedbackRepository feedbackRepository;
    private final FarmRepository farmRepository;

    /*
     * Create a scientist review for farmer feedback.
     */
    @Transactional
    public ScientistReview review(
            UUID farmId,
            UUID feedbackId,
            UUID reviewerId,
            String reviewerName,
            String reviewDecision,
            String reviewComments,
            Boolean validatedForLearning,
            String evidenceReference,
            String notes
    ) {

        if (farmId == null) {
            throw new RuntimeException("Farm ID is required");
        }

        if (feedbackId == null) {
            throw new RuntimeException("Feedback ID is required");
        }

        if (reviewDecision == null
                || reviewDecision.isBlank()) {

            throw new RuntimeException(
                    "Review decision is required"
            );
        }

        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );

        AIFeedback feedback =
                feedbackRepository.findById(feedbackId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI feedback not found with ID: "
                                                + feedbackId
                                )
                        );

        if (feedback.getFarm() == null
                || !farmId.equals(
                feedback.getFarm().getId()
        )) {

            throw new RuntimeException(
                    "Feedback does not belong to the specified farm"
            );
        }

        String decision =
                normalizeDecision(
                        reviewDecision
                );

        validateDecision(
                decision
        );

        /*
         * Learning validation must only be enabled
         * for an accepted review.
         */
        boolean learningEligible =
                Boolean.TRUE.equals(
                        validatedForLearning
                );

        if (learningEligible
                && !"ACCEPT".equals(decision)
                && !"ACCEPT_WITH_REVISION".equals(decision)) {

            throw new RuntimeException(
                    "Feedback can be marked for learning only when the review is accepted"
            );
        }

        /*
         * Mark feedback as being reviewed.
         */
        feedback.setStatus(
                "UNDER_REVIEW"
        );

        feedbackRepository.save(
                feedback
        );

        ScientistReview review =
                ScientistReview.builder()
                        .farm(farm)
                        .feedback(feedback)
                        .reviewerId(reviewerId)
                        .reviewerName(reviewerName)
                        .reviewDecision(decision)
                        .reviewComments(reviewComments)
                        .validatedForLearning(
                                learningEligible
                        )
                        .evidenceReference(
                                evidenceReference
                        )
                        .status(
                                "COMPLETED"
                        )
                        .reviewedAt(
                                LocalDateTime.now()
                        )
                        .notes(notes)
                        .build();

        ScientistReview savedReview =
                reviewRepository.save(
                        review
                );

        /*
         * Update farmer-feedback status based on
         * the expert decision.
         */
        updateFeedbackStatus(
                feedback,
                decision
        );

        return savedReview;
    }

    /*
     * Get one scientist review.
     */
    public ScientistReview getById(
            UUID id
    ) {

        return reviewRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Scientist review not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * Get all reviews for a farm.
     */
    public List<ScientistReview> getByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return reviewRepository
                .findByFarmIdOrderByReviewedAtDesc(
                        farmId
                );
    }

    /*
     * Get all reviews for one feedback record.
     */
    public List<ScientistReview> getByFeedback(
            UUID feedbackId
    ) {

        if (!feedbackRepository.existsById(
                feedbackId
        )) {

            throw new RuntimeException(
                    "AI feedback not found"
            );
        }

        return reviewRepository
                .findByFeedbackIdOrderByReviewedAtDesc(
                        feedbackId
                );
    }

    /*
     * Get accepted/validated reviews that may be
     * used as future learning or research signals.
     */
    public List<ScientistReview>
    getValidatedForLearning() {

        return reviewRepository
                .findByValidatedForLearningTrueOrderByReviewedAtDesc();
    }

    /*
     * Get reviews by decision.
     */
    public List<ScientistReview>
    getByDecision(
            String reviewDecision
    ) {

        return reviewRepository
                .findByReviewDecisionOrderByReviewedAtDesc(
                        normalizeDecision(
                                reviewDecision
                        )
                );
    }

    /*
     * Additional helper for a pending feedback item.
     */
    @Transactional
    public void markFeedbackUnderReview(
            UUID feedbackId
    ) {

        AIFeedback feedback =
                feedbackRepository.findById(
                        feedbackId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "AI feedback not found with ID: "
                                        + feedbackId
                        )
                );

        if ("VALIDATED".equalsIgnoreCase(
                feedback.getStatus()
        )
                || "REJECTED".equalsIgnoreCase(
                feedback.getStatus()
        )) {

            throw new RuntimeException(
                    "Completed feedback cannot be moved back to review"
            );
        }

        feedback.setStatus(
                "UNDER_REVIEW"
        );

        feedbackRepository.save(
                feedback
        );
    }

    /*
     * Update feedback status after expert decision.
     */
    private void updateFeedbackStatus(
            AIFeedback feedback,
            String decision
    ) {

        switch (decision) {

            case "ACCEPT":
            case "ACCEPT_WITH_REVISION":

                feedback.setStatus(
                        "VALIDATED"
                );

                break;

            case "REJECT":

                feedback.setStatus(
                        "REJECTED"
                );

                break;

            case "NEEDS_MORE_DATA":

                feedback.setStatus(
                        "NEEDS_MORE_INFORMATION"
                );

                break;

            default:

                throw new RuntimeException(
                        "Unsupported review decision: "
                                + decision
                );
        }

        feedbackRepository.save(
                feedback
        );
    }

    /*
     * Validate allowed expert decisions.
     */
    private void validateDecision(
            String decision
    ) {

        if (!decision.equals("ACCEPT")
                && !decision.equals("ACCEPT_WITH_REVISION")
                && !decision.equals("REJECT")
                && !decision.equals("NEEDS_MORE_DATA")) {

            throw new RuntimeException(
                    "Invalid review decision. "
                            + "Allowed values: "
                            + "ACCEPT, "
                            + "ACCEPT_WITH_REVISION, "
                            + "REJECT, "
                            + "NEEDS_MORE_DATA"
            );
        }
    }

    private String normalizeDecision(
            String decision
    ) {

        if (decision == null) {
            return null;
        }

        return decision.trim()
                .toUpperCase();
    }

    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }
}