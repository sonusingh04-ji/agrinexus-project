package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryFertilizerScheduleRepository
        extends JpaRepository<FertilizerSchedule, UUID> {

    List<FertilizerSchedule>
    findByFarmIdOrderByPlannedDateDesc(
            UUID farmId
    );
}