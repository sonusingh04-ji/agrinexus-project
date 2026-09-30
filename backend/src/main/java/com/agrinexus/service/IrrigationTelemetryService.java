package com.agrinexus.service;

import com.agrinexus.dto.IrrigationTelemetryRequest;
import com.agrinexus.dto.IrrigationTelemetryResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IrrigationTelemetryService {

    private final IrrigationTelemetryRepository telemetryRepository;
    private final FarmRepository farmRepository;

    /*
     * Store one telemetry reading.
     */
    public IrrigationTelemetryResponse create(
            IrrigationTelemetryRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : LocalDateTime.now();

        IrrigationTelemetry telemetry =
                IrrigationTelemetry.builder()
                        .farm(farm)
                        .deviceId(request.getDeviceId())
                        .recordedAt(recordedAt)

                        .vwc15cm(
                                request.getVwc15cm()
                        )

                        .vwc30cm(
                                request.getVwc30cm()
                        )

                        .vwc60cm(
                                request.getVwc60cm()
                        )

                        .pumpState(
                                request.getPumpState()
                        )

                        .flowRateLpm(
                                request.getFlowRateLpm()
                        )

                        .linePressureBar(
                                request.getLinePressureBar()
                        )

                        .waterTableDepthM(
                                request.getWaterTableDepthM()
                        )

                        .pumpRuntimeMinutes(
                                request.getPumpRuntimeMinutes()
                        )

                        .rainfallMm(
                                request.getRainfallMm()
                        )

                        .et0Mm(
                                request.getEt0Mm()
                        )

                        .etcMm(
                                request.getEtcMm()
                        )

                        .solarIrradianceWm2(
                                request.getSolarIrradianceWm2()
                        )

                        .solarPowerKw(
                                request.getSolarPowerKw()
                        )

                        .cloudCoverPercent(
                                request.getCloudCoverPercent()
                        )

                        .storageLevelPercent(
                                request.getStorageLevelPercent()
                        )

                        .batterySocPercent(
                                request.getBatterySocPercent()
                        )

                        .cropStage(
                                request.getCropStage()
                        )

                        .kc(
                                request.getKc()
                        )

                        .source(
                                request.getSource()
                        )

                        .sensorStatus(
                                request.getSensorStatus()
                        )

                        .payloadReference(
                                request.getPayloadReference()
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();

        /*
         * Optional calculation:
         *
         * ETc = Kc × ET0
         *
         * We calculate it only when both values exist
         * and the caller did not provide ETc.
         */
        if (telemetry.getEtcMm() == null
                && telemetry.getEt0Mm() != null
                && telemetry.getKc() != null) {

            telemetry.setEtcMm(
                    telemetry.getEt0Mm()
                            * telemetry.getKc()
            );
        }

        return toResponse(
                telemetryRepository.save(telemetry)
        );
    }

    /*
     * Get telemetry by ID.
     */
    public IrrigationTelemetryResponse getById(
            UUID id
    ) {

        IrrigationTelemetry telemetry =
                telemetryRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Irrigation telemetry not found with ID: "
                                                + id
                                )
                        );

        return toResponse(telemetry);
    }

    /*
     * Get all telemetry for a farm.
     */
    public List<IrrigationTelemetryResponse> getFarmTelemetry(
            UUID farmId
    ) {

        verifyFarm(farmId);

        return telemetryRepository
                .findByFarmIdOrderByRecordedAtDesc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get telemetry for a specific device.
     */
    public List<IrrigationTelemetryResponse> getDeviceTelemetry(
            UUID farmId,
            String deviceId
    ) {

        verifyFarm(farmId);

        if (deviceId == null
                || deviceId.isBlank()) {

            throw new RuntimeException(
                    "Device ID is required"
            );
        }

        return telemetryRepository
                .findByFarmIdAndDeviceIdOrderByRecordedAtDesc(
                        farmId,
                        deviceId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get telemetry within a time range.
     */
    public List<IrrigationTelemetryResponse> getRange(
            UUID farmId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        verifyFarm(farmId);

        if (start == null || end == null) {

            throw new RuntimeException(
                    "Start and end time are required"
            );
        }

        if (start.isAfter(end)) {

            throw new RuntimeException(
                    "Start time cannot be after end time"
            );
        }

        return telemetryRepository
                .findByFarmIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        farmId,
                        start,
                        end
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * Get latest telemetry.
     */
    public IrrigationTelemetryResponse getLatest(
            UUID farmId
    ) {

        verifyFarm(farmId);

        IrrigationTelemetry telemetry =
                telemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        if (telemetry == null) {
            return null;
        }

        return toResponse(telemetry);
    }

    /*
     * Delete telemetry.
     *
     * Useful for removing incorrect demo/test records.
     */
    public void delete(
            UUID id
    ) {

        if (!telemetryRepository.existsById(id)) {

            throw new RuntimeException(
                    "Irrigation telemetry not found with ID: "
                            + id
            );
        }

        telemetryRepository.deleteById(id);
    }

    /*
     * Validation.
     */
    private void validate(
            IrrigationTelemetryRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }

        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getPumpState() != null
                && request.getPumpState() != 0
                && request.getPumpState() != 1) {

            throw new RuntimeException(
                    "Pump state must be 0 or 1"
            );
        }

        validatePercentage(
                request.getVwc15cm(),
                "VWC 15cm"
        );

        validatePercentage(
                request.getVwc30cm(),
                "VWC 30cm"
        );

        validatePercentage(
                request.getVwc60cm(),
                "VWC 60cm"
        );

        validatePercentage(
                request.getCloudCoverPercent(),
                "Cloud cover"
        );

        validatePercentage(
                request.getStorageLevelPercent(),
                "Storage level"
        );

        validatePercentage(
                request.getBatterySocPercent(),
                "Battery SoC"
        );

        validateNonNegative(
                request.getFlowRateLpm(),
                "Flow rate"
        );

        validateNonNegative(
                request.getLinePressureBar(),
                "Line pressure"
        );

        validateNonNegative(
                request.getWaterTableDepthM(),
                "Water-table depth"
        );

        validateNonNegative(
                request.getPumpRuntimeMinutes(),
                "Pump runtime"
        );

        validateNonNegative(
                request.getRainfallMm(),
                "Rainfall"
        );

        validateNonNegative(
                request.getEt0Mm(),
                "ET0"
        );

        validateNonNegative(
                request.getEtcMm(),
                "ETc"
        );

        validateNonNegative(
                request.getSolarIrradianceWm2(),
                "Solar irradiance"
        );

        validateNonNegative(
                request.getSolarPowerKw(),
                "Solar power"
        );

        validateNonNegative(
                request.getKc(),
                "Kc"
        );
    }

    private void validatePercentage(
            Double value,
            String field
    ) {

        if (value != null
                && (value < 0 || value > 100)) {

            throw new RuntimeException(
                    field
                            + " must be between 0 and 100"
            );
        }
    }

    private void validateNonNegative(
            Double value,
            String field
    ) {

        if (value != null && value < 0) {

            throw new RuntimeException(
                    field + " cannot be negative"
            );
        }
    }

    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    private IrrigationTelemetryResponse toResponse(
            IrrigationTelemetry telemetry
    ) {

        return IrrigationTelemetryResponse.builder()
                .id(telemetry.getId())
                .farmId(
                        telemetry.getFarm() != null
                                ? telemetry.getFarm().getId()
                                : null
                )
                .deviceId(
                        telemetry.getDeviceId()
                )
                .recordedAt(
                        telemetry.getRecordedAt()
                )
                .vwc15cm(
                        telemetry.getVwc15cm()
                )
                .vwc30cm(
                        telemetry.getVwc30cm()
                )
                .vwc60cm(
                        telemetry.getVwc60cm()
                )
                .pumpState(
                        telemetry.getPumpState()
                )
                .flowRateLpm(
                        telemetry.getFlowRateLpm()
                )
                .linePressureBar(
                        telemetry.getLinePressureBar()
                )
                .waterTableDepthM(
                        telemetry.getWaterTableDepthM()
                )
                .pumpRuntimeMinutes(
                        telemetry.getPumpRuntimeMinutes()
                )
                .rainfallMm(
                        telemetry.getRainfallMm()
                )
                .et0Mm(
                        telemetry.getEt0Mm()
                )
                .etcMm(
                        telemetry.getEtcMm()
                )
                .solarIrradianceWm2(
                        telemetry.getSolarIrradianceWm2()
                )
                .solarPowerKw(
                        telemetry.getSolarPowerKw()
                )
                .cloudCoverPercent(
                        telemetry.getCloudCoverPercent()
                )
                .storageLevelPercent(
                        telemetry.getStorageLevelPercent()
                )
                .batterySocPercent(
                        telemetry.getBatterySocPercent()
                )
                .cropStage(
                        telemetry.getCropStage()
                )
                .kc(
                        telemetry.getKc()
                )
                .source(
                        telemetry.getSource()
                )
                .sensorStatus(
                        telemetry.getSensorStatus()
                )
                .payloadReference(
                        telemetry.getPayloadReference()
                )
                .notes(
                        telemetry.getNotes()
                )
                .build();
    }
}