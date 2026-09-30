package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PumpCompletionResponse {

    private UUID farmId;

    private String deviceId;

    private Integer pumpState;

    private Double currentMoisture;

    private Double targetMoisture;

    private String pumpAction;

    private String message;

    private Boolean targetReached;

    private String dataStatus;

    private LocalDateTime evaluatedAt;
}