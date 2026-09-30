package com.agrinexus.repository;

import com.agrinexus.entity.NutrientBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NutrientBalanceRepository
        extends JpaRepository<NutrientBalance, UUID> {

    /*
     * Get all nutrient balances for a farm,
     * newest period first.
     */
    List<NutrientBalance>
    findByFarmIdOrderByPeriodStartDesc(
            UUID farmId
    );

    /*
     * Get balances for a specific crop.
     */
    List<NutrientBalance>
    findByFarmIdAndCropIdOrderByPeriodStartDesc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get balances that overlap a requested date range.
     */
    List<NutrientBalance>
    findByFarmIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
            UUID farmId,
            LocalDate periodEnd,
            LocalDate periodStart
    );

    /*
     * Get the latest nutrient balance for a farm.
     */
    NutrientBalance
    findFirstByFarmIdOrderByPeriodEndDesc(
            UUID farmId
    );

    /*
     * Get the latest balance for a specific crop.
     */
    NutrientBalance
    findFirstByFarmIdAndCropIdOrderByPeriodEndDesc(
            UUID farmId,
            UUID cropId
    );
}