package com.agrinexus.repository;

import com.agrinexus.entity.AIFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIFeedbackRepository
        extends JpaRepository<AIFeedback, UUID> {

    List<AIFeedback>
    findByFarmIdOrderByCreatedAtDesc(UUID farmId);

    List<AIFeedback>
    findByStatusOrderByCreatedAtAsc(String status);

    List<AIFeedback>
    findByExplanationIdOrderByCreatedAtDesc(
            UUID explanationId
    );

    List<AIFeedback>
    findByUserIdOrderByCreatedAtDesc(
            UUID userId
    );
}