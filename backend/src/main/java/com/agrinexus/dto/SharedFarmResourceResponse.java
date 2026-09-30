package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFarmResourceResponse {

    private UUID id;

    private UUID cooperativeId;

    private String resourceType;

    private String resourceName;

    private UUID ownerFarmerId;

    private String location;

    private String status;

    private Double estimatedTotalCostInr;

    private Double estimatedUsageCostInr;

    private String usageUnit;

    private String description;

    private String dataStatus;

    private String source;

    private String notes;

    private LocalDateTime createdAt;
}