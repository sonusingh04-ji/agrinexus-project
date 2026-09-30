package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IVRRequest {

    private UUID farmId;

    private UUID sessionId;

    private String callerReference;

    private String language;

    /*
     * DTMF digit:
     *
     * 1 = Today
     * 2 = Weather
     * 3 = Water
     * 4 = Crop
     * 5 = Ask AgriNexus
     * 0 = Exit
     */
    private String digit;

    /*
     * Used when the caller asks a direct question.
     */
    private String question;
}