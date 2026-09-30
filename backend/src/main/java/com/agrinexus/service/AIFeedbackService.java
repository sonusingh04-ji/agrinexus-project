package com.agrinexus.service;

import com.agrinexus.dto.AIFeedbackRequest;
import com.agrinexus.dto.AIFeedbackResponse;
import com.agrinexus.entity.AIFeedback;
import com.agrinexus.entity.AIConversation;
import com.agrinexus.entity.AIRecommendationExplanation;
import com.agrinexus.entity.Farm;
import com.agrinexus.repository.AIConversationRepository;
import com.agrinexus.repository.AIFeedbackRepository;
import com.agrinexus.repository.AIRecommendationExplanationRepository;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIFeedbackService {

    private final AIFeedbackRepository feedbackRepository;
    private final FarmRepository farmRepository;
    private final AIConversationRepository conversationRepository;
    private final AIRecommendationExplanationRepository explanationRepository;

    /*
     * Submit farmer feedback.
     */
    public AIFeedbackResponse create(
            AIFeedbackRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(
                        request.getFarmId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        AIConversation conversation = null;

        if (request.getConversationId() != null) {

            conversation =
                    conversationRepository.findById(
                            request.getConversationId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI conversation not found with ID: "
                                            + request.getConversationId()
                            )
                    );

            verifyConversationFarm(
                    conversation,
                    farm.getId()
            );
        }

        AIRecommendationExplanation explanation = null;

        if (request.getExplanationId() != null) {

            explanation =
                    explanationRepository.findById(
                            request.getExplanationId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI explanation not found with ID: "
                                            + request.getExplanationId()
                            )
                    );

            if (explanation.getFarm() == null
                    || !farm.getId().equals(
                    explanation.getFarm().getId()
            )) {

                throw new RuntimeException(
                        "Explanation does not belong to the specified farm"
                );
            }
        }

        AIFeedback feedback =
                AIFeedback.builder()
                        .farm(farm)
                        .conversation(conversation)
                        .explanation(explanation)
                        .userId(request.getUserId())
                        .feedbackType(
                                normalizeFeedbackType(
                                        request.getFeedbackType()
                                )
                        )
                        .rating(request.getRating())
                        .observation(request.getObservation())
                        .observedOutcome(
                                request.getObservedOutcome()
                        )
                        .recommendationFollowed(
                                request.getRecommendationFollowed()
                        )
                        .evidenceProvided(
                                request.getEvidenceProvided()
                        )
                        .status("SUBMITTED")
                        .source("FARMER")
                        .notes(request.getNotes())
                        .createdAt(LocalDateTime.now())
                        .build();

        return toResponse(
                feedbackRepository.save(feedback)
        );
    }

    /*
     * Get feedback by ID.
     */
    public AIFeedbackResponse getById(
            UUID id
    ) {

        AIFeedback feedback =
                feedbackRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI feedback not found with ID: "
                                                + id
                                )
                        );

        return toResponse(feedback);
    }

    /*
     * Get feedback for a farm.
     */
    public List<AIFeedbackResponse> getByFarm(
            UUID farmId
    ) {

        verifyFarm(farmId);

        return feedbackRepository
                .findByFarmIdOrderByCreatedAtDesc(
                        farmId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get pending feedback for scientist review.
     */
    public List<AIFeedbackResponse> getPendingReview() {

        return feedbackRepository
                .findByStatusOrderByCreatedAtAsc(
                        "SUBMITTED"
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Update feedback before expert review.
     *
     * This is useful if a farmer adds more information.
     */
    public AIFeedbackResponse update(
            UUID id,
            AIFeedbackRequest request
    ) {

        AIFeedback feedback =
                feedbackRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI feedback not found with ID: "
                                                + id
                                )
                        );

        if (!"SUBMITTED".equalsIgnoreCase(
                feedback.getStatus()
        )
                && !"NEEDS_MORE_INFORMATION".equalsIgnoreCase(
                feedback.getStatus()
        )) {

            throw new RuntimeException(
                    "Feedback cannot be updated after review has been completed"
            );
        }

        if (request.getFeedbackType() != null) {
            feedback.setFeedbackType(
                    normalizeFeedbackType(
                            request.getFeedbackType()
                    )
            );
        }

        if (request.getRating() != null) {
            validateRating(
                    request.getRating()
            );
            feedback.setRating(
                    request.getRating()
            );
        }

        if (request.getObservation() != null) {
            feedback.setObservation(
                    request.getObservation()
            );
        }

        if (request.getObservedOutcome() != null) {
            feedback.setObservedOutcome(
                    request.getObservedOutcome()
            );
        }

        if (request.getRecommendationFollowed() != null) {
            feedback.setRecommendationFollowed(
                    request.getRecommendationFollowed()
            );
        }

        if (request.getEvidenceProvided() != null) {
            feedback.setEvidenceProvided(
                    request.getEvidenceProvided()
            );
        }

        if (request.getNotes() != null) {
            feedback.setNotes(
                    request.getNotes()
            );
        }

        return toResponse(
                feedbackRepository.save(feedback)
        );
    }

    /*
     * Withdraw feedback before review.
     */
    public void delete(
            UUID id
    ) {

        AIFeedback feedback =
                feedbackRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI feedback not found"
                                )
                        );

        if ("VALIDATED".equalsIgnoreCase(
                feedback.getStatus()
        )
                || "REJECTED".equalsIgnoreCase(
                feedback.getStatus()
        )) {

            throw new RuntimeException(
                    "Reviewed feedback cannot be deleted"
            );
        }

        feedbackRepository.deleteById(id);
    }

    private void validate(
            AIFeedbackRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }

        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getRating() != null) {

            validateRating(
                    request.getRating()
            );
        }

        if (request.getFeedbackType() == null
                || request.getFeedbackType().isBlank()) {

            throw new RuntimeException(
                    "Feedback type is required"
            );
        }

        if ((request.getObservation() == null
                || request.getObservation().isBlank())
                && (
                request.getObservedOutcome() == null
                        || request.getObservedOutcome().isBlank()
        )
                && request.getRating() == null) {

            throw new RuntimeException(
                    "Provide an observation, outcome or rating"
            );
        }
    }

    private void validateRating(
            Integer rating
    ) {

        if (rating < 1 || rating > 5) {

            throw new RuntimeException(
                    "Rating must be between 1 and 5"
            );
        }
    }

    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    private void verifyConversationFarm(
            AIConversation conversation,
            UUID farmId
    ) {

        if (conversation.getFarm() == null
                || !farmId.equals(
                conversation.getFarm().getId()
        )) {

            throw new RuntimeException(
                    "Conversation does not belong to the specified farm"
            );
        }
    }

    private String normalizeFeedbackType(
            String type
    ) {

        if (type == null
                || type.isBlank()) {

            return "OTHER";
        }

        return type.trim()
                .toUpperCase();
    }

    private AIFeedbackResponse toResponse(
            AIFeedback feedback
    ) {

        return AIFeedbackResponse.builder()
                .id(feedback.getId())
                .farmId(
                        feedback.getFarm() != null
                                ? feedback.getFarm().getId()
                                : null
                )
                .conversationId(
                        feedback.getConversation() != null
                                ? feedback.getConversation().getId()
                                : null
                )
                .explanationId(
                        feedback.getExplanation() != null
                                ? feedback.getExplanation().getId()
                                : null
                )
                .userId(feedback.getUserId())
                .feedbackType(feedback.getFeedbackType())
                .rating(feedback.getRating())
                .observation(feedback.getObservation())
                .observedOutcome(
                        feedback.getObservedOutcome()
                )
                .recommendationFollowed(
                        feedback.getRecommendationFollowed()
                )
                .evidenceProvided(
                        feedback.getEvidenceProvided()
                )
                .status(feedback.getStatus())
                .source(feedback.getSource())
                .notes(feedback.getNotes())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}