package com.agrinexus.repository;

import com.agrinexus.entity.CropRotationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CropRotationDecisionRepository
        extends JpaRepository<CropRotationDecision, UUID> {

    /*
     * -------------------------------------------------------------
     * All rotation decisions for a farm.
     * Newest decision first.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Latest rotation decision for a farm.
     * -------------------------------------------------------------
     */
    Optional<CropRotationDecision>
    findFirstByFarmIdOrderByEvaluatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Decisions involving a previous crop.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdAndPreviousCropIdOrderByEvaluatedAtDesc(
            UUID farmId,
            UUID previousCropId
    );

    /*
     * -------------------------------------------------------------
     * Decisions recommending a particular crop.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdAndRecommendedCropIdOrderByEvaluatedAtDesc(
            UUID farmId,
            UUID recommendedCropId
    );

    /*
     * -------------------------------------------------------------
     * Decisions for a target season.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdAndTargetSeasonOrderByEvaluatedAtDesc(
            UUID farmId,
            String targetSeason
    );

    /*
     * -------------------------------------------------------------
     * Decisions by data status.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdAndDataStatusOrderByEvaluatedAtDesc(
            UUID farmId,
            String dataStatus
    );

    /*
     * -------------------------------------------------------------
     * Decisions by methodology version.
     * Useful for scientific auditing.
     * -------------------------------------------------------------
     */
    List<CropRotationDecision>
    findByFarmIdAndMethodologyVersionOrderByEvaluatedAtDesc(
            UUID farmId,
            String methodologyVersion
    );
}