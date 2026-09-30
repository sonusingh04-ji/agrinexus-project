package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID farmerId;

    @Column(nullable = false)
    private String name;

    /*
     * Farm area in acres.
     */
    private Double areaAcres;

    private String location;

    /** Structured geography used by India-wide reference resolution. */
    @Column(length = 100)
    private String stateName;

    @Column(length = 150)
    private String districtName;

    @Column(length = 150)
    private String subDistrictName;

    private Double latitude;

    private Double longitude;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}