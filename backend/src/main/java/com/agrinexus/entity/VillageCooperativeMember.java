package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "village_cooperative_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cooperative_farmer",
                        columnNames = {
                                "cooperative_id",
                                "farmer_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VillageCooperativeMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Cooperative to which the farmer belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cooperative_id", nullable = false)
    private VillageCooperative cooperative;

    /*
     * Existing farmer identifier.
     */
    @Column(nullable = false)
    private UUID farmerId;

    /*
     * Optional farm belonging to this farmer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    /*
     * MEMBER
     * COORDINATOR
     */
    @Column(nullable = false, length = 30)
    private String role;

    /*
     * ACTIVE
     * INACTIVE
     */
    @Column(nullable = false, length = 30)
    private String status;

    private LocalDateTime joinedAt;

    @Column(length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {

        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }

        if (role == null) {
            role = "MEMBER";
        }

        if (status == null) {
            status = "ACTIVE";
        }
    }
}