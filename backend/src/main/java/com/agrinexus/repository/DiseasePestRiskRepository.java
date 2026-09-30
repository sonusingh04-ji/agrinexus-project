package com.agrinexus.repository;

import com.agrinexus.entity.DiseasePestRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiseasePestRiskRepository
        extends JpaRepository<DiseasePestRisk, UUID> {

    List<DiseasePestRisk>
    findByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    List<DiseasePestRisk>
    findByFarmIdAndRiskLevelOrderByEvaluatedAtDesc(
            UUID farmId,
            String riskLevel
    );

    List<DiseasePestRisk>
    findByCropIdOrderByEvaluatedAtDesc(
            UUID cropId
    );

    List<DiseasePestRisk>
    findByFarmIdAndForecastBasedOrderByEvaluatedAtDesc(
            UUID farmId,
            Boolean forecastBased
    );
}