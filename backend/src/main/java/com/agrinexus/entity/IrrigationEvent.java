package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "irrigation_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(length = 64)
    private String deviceId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double durationMinutes;

    private Double volumeLiters;

    @Column(length = 50)
    private String triggerReason;

    @Column(length = 50)
    private String completionStatus;

    private Double moistureBefore;

    private Double moistureAfter;

    private Double targetMoisture;

    private Boolean pumpWasOn;

    @Column(length = 3000)
    private String notes;
}