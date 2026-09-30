package com.agrinexus.repository;

import com.agrinexus.entity.IrrigationTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IrrigationTelemetryRepository
        extends JpaRepository<IrrigationTelemetry, UUID> {

    /*
     * Get all telemetry records for a farm,
     * newest first.
     */
    List<IrrigationTelemetry>
    findByFarmIdOrderByRecordedAtDesc(
            UUID farmId
    );

    /*
     * Get telemetry records for a specific device.
     */
    List<IrrigationTelemetry>
    findByFarmIdAndDeviceIdOrderByRecordedAtDesc(
            UUID farmId,
            String deviceId
    );

    /*
     * Get telemetry within a time range.
     *
     * Useful for:
     * - irrigation history
     * - moisture trends
     * - pump analysis
     * - solar analysis
     */
    List<IrrigationTelemetry>
    findByFarmIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID farmId,
            LocalDateTime start,
            LocalDateTime end
    );

    /*
     * Get the latest telemetry record for a farm.
     */
    IrrigationTelemetry
    findFirstByFarmIdOrderByRecordedAtDesc(
            UUID farmId
    );

    /*
     * Get the latest telemetry record for a device.
     */
    IrrigationTelemetry
    findFirstByFarmIdAndDeviceIdOrderByRecordedAtDesc(
            UUID farmId,
            String deviceId
    );

    /*
     * Get the latest record where the pump is ON.
     */
    IrrigationTelemetry
    findFirstByFarmIdAndPumpStateOrderByRecordedAtDesc(
            UUID farmId,
            Integer pumpState
    );

    /*
     * Get telemetry where pump was running.
     *
     * pumpState = 1
     */
    List<IrrigationTelemetry>
    findByFarmIdAndPumpStateOrderByRecordedAtDesc(
            UUID farmId,
            Integer pumpState
    );

    /*
     * Get telemetry by sensor status.
     *
     * Examples:
     * ONLINE
     * OFFLINE
     * WARNING
     * CALIBRATION_REQUIRED
     */
    List<IrrigationTelemetry>
    findByFarmIdAndSensorStatusIgnoreCaseOrderByRecordedAtDesc(
            UUID farmId,
            String sensorStatus
    );

    /*
     * Get latest telemetry before or at a specified time.
     *
     * Useful for decision-engine historical lookups.
     */
    IrrigationTelemetry
    findFirstByFarmIdAndRecordedAtLessThanEqualOrderByRecordedAtDesc(
            UUID farmId,
            LocalDateTime recordedAt
    );
}