package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FertilizerRecommendationRepository
        extends JpaRepository<FertilizerRecommendation, UUID> {

    /*
     * Get all recommendations for a farm.
     */
    List<FertilizerRecommendation>
    findByFarmIdOrderByGeneratedAtDesc(
            UUID farmId
    );

    /*
     * Get recommendations for a specific crop.
     */
    List<FertilizerRecommendation>
    findByFarmIdAndCropIdOrderByGeneratedAtDesc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get recommendations by status.
     *
     * Examples:
     * READY
     * INSUFFICIENT_DATA
     * REVIEW_REQUIRED
     */
    List<FertilizerRecommendation>
    findByFarmIdAndStatusIgnoreCaseOrderByGeneratedAtDesc(
            UUID farmId,
            String status
    );

    /*
     * Get latest recommendation for a farm.
     */
    FertilizerRecommendation
    findFirstByFarmIdOrderByGeneratedAtDesc(
            UUID farmId
    );

    /*
     * Get latest recommendation for a farm and crop.
     */
    FertilizerRecommendation
    findFirstByFarmIdAndCropIdOrderByGeneratedAtDesc(
            UUID farmId,
            UUID cropId
    );
}