package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedResourceUsageRequest {

    private UUID resourceId;

    private UUID farmerId;

    private UUID farmId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double quantityUsed;

    private Double usageCostInr;

    private Double memberShareInr;

    /*
     * COMPLETED
     * CANCELLED
     * PLANNED
     */
    private String status;

    private String purpose;

    private String dataStatus;

    private String source;

    private String notes;
}