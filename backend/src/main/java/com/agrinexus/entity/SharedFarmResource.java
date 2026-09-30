package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_farm_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFarmResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Cooperative owning/managing the resource.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cooperative_id", nullable = false)
    private VillageCooperative cooperative;

    /*
     * Resource examples:
     *
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
    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(nullable = false, length = 200)
    private String resourceName;

    /*
     * Owning/hosting farmer.
     */
    private UUID ownerFarmerId;

    /*
     * Optional location.
     */
    @Column(length = 300)
    private String location;

    /*
     * AVAILABLE
     * IN_USE
     * MAINTENANCE
     * INACTIVE
     */
    @Column(nullable = false, length = 30)
    private String status;

    /*
     * Estimated total acquisition/rental value
     * when known.
     */
    private Double estimatedTotalCostInr;

    /*
     * Estimated operating cost per hour/day/use.
     */
    private Double estimatedUsageCostInr;

    /*
     * HOUR
     * DAY
     * USE
     */
    @Column(length = 20)
    private String usageUnit;

    @Column(length = 2000)
    private String description;

    @Column(length = 40)
    private String dataStatus;

    @Column(length = 300)
    private String source;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "AVAILABLE";
        }
    }
}