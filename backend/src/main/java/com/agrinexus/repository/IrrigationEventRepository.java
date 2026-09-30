package com.agrinexus.repository;

import com.agrinexus.entity.IrrigationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IrrigationEventRepository
        extends JpaRepository<IrrigationEvent, UUID> {

    List<IrrigationEvent>
    findByFarmIdOrderByStartTimeDesc(UUID farmId);

    List<IrrigationEvent>
    findByFarmIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID farmId,
            LocalDateTime start,
            LocalDateTime end
    );

    IrrigationEvent
    findFirstByFarmIdOrderByStartTimeDesc(UUID farmId);

    List<IrrigationEvent>
    findByFarmIdAndCompletionStatusIgnoreCaseOrderByStartTimeDesc(
            UUID farmId,
            String completionStatus
    );
}