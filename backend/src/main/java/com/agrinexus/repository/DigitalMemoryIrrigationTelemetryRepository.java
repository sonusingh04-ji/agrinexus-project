package com.agrinexus.repository;

import com.agrinexus.entity.IrrigationTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryIrrigationTelemetryRepository
        extends JpaRepository<IrrigationTelemetry, UUID> {

    List<IrrigationTelemetry> findByFarmIdOrderByRecordedAtDesc(
            UUID farmId
    );
}