package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "solar_pump_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolarPumpProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false, unique = true)
    private Farm farm;

    private Double panelAreaM2;

    private Double panelEfficiencyPercent;

    private Double ratedSolarPowerKw;

    private Double pumpRatedPowerKw;

    private Double pumpEfficiencyPercent;

    private Double pumpHeadMeters;

    private Double pumpRatedFlowLpm;

    private Double batteryCapacityKwh;

    private Double batterySocPercent;

    private Double tankCapacityLiters;

    private Double currentTankLevelLiters;

    private Double minimumSolarIrradianceWm2;

    @Column(length = 50)
    private String source;

    @Column(length = 30)
    private String dataStatus;

    @Column(length = 2500)
    private String notes;
}