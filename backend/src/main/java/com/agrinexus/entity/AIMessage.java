package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Conversation containing this message.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AIConversation conversation;

    /*
     * Message sender.
     *
     * USER
     * AI
     * SYSTEM
     */
    @Column(nullable = false, length = 20)
    private String role;

    /*
     * User's question or AI's answer.
     */
    @Column(nullable = false, length = 10000)
    private String content;

    /*
     * Language of this particular message.
     */
    @Column(length = 20)
    private String language;

    /*
     * Interaction channel.
     *
     * WEB
     * MOBILE
     * IVR
     * VOICE
     * SMS
     */
    @Column(length = 30)
    private String channel;

    /*
     * Message ordering inside the conversation.
     */
    @Column(nullable = false)
    private Integer sequenceNumber;

    /*
     * Whether the AI message was generated using
     * stored decision-engine context.
     */
    private Boolean decisionContextUsed;

    /*
     * Optional response confidence.
     *
     * 0.0 - 1.0
     */
    private Double confidence;

    /*
     * Source/provenance.
     *
     * RULE_ENGINE
     * KNOWLEDGE_REFERENCE
     * AI_MODEL
     * TEST
     */
    @Column(length = 40)
    private String source;

    /*
     * Whether this message requires human/expert
     * follow-up.
     */
    private Boolean requiresReview;

    /*
     * Message timestamp.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}