package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "water_storage_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaterStorageProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false, unique = true)
    private Farm farm;

    private Double catchmentAreaM2;

    private Double slopePercent;

    private Double runoffCoefficient;

    private Double infiltrationRateMmPerHour;

    private Double storageCapacityLiters;

    private Double currentStorageLiters;

    private Double storageSafetyLimitPercent;

    @Column(length = 50)
    private String storageType;

    @Column(length = 50)
    private String source;

    @Column(length = 30)
    private String dataStatus;

    @Column(length = 2000)
    private String notes;
}