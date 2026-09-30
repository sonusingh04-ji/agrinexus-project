package com.agrinexus.repository;

import com.agrinexus.entity.FarmerAlert;
import com.agrinexus.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FarmerAlertRepository
        extends JpaRepository<FarmerAlert, UUID> {

    // All alerts for a specific farm
    List<FarmerAlert> findByFarmIdOrderByCreatedAtDesc(
            UUID farmId
    );

    // Unread alerts for a specific farm
    List<FarmerAlert> findByFarmIdAndIsReadFalseOrderByCreatedAtDesc(
            UUID farmId
    );

    // Alerts filtered by risk level
    List<FarmerAlert> findByFarmIdAndRiskLevelOrderByCreatedAtDesc(
            UUID farmId,
            RiskLevel riskLevel
    );

    // Only currently valid alerts
    List<FarmerAlert> findByFarmIdAndValidUntilAfterOrderByCreatedAtDesc(
            UUID farmId,
            LocalDateTime currentTime
    );

    /*
     * Check whether the same active alert already exists.
     *
     * Duplicate prevention is based on:
     * Farm ID + Title + Risk Level + Valid Alert
     */
    @Query("""
            SELECT COUNT(a) > 0
            FROM FarmerAlert a
            WHERE a.farm.id = :farmId
            AND a.title = :title
            AND a.riskLevel = :riskLevel
            AND a.validUntil > :currentTime
            """)
    boolean existsActiveDuplicateAlert(
            @Param("farmId") UUID farmId,
            @Param("title") String title,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("currentTime") LocalDateTime currentTime
    );
    @Transactional
    long deleteByFarmId(UUID farmId);
}