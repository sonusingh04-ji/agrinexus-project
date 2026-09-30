package com.agrinexus.repository;

import com.agrinexus.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIConversationRepository
        extends JpaRepository<AIConversation, UUID> {

    List<AIConversation>
    findByFarmIdOrderByLastMessageAtDesc(
            UUID farmId
    );

    List<AIConversation>
    findByFarmIdAndStatusOrderByLastMessageAtDesc(
            UUID farmId,
            String status
    );

    AIConversation
    findFirstByFarmIdAndStatusOrderByLastMessageAtDesc(
            UUID farmId,
            String status
    );

    List<AIConversation>
    findByUserIdOrderByLastMessageAtDesc(
            UUID userId
    );
}