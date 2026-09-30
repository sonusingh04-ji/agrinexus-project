package com.agrinexus.repository;

import com.agrinexus.entity.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AIMessageRepository
        extends JpaRepository<AIMessage, UUID> {

    List<AIMessage>
    findByConversationIdOrderBySequenceNumberAsc(
            UUID conversationId
    );

    AIMessage
    findFirstByConversationIdOrderBySequenceNumberDesc(
            UUID conversationId
    );

    List<AIMessage>
    findByConversationIdAndRoleOrderBySequenceNumberAsc(
            UUID conversationId,
            String role
    );
}