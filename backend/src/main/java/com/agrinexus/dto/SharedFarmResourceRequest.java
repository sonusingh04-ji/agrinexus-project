package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFarmResourceRequest {

    private UUID cooperativeId;

    /*
     * WEATHER_STATION
     * SOIL_SENSOR
     * WATER_PUMP
     * TRACTOR
     * SPRAYER
     * HARVESTER
     * STORAGE
     * TRANSPORT
     * OTHER
     */
    private String resourceType;

    private String resourceName;

    private UUID ownerFarmerId;

    private String location;

    /*
     * AVAILABLE
     * IN_USE
     * MAINTENANCE
     * INACTIVE
     */
    private String status;

    private Double estimatedTotalCostInr;

    private Double estimatedUsageCostInr;

    /*
     * HOUR
     * DAY
     * USE
     */
    private String usageUnit;

    private String description;

    private String dataStatus;

    private String source;

    private String notes;
}