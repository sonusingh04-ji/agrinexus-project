package com.agrinexus.repository;

import com.agrinexus.entity.LulcObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LulcObservationRepository
        extends JpaRepository<LulcObservation, UUID> {


    /*
     * Latest LULC analysis for a farm.
     */
    Optional<LulcObservation>
    findFirstByFarmIdOrderByAnalyzedAtDesc(
            UUID farmId
    );


    /*
     * Full history.
     */
    List<LulcObservation>
    findByFarmIdOrderByAnalyzedAtDesc(
            UUID farmId
    );


    /*
     * Check whether the farm already has a
     * result for a particular base year.
     */
    Optional<LulcObservation>
    findFirstByFarmIdAndBaseYearOrderByAnalyzedAtDesc(
            UUID farmId,
            Integer baseYear
    );
}