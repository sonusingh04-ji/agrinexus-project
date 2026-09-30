package com.agrinexus.repository;

import com.agrinexus.entity.FarmSustainabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmSustainabilityScoreRepository
        extends JpaRepository<FarmSustainabilityScore, UUID> {

    /*
     * -------------------------------------------------------------
     * Get all sustainability scores for a farm.
     * Newest score first.
     * -------------------------------------------------------------
     */
    List<FarmSustainabilityScore>
    findByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get latest sustainability score for a farm.
     * -------------------------------------------------------------
     */
    Optional<FarmSustainabilityScore>
    findFirstByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get scores for a particular crop.
     * -------------------------------------------------------------
     */
    List<FarmSustainabilityScore>
    findByCropIdOrderByEvaluatedAtDesc(
            UUID cropId
    );

    /*
     * -------------------------------------------------------------
     * Get scores by sustainability level.
     *
     * Examples:
     * EXCELLENT
     * GOOD
     * MODERATE
     * NEEDS_IMPROVEMENT
     * CRITICAL
     * -------------------------------------------------------------
     */
    List<FarmSustainabilityScore>
    findByFarmIdAndSustainabilityLevelOrderByEvaluatedAtDesc(
            UUID farmId,
            String sustainabilityLevel
    );

    /*
     * -------------------------------------------------------------
     * Get scores by data status.
     *
     * TEST
     * CALCULATED
     * MIXED
     * -------------------------------------------------------------
     */
    List<FarmSustainabilityScore>
    findByFarmIdAndDataStatusOrderByEvaluatedAtDesc(
            UUID farmId,
            String dataStatus
    );

    /*
     * -------------------------------------------------------------
     * Get scores by methodology version.
     * Useful for scientific auditability.
     * -------------------------------------------------------------
     */
    List<FarmSustainabilityScore>
    findByFarmIdAndMethodologyVersionOrderByEvaluatedAtDesc(
            UUID farmId,
            String methodologyVersion
    );
}