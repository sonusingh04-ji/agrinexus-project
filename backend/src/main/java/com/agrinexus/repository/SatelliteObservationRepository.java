package com.agrinexus.repository;

import com.agrinexus.entity.SatelliteObservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SatelliteObservationRepository
        extends JpaRepository<SatelliteObservation, UUID> {

    /*
     * All observations for a farm,
     * newest first.
     */
    List<SatelliteObservation>
    findByFarmIdOrderByObservedAtDesc(
            UUID farmId
    );

    /*
     * Latest observation for a farm.
     */
    Optional<SatelliteObservation>
    findFirstByFarmIdOrderByObservedAtDesc(
            UUID farmId
    );

    /*
     * Observations associated with a crop.
     */
    List<SatelliteObservation>
    findByCropIdOrderByObservedAtDesc(
            UUID cropId
    );

    /*
     * Observations from a particular source.
     */
    List<SatelliteObservation>
    findByFarmIdAndDataSourceOrderByObservedAtDesc(
            UUID farmId,
            String dataSource
    );

    /*
     * Observations by data status.
     *
     * Examples:
     * TEST
     * EXTERNAL
     * OFFICIAL
     * CALCULATED
     */
    List<SatelliteObservation>
    findByFarmIdAndDataStatusOrderByObservedAtDesc(
            UUID farmId,
            String dataStatus
    );

    /*
     * Observations between two timestamps.
     */
    List<SatelliteObservation>
    findByFarmIdAndObservedAtBetweenOrderByObservedAtAsc(
            UUID farmId,
            LocalDateTime start,
            LocalDateTime end
    );

    /*
     * Latest observation for a farm and source.
     */
    Optional<SatelliteObservation>
    findFirstByFarmIdAndDataSourceOrderByObservedAtDesc(
            UUID farmId,
            String dataSource
    );

    /*
     * Observations by crop condition.
     */
    List<SatelliteObservation>
    findByFarmIdAndCropConditionOrderByObservedAtDesc(
            UUID farmId,
            String cropCondition
    );

    /*
     * Observations showing declining vegetation.
     */
    List<SatelliteObservation>
    findByFarmIdAndChangeStatusOrderByObservedAtDesc(
            UUID farmId,
            String changeStatus
    );
}