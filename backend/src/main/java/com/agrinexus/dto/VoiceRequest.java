package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceRequest {

    private UUID farmId;

    private UUID conversationId;

    /*
     * Recognized speech converted to text.
     *
     * Example:
     * "आज पानी देना है?"
     */
    private String transcript;

    /*
     * EN
     * HI
     */
    private String language;

    /*
     * WEB
     * MOBILE
     * IVR
     * VOICE
     */
    private String channel;
}