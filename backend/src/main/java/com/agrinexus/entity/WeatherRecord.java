package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;
    @Column(nullable = false)
    private LocalDateTime recordedAt;
    private Double temperature;
    private Double feelsLike;
    private Integer humidity;
    private Double rainfall;
    private Double windSpeed;
    private Integer windDirection;
    private Double pressure;
    private String weatherCondition;
    @Column(length = 1000)
    private String weatherDescription;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
}