package com.agrinexus.entity;

import com.agrinexus.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "combined_risk_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CombinedRiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false)
    private Double combinedRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(length = 3000)
    private String analysis;

    @Column(length = 3000)
    private String recommendation;

    private Double ndvi;

    private Double soilMoisture;

    private Double temperature;

    private Double rainfall;

    private Double windSpeed;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {

        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }
}