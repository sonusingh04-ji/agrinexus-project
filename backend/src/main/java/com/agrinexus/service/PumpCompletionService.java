package com.agrinexus.service;

import com.agrinexus.dto.PumpCompletionResponse;
import com.agrinexus.entity.IrrigationConfig;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.repository.IrrigationConfigRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PumpCompletionService {

    private final IrrigationConfigRepository configRepository;
    private final IrrigationTelemetryRepository telemetryRepository;

    public PumpCompletionResponse evaluate(
            UUID farmId
    ) {

        IrrigationTelemetry telemetry =
                telemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        if (telemetry == null) {

            throw new RuntimeException(
                    "No irrigation telemetry available"
            );
        }

        IrrigationConfig config =
                configRepository.findByFarmId(farmId);

        Double fc =
                config != null
                        ? config.getFieldCapacity()
                        : null;

        Double targetFraction =
                config != null
                        && config.getTargetFieldCapacityFraction() != null
                        ? config.getTargetFieldCapacityFraction()
                        : 0.95;

        Double target =
                fc != null
                        ? fc * targetFraction
                        : null;

        /*
         * Prefer 30 cm root-zone measurement.
         */
        Double moisture =
                telemetry.getVwc30cm();

        if (moisture == null) {
            moisture = telemetry.getVwc15cm();
        }

        if (moisture == null) {
            moisture = telemetry.getVwc60cm();
        }

        boolean pumpOn =
                telemetry.getPumpState() != null
                        && telemetry.getPumpState() == 1;

        /*
         * Resolve provenance from the actual telemetry source.
         */
        String dataStatus =
                resolveDataStatus(telemetry);

        /*
         * Pump is OFF.
         */
        if (!pumpOn) {

            return PumpCompletionResponse.builder()
                    .farmId(farmId)
                    .deviceId(telemetry.getDeviceId())
                    .pumpState(telemetry.getPumpState())
                    .currentMoisture(moisture)
                    .targetMoisture(target)
                    .pumpAction("KEEP_OFF")
                    .message("Pump system is idle.")
                    .targetReached(false)
                    .dataStatus(dataStatus)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        /*
         * Pump is ON and target has been reached.
         */
        if (target != null
                && moisture != null
                && moisture >= target) {

            return PumpCompletionResponse.builder()
                    .farmId(farmId)
                    .deviceId(telemetry.getDeviceId())
                    .pumpState(telemetry.getPumpState())
                    .currentMoisture(moisture)
                    .targetMoisture(target)
                    .pumpAction("TRIGGER_SHUTDOWN")
                    .message(
                            "Irrigation target reached—check and switch off the pump."
                    )
                    .targetReached(true)
                    .dataStatus(dataStatus)
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        /*
         * Pump is ON but target has not been reached.
         */
        return PumpCompletionResponse.builder()
                .farmId(farmId)
                .deviceId(telemetry.getDeviceId())
                .pumpState(telemetry.getPumpState())
                .currentMoisture(moisture)
                .targetMoisture(target)
                .pumpAction("KEEP_RUNNING")
                .message("Irrigation is in progress.")
                .targetReached(false)
                .dataStatus(dataStatus)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    /*
     * Resolve data provenance.
     */
    private String resolveDataStatus(
            IrrigationTelemetry telemetry
    ) {

        if (telemetry == null) {
            return "NOT_AVAILABLE";
        }

        if ("TEST".equalsIgnoreCase(
                telemetry.getSensorStatus()
        )
                || "TEST_SENSOR".equalsIgnoreCase(
                telemetry.getSource()
        )) {

            return "TEST";
        }

        if ("SENSOR".equalsIgnoreCase(
                telemetry.getSource()
        )
                || "IOT_SENSOR".equalsIgnoreCase(
                telemetry.getSource()
        )) {

            return "MEASURED";
        }

        if ("CALCULATED".equalsIgnoreCase(
                telemetry.getSource()
        )) {

            return "CALCULATED";
        }

        if ("ESTIMATED".equalsIgnoreCase(
                telemetry.getSource()
        )) {

            return "ESTIMATED";
        }

        return "UNKNOWN";
    }
}