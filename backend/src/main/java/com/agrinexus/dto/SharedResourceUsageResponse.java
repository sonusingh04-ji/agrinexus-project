package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedResourceUsageResponse {

    private UUID id;

    private UUID resourceId;

    private String resourceName;

    private String resourceType;

    private UUID cooperativeId;

    private UUID farmerId;

    private UUID farmId;

    private String farmName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double quantityUsed;

    private Double usageCostInr;

    private Double memberShareInr;

    private String status;

    private String purpose;

    private String dataStatus;

    private String source;

    private String notes;
}