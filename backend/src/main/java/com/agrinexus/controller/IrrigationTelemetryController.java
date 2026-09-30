package com.agrinexus.controller;

import com.agrinexus.dto.IrrigationTelemetryRequest;
import com.agrinexus.dto.IrrigationTelemetryResponse;
import com.agrinexus.service.IrrigationTelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/irrigation-telemetry")
@RequiredArgsConstructor
public class IrrigationTelemetryController {

    private final IrrigationTelemetryService service;

    /*
     * Store telemetry.
     *
     * POST /api/irrigation-telemetry
     */
    @PostMapping
    public ResponseEntity<IrrigationTelemetryResponse> create(
            @RequestBody IrrigationTelemetryRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    /*
     * Get telemetry by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IrrigationTelemetryResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    /*
     * Get all telemetry for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<IrrigationTelemetryResponse>>
    getFarmTelemetry(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getFarmTelemetry(farmId)
        );
    }

    /*
     * Get telemetry for one device.
     */
    @GetMapping("/farm/{farmId}/device/{deviceId}")
    public ResponseEntity<List<IrrigationTelemetryResponse>>
    getDeviceTelemetry(
            @PathVariable UUID farmId,
            @PathVariable String deviceId
    ) {

        return ResponseEntity.ok(
                service.getDeviceTelemetry(
                        farmId,
                        deviceId
                )
        );
    }

    /*
     * Get telemetry within a time range.
     */
    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<IrrigationTelemetryResponse>>
    getRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime end
    ) {

        return ResponseEntity.ok(
                service.getRange(
                        farmId,
                        start,
                        end
                )
        );
    }

    /*
     * Get latest telemetry.
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<IrrigationTelemetryResponse>
    getLatest(
            @PathVariable UUID farmId
    ) {

        IrrigationTelemetryResponse response =
                service.getLatest(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Delete telemetry by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable UUID id
    ) {

        service.delete(id);

        return ResponseEntity.ok(
                "Irrigation telemetry deleted successfully"
        );
    }
}