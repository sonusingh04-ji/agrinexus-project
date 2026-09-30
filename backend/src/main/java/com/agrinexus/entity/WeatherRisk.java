package com.agrinexus.entity;

import com.agrinexus.enums.RiskLevel;
import com.agrinexus.enums.RiskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_risks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskType riskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(length = 3000)
    private String recommendation;

    @Column(nullable = false)
    private Double riskScore;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime validUntil;

    /*
     * Indicates whether this risk was detected from:
     * CURRENT weather data or FORECAST weather data.
     */
    @Column(nullable = false)
    private Boolean forecastRisk = false;

    /*
     * The future date/time for which the predicted
     * weather risk is expected.
     */
    private LocalDateTime forecastFor;

    @PrePersist
    protected void onCreate() {

        if (this.detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }

        if (this.forecastRisk == null) {
            this.forecastRisk = false;
        }
    }
}