package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FertilizerScheduleRepository
        extends JpaRepository<FertilizerSchedule, UUID> {

    /*
     * Get all fertilizer schedules for a farm.
     * Newest planned activity first.
     */
    List<FertilizerSchedule>
    findByFarmIdOrderByPlannedDateAsc(
            UUID farmId
    );

    /*
     * Get schedules for a specific crop.
     */
    List<FertilizerSchedule>
    findByFarmIdAndCropIdOrderByPlannedDateAsc(
            UUID farmId,
            UUID cropId
    );

    /*
     * Get schedules by status.
     *
     * Examples:
     * PLANNED
     * DUE
     * COMPLETED
     * DELAYED
     * CANCELLED
     * REASSESS
     */
    List<FertilizerSchedule>
    findByFarmIdAndStatusIgnoreCaseOrderByPlannedDateAsc(
            UUID farmId,
            String status
    );

    /*
     * Get activities due between two dates.
     */
    List<FertilizerSchedule>
    findByFarmIdAndPlannedDateBetweenOrderByPlannedDateAsc(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Get all schedules for a particular stage.
     */
    List<FertilizerSchedule>
    findByFarmIdAndCropIdAndStageIgnoreCaseOrderByPlannedDateAsc(
            UUID farmId,
            UUID cropId,
            String stage
    );

    /*
     * Get the latest scheduled activity for a farm.
     */
    FertilizerSchedule
    findFirstByFarmIdOrderByPlannedDateDesc(
            UUID farmId
    );

    /*
     * Get the next planned activity for a farm.
     */
    FertilizerSchedule
    findFirstByFarmIdAndPlannedDateGreaterThanEqualOrderByPlannedDateAsc(
            UUID farmId,
            LocalDate date
    );

    /*
     * Get schedules requiring reassessment.
     */
    List<FertilizerSchedule>
    findByFarmIdAndReassessmentRequiredTrueOrderByPlannedDateAsc(
            UUID farmId
    );
}