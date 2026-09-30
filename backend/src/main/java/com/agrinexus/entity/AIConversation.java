package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm whose context is used by this conversation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Optional farmer/user identifier.
     *
     * We keep this as UUID so it can later connect
     * to the existing User entity without forcing
     * a new relationship before we verify that model.
     */
    private UUID userId;

    /*
     * Language used by the conversation.
     *
     * Examples:
     * EN
     * HI
     */
    @Column(length = 20)
    private String language;

    /*
     * Interaction channel.
     *
     * Examples:
     * WEB
     * MOBILE
     * IVR
     * VOICE
     * SMS
     */
    @Column(length = 30)
    private String channel;

    /*
     * Current conversation status.
     *
     * ACTIVE
     * COMPLETED
     * ARCHIVED
     */
    @Column(nullable = false, length = 30)
    private String status;

    /*
     * Short title generated from the conversation.
     */
    @Column(length = 300)
    private String title;

    /*
     * When the conversation started.
     */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /*
     * Last interaction time.
     */
    private LocalDateTime lastMessageAt;

    /*
     * Additional metadata.
     */
    @Column(length = 2000)
    private String notes;
}