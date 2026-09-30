package com.agrinexus.controller;

import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.service.SatelliteObservationService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/satellite-observations")
@RequiredArgsConstructor
public class SatelliteObservationController {

    private final SatelliteObservationService observationService;

    @PostMapping
    public ResponseEntity<SatelliteObservation> create(
            @RequestBody SatelliteObservationRequest request
    ) {

        SatelliteObservation observation =
                observationService.create(
                        request.getFarmId(),
                        request.getCropId(),
                        request.getObservedAt(),
                        request.getSatelliteSource(),
                        request.getSceneId(),
                        request.getNdvi(),
                        request.getNdwi(),
                        request.getCloudCoverPercent(),
                        request.getSpatialResolutionMeters(),
                        request.getDataStatus(),
                        request.getDataSource(),
                        request.getConfidence(),
                        request.getForecastBased()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(observation);
    }


    /*
     * -------------------------------------------------------------
     * Get latest observation for a farm
     *
     * GET /api/satellite-observations/farm/{farmId}/latest
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<SatelliteObservation> getLatest(
            @PathVariable UUID farmId
    ) {

        SatelliteObservation observation =
                observationService.getLatest(
                        farmId
                );

        if (observation == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                observation
        );
    }


    /*
     * -------------------------------------------------------------
     * Get all observations for a farm
     *
     * GET /api/satellite-observations/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SatelliteObservation>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                observationService.getByFarm(
                        farmId
                )
        );
    }


    /*
     * -------------------------------------------------------------
     * Get declining observations
     *
     * GET /api/satellite-observations/farm/{farmId}/declining
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/declining")
    public ResponseEntity<List<SatelliteObservation>> getDeclining(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                observationService.getDeclining(
                        farmId
                )
        );
    }


    /*
     * -------------------------------------------------------------
     * Get observations by source
     *
     * GET /api/satellite-observations/farm/{farmId}/source/{source}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/source/{source}")
    public ResponseEntity<List<SatelliteObservation>> getBySource(
            @PathVariable UUID farmId,
            @PathVariable String source
    ) {

        return ResponseEntity.ok(
                observationService.getBySource(
                        farmId,
                        source
                )
        );
    }


    /*
     * -------------------------------------------------------------
     * Get observations by data status
     *
     * GET /api/satellite-observations/farm/{farmId}/status/{dataStatus}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/status/{dataStatus}")
    public ResponseEntity<List<SatelliteObservation>> getByDataStatus(
            @PathVariable UUID farmId,
            @PathVariable String dataStatus
    ) {

        return ResponseEntity.ok(
                observationService.getByDataStatus(
                        farmId,
                        dataStatus
                )
        );
    }


    /*
     * -------------------------------------------------------------
     * Get observation by ID
     *
     * GET /api/satellite-observations/{id}
     * -------------------------------------------------------------
     */
    @GetMapping("/{id}")
    public ResponseEntity<SatelliteObservation> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                observationService.getById(
                        id
                )
        );
    }


    /*
     * -------------------------------------------------------------
     * Request DTO
     * -------------------------------------------------------------
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SatelliteObservationRequest {

        private UUID farmId;

        private UUID cropId;

        private LocalDateTime observedAt;

        private String satelliteSource;

        private String sceneId;

        private Double ndvi;

        private Double ndwi;

        private Double cloudCoverPercent;

        private Double spatialResolutionMeters;

        private String dataStatus;

        private String dataSource;

        private Double confidence;

        private Boolean forecastBased;
    }
}