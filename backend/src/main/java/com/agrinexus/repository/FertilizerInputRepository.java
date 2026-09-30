package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FertilizerInputRepository
        extends JpaRepository<FertilizerInput, UUID> {

    /*
     * Get all fertilizer/input records for a farm.
     */
    List<FertilizerInput> findByFarmIdOrderByApplicationDateDesc(
            UUID farmId
    );

    /*
     * Get inputs associated with a specific crop.
     */
    List<FertilizerInput> findByFarmIdAndCropIdOrderByApplicationDateDesc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get inputs within a date range.
     */
    List<FertilizerInput>
    findByFarmIdAndApplicationDateBetweenOrderByApplicationDateDesc(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Get inputs by type.
     *
     * Examples:
     * CHEMICAL
     * ORGANIC
     * BIOLOGICAL
     * MANURE
     */
    List<FertilizerInput>
    findByFarmIdAndInputTypeIgnoreCaseOrderByApplicationDateDesc(
            UUID farmId,
            String inputType
    );
}