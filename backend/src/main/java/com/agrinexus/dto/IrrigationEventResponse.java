package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationEventResponse {

    private UUID id;

    private UUID farmId;

    private String deviceId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double durationMinutes;

    private Double volumeLiters;

    private String triggerReason;

    private String completionStatus;

    private Double moistureBefore;

    private Double moistureAfter;

    private Double targetMoisture;

    private Boolean pumpWasOn;

    private String notes;
}