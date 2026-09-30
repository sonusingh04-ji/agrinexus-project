package com.agrinexus.service;

import com.agrinexus.dto.AIRecommendationExplanationRequest;
import com.agrinexus.dto.AIRecommendationExplanationResponse;
import com.agrinexus.entity.AIContextSnapshot;
import com.agrinexus.entity.AIConversation;
import com.agrinexus.entity.AIRecommendationExplanation;
import com.agrinexus.entity.Farm;
import com.agrinexus.repository.AIContextSnapshotRepository;
import com.agrinexus.repository.AIConversationRepository;
import com.agrinexus.repository.AIRecommendationExplanationRepository;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIRecommendationExplanationService {

    private final AIRecommendationExplanationRepository explanationRepository;
    private final FarmRepository farmRepository;
    private final AIConversationRepository conversationRepository;
    private final AIContextSnapshotRepository contextSnapshotRepository;

    /*
     * Create a new explanation.
     */
    public AIRecommendationExplanationResponse create(
            AIRecommendationExplanationRequest request
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

            /*
             * Prevent linking an explanation to
             * a conversation belonging to another farm.
             */
            if (conversation.getFarm() != null
                    && !farm.getId().equals(
                    conversation.getFarm().getId()
            )) {

                throw new RuntimeException(
                        "Conversation does not belong to the specified farm"
                );
            }
        }

        AIContextSnapshot contextSnapshot = null;

        if (request.getContextSnapshotId() != null) {

            contextSnapshot =
                    contextSnapshotRepository.findById(
                            request.getContextSnapshotId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI context snapshot not found with ID: "
                                            + request.getContextSnapshotId()
                            )
                    );

            /*
             * Prevent linking an explanation to
             * another farm's context.
             */
            if (contextSnapshot.getFarm() != null
                    && !farm.getId().equals(
                    contextSnapshot.getFarm().getId()
            )) {

                throw new RuntimeException(
                        "Context snapshot does not belong to the specified farm"
                );
            }
        }

        AIRecommendationExplanation explanation =
                AIRecommendationExplanation.builder()
                        .farm(farm)
                        .conversation(conversation)
                        .contextSnapshot(contextSnapshot)
                        .decisionType(
                                request.getDecisionType()
                        )
                        .decision(
                                request.getDecision()
                        )
                        .why(
                                request.getWhy()
                        )
                        .contributingFactors(
                                request.getContributingFactors()
                        )
                        .dataUsed(
                                request.getDataUsed()
                        )
                        .confidence(
                                request.getConfidence()
                        )
                        .uncertainty(
                                request.getUncertainty()
                        )
                        .dataStatus(
                                request.getDataStatus()
                        )
                        .source(
                                request.getSource()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        return toResponse(
                explanationRepository.save(
                        explanation
                )
        );
    }

    /*
     * Get explanation by ID.
     */
    public AIRecommendationExplanationResponse getById(
            UUID id
    ) {

        AIRecommendationExplanation explanation =
                explanationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI recommendation explanation not found with ID: "
                                                + id
                                )
                        );

        return toResponse(explanation);
    }

    /*
     * Get all explanations for a farm.
     */
    public List<AIRecommendationExplanationResponse> getByFarm(
            UUID farmId
    ) {

        verifyFarm(farmId);

        return explanationRepository
                .findByFarmIdOrderByCreatedAtDesc(
                        farmId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get explanations belonging to a conversation.
     */
    public List<AIRecommendationExplanationResponse>
    getByConversation(
            UUID conversationId
    ) {

        if (!conversationRepository.existsById(
                conversationId
        )) {

            throw new RuntimeException(
                    "AI conversation not found"
            );
        }

        return explanationRepository
                .findByConversationIdOrderByCreatedAtDesc(
                        conversationId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get latest explanation for a farm.
     */
    public AIRecommendationExplanationResponse
    getLatestByFarm(
            UUID farmId
    ) {

        verifyFarm(farmId);

        AIRecommendationExplanation explanation =
                explanationRepository
                        .findFirstByFarmIdOrderByCreatedAtDesc(
                                farmId
                        );

        if (explanation == null) {
            return null;
        }

        return toResponse(explanation);
    }

    /*
     * Delete an explanation.
     *
     * Mainly useful for correcting test/demo records.
     */
    public void delete(
            UUID id
    ) {

        if (!explanationRepository.existsById(id)) {

            throw new RuntimeException(
                    "AI recommendation explanation not found"
            );
        }

        explanationRepository.deleteById(id);
    }

    /*
     * Validation.
     */
    private void validate(
            AIRecommendationExplanationRequest request
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

        if (request.getDecisionType() == null
                || request.getDecisionType().isBlank()) {

            throw new RuntimeException(
                    "Decision type is required"
            );
        }

        if (request.getDecision() == null
                || request.getDecision().isBlank()) {

            throw new RuntimeException(
                    "Decision is required"
            );
        }

        if (request.getWhy() == null
                || request.getWhy().isBlank()) {

            throw new RuntimeException(
                    "Why explanation is required"
            );
        }

        if (request.getConfidence() != null
                && (
                request.getConfidence() < 0.0
                        || request.getConfidence() > 1.0
        )) {

            throw new RuntimeException(
                    "Confidence must be between 0 and 1"
            );
        }
    }

    /*
     * Verify farm exists.
     */
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

    /*
     * Convert entity → response.
     */
    private AIRecommendationExplanationResponse
    toResponse(
            AIRecommendationExplanation explanation
    ) {

        return AIRecommendationExplanationResponse
                .builder()
                .id(
                        explanation.getId()
                )
                .farmId(
                        explanation.getFarm() != null
                                ? explanation.getFarm().getId()
                                : null
                )
                .conversationId(
                        explanation.getConversation() != null
                                ? explanation
                                .getConversation()
                                .getId()
                                : null
                )
                .contextSnapshotId(
                        explanation.getContextSnapshot() != null
                                ? explanation
                                .getContextSnapshot()
                                .getId()
                                : null
                )
                .decisionType(
                        explanation.getDecisionType()
                )
                .decision(
                        explanation.getDecision()
                )
                .why(
                        explanation.getWhy()
                )
                .contributingFactors(
                        explanation.getContributingFactors()
                )
                .dataUsed(
                        explanation.getDataUsed()
                )
                .confidence(
                        explanation.getConfidence()
                )
                .uncertainty(
                        explanation.getUncertainty()
                )
                .dataStatus(
                        explanation.getDataStatus()
                )
                .source(
                        explanation.getSource()
                )
                .createdAt(
                        explanation.getCreatedAt()
                )
                .build();
    }
}