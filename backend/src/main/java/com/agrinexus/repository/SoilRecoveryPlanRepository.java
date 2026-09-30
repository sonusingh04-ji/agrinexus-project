package com.agrinexus.repository;

import com.agrinexus.entity.SoilRecoveryPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoilRecoveryPlanRepository
        extends JpaRepository<SoilRecoveryPlan, UUID> {

    /*
     * Get all recovery plans for a farm.
     * Earlier plans first by season number.
     */
    List<SoilRecoveryPlan>
    findByFarmIdOrderBySeasonNumberAsc(
            UUID farmId
    );

    /*
     * Get recovery plans for a specific crop.
     */
    List<SoilRecoveryPlan>
    findByFarmIdAndCropIdOrderBySeasonNumberAsc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get plans by status.
     *
     * Examples:
     * PLANNED
     * ACTIVE
     * COMPLETED
     * REVIEW_REQUIRED
     * PAUSED
     */
    List<SoilRecoveryPlan>
    findByFarmIdAndStatusIgnoreCaseOrderBySeasonNumberAsc(
            UUID farmId,
            String status
    );

    /*
     * Get plans by focus area.
     *
     * Examples:
     * ORGANIC_CARBON
     * NITROGEN_BALANCE
     * PHOSPHORUS_BALANCE
     * POTASSIUM_BALANCE
     * SOIL_PH
     * NUTRIENT_BALANCE
     */
    List<SoilRecoveryPlan>
    findByFarmIdAndFocusAreaIgnoreCaseOrderBySeasonNumberAsc(
            UUID farmId,
            String focusArea
    );

    /*
     * Get plans by trend.
     *
     * Examples:
     * IMPROVING
     * STABLE
     * DECLINING
     * INSUFFICIENT_DATA
     */
    List<SoilRecoveryPlan>
    findByFarmIdAndTrendIgnoreCaseOrderBySeasonNumberAsc(
            UUID farmId,
            String trend
    );

    /*
     * Get the latest/recent recovery plan for a farm.
     */
    SoilRecoveryPlan
    findFirstByFarmIdOrderBySeasonNumberDesc(
            UUID farmId
    );

    /*
     * Get the latest/recent recovery plan for
     * a particular crop.
     */
    SoilRecoveryPlan
    findFirstByFarmIdAndCropIdOrderBySeasonNumberDesc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get active recovery plans.
     */
    List<SoilRecoveryPlan>
    findByFarmIdAndStatusIgnoreCase(
            UUID farmId,
            String status
    );
}