package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IVRResponse {

    private UUID sessionId;

    private UUID farmId;

    private String language;

    private String menu;

    private String spokenText;

    private String responseType;

    /*
     * MENU
     * INFORMATION
     * COPILOT
     * ALERT
     * EXIT
     */
    private String action;

    private Boolean sessionActive;

    private String dataStatus;

    private LocalDateTime respondedAt;
}