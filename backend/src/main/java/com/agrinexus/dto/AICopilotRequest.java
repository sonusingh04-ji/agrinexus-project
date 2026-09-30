package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICopilotRequest {

    private UUID farmId;

    /*
     * Existing conversation ID.
     *
     * Null = start a new conversation.
     */
    private UUID conversationId;

    /*
     * Farmer question.
     */
    private String question;

    /*
     * Language:
     *
     * EN
     * HI
     */
    private String language;

    /*
     * Interaction channel:
     *
     * WEB
     * MOBILE
     * VOICE
     * IVR
     * SMS
     */
    private String channel;
}