package com.agrinexus.repository;

import com.agrinexus.entity.SowingAdvisorDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SowingAdvisorDecisionRepository
        extends JpaRepository<SowingAdvisorDecision, UUID> {

    /*
     * -------------------------------------------------------------
     * Get all sowing decisions for a farm.
     * Newest first.
     * -------------------------------------------------------------
     */
    List<SowingAdvisorDecision>
    findByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get the latest sowing decision for a farm.
     * -------------------------------------------------------------
     */
    Optional<SowingAdvisorDecision>
    findFirstByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get sowing decisions for a particular crop.
     * -------------------------------------------------------------
     */
    List<SowingAdvisorDecision>
    findByCropIdOrderByEvaluatedAtDesc(
            UUID cropId
    );

    /*
     * -------------------------------------------------------------
     * Get decisions by final decision.
     *
     * Examples:
     * SOW_NOW
     * WAIT
     * UNSUITABLE
     * INSUFFICIENT_DATA
     * -------------------------------------------------------------
     */
    List<SowingAdvisorDecision>
    findByFarmIdAndDecisionOrderByEvaluatedAtDesc(
            UUID farmId,
            String decision
    );

    /*
     * -------------------------------------------------------------
     * Get decisions by data status.
     *
     * TEST
     * CALCULATED
     * MIXED
     * OFFICIAL
     * -------------------------------------------------------------
     */
    List<SowingAdvisorDecision>
    findByFarmIdAndDataStatusOrderByEvaluatedAtDesc(
            UUID farmId,
            String dataStatus
    );

    /*
     * -------------------------------------------------------------
     * Get decisions by methodology version.
     * Useful for scientific auditability.
     * -------------------------------------------------------------
     */
    List<SowingAdvisorDecision>
    findByFarmIdAndMethodologyVersionOrderByEvaluatedAtDesc(
            UUID farmId,
            String methodologyVersion
    );
}