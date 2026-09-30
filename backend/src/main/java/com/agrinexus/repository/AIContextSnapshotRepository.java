package com.agrinexus.repository;

import com.agrinexus.entity.AIContextSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIContextSnapshotRepository
        extends JpaRepository<AIContextSnapshot, UUID> {

    List<AIContextSnapshot>
    findByConversationIdOrderByCapturedAtDesc(
            UUID conversationId
    );

    AIContextSnapshot
    findFirstByConversationIdOrderByCapturedAtDesc(
            UUID conversationId
    );

    List<AIContextSnapshot>
    findByFarmIdOrderByCapturedAtDesc(
            UUID farmId
    );

    AIContextSnapshot
    findFirstByMessageId(
            UUID messageId
    );
}