package com.agrinexus.repository;

import com.agrinexus.entity.AIRecommendationExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIRecommendationExplanationRepository
        extends JpaRepository<AIRecommendationExplanation, UUID> {

    List<AIRecommendationExplanation>
    findByFarmIdOrderByCreatedAtDesc(UUID farmId);

    List<AIRecommendationExplanation>
    findByConversationIdOrderByCreatedAtDesc(
            UUID conversationId
    );

    AIRecommendationExplanation
    findFirstByFarmIdOrderByCreatedAtDesc(
            UUID farmId
    );
}