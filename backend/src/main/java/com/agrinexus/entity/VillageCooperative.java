package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "village_cooperatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VillageCooperative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Cooperative/group name.
     *
     * Example:
     * Mathura Water-Smart Farmers Group
     */
    @Column(nullable = false, length = 200)
    private String name;

    /*
     * Village/locality.
     */
    @Column(length = 200)
    private String village;

    /*
     * District.
     */
    @Column(length = 100)
    private String district;

    /*
     * State.
     */
    @Column(length = 100)
    private String state;

    /*
     * Optional contact description.
     */
    @Column(length = 500)
    private String contactInfo;

    /*
     * Farmer who created/coordinates the group.
     *
     * This references the existing farmer ID rather than
     * creating a duplicate Farmer entity.
     */
    @Column(nullable = false)
    private UUID coordinatorFarmerId;

    /*
     * ACTIVE
     * INACTIVE
     */
    @Column(nullable = false, length = 30)
    private String status;

    /*
     * Data provenance.
     */
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
            status = "ACTIVE";
        }
    }
}