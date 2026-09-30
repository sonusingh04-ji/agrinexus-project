package com.agrinexus.repository;

import com.agrinexus.entity.IrrigationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryIrrigationEventRepository
        extends JpaRepository<IrrigationEvent, UUID> {

    List<IrrigationEvent> findByFarmIdOrderByStartTimeDesc(
            UUID farmId
    );
}