package com.agrinexus.entity;

import com.agrinexus.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "farmer_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String message;

    @Column(length = 3000)
    private String recommendation;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime validUntil;

    @PrePersist
    protected void onCreate() {

        if (isRead == null) {
            isRead = false;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}