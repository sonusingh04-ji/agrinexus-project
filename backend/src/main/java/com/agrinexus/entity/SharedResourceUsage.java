package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_resource_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedResourceUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Shared cooperative resource.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private SharedFarmResource resource;

    /*
     * Farmer using the resource.
     */
    @Column(nullable = false)
    private UUID farmerId;

    /*
     * Optional farm receiving the service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    /*
     * Start/end time of use.
     */
    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /*
     * Duration in the resource's usage unit.
     */
    private Double quantityUsed;

    /*
     * Calculated/recorded usage cost.
     */
    private Double usageCostInr;

    /*
     * Optional shared-cost portion.
     */
    private Double memberShareInr;

    /*
     * COMPLETED
     * CANCELLED
     * PLANNED
     */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 1500)
    private String purpose;

    @Column(length = 40)
    private String dataStatus;

    @Column(length = 300)
    private String source;

    @Column(length = 2000)
    private String notes;

    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = "PLANNED";
        }
    }
}