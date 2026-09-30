package com.agrinexus.repository;

import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WeatherRiskRepository
        extends JpaRepository<WeatherRisk, UUID> {

    // All risks for a farm
    List<WeatherRisk> findByFarmIdOrderByDetectedAtDesc(
            UUID farmId
    );

    // Risks filtered by risk level
    List<WeatherRisk> findByFarmIdAndRiskLevelOrderByDetectedAtDesc(
            UUID farmId,
            RiskLevel riskLevel
    );

    // Only currently active risks
    List<WeatherRisk> findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
            UUID farmId,
            LocalDateTime currentTime
    );
}