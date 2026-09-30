package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FertilizerReferenceRepository
        extends JpaRepository<FertilizerReference, UUID> {

    /*
     * Get verified references for a crop.
     */
    List<FertilizerReference> findByCropIdAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
            UUID cropId,
            String dataStatus
    );

    /*
     * Get references for crop + region.
     */
    List<FertilizerReference>
    findByCropIdAndRegionIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
            UUID cropId,
            String region,
            String dataStatus
    );

    /*
     * Get references for crop + application stage.
     */
    List<FertilizerReference>
    findByCropIdAndApplicationStageIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
            UUID cropId,
            String applicationStage,
            String dataStatus
    );

    /*
     * Get exact crop + region + stage references.
     */
    List<FertilizerReference>
    findByCropIdAndRegionIgnoreCaseAndApplicationStageIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
            UUID cropId,
            String region,
            String applicationStage,
            String dataStatus
    );
}