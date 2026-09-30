package com.agrinexus.repository;

import com.agrinexus.entity.CropHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropHistoryRepository
        extends JpaRepository<CropHistory, UUID> {

    /*
     * Get all crop history records for a farm,
     * newest/current records first.
     */
    List<CropHistory> findByFarmIdOrderBySowingDateDesc(
            UUID farmId
    );

    /*
     * Get crop history records for a specific crop.
     */
    List<CropHistory> findByCropIdOrderBySowingDateDesc(
            UUID cropId
    );

    /*
     * Get the latest crop history record for a farm.
     */
    CropHistory findFirstByFarmIdOrderBySowingDateDesc(
            UUID farmId
    );


    CropHistory findFirstByFarmIdAndHarvestDateIsNullOrderBySowingDateDesc(
            UUID farmId
    );

    /*
     * Find records where residue is present.
     */
    List<CropHistory> findByFarmIdAndResiduePresentTrueOrderBySowingDateDesc(
            UUID farmId
    );
}