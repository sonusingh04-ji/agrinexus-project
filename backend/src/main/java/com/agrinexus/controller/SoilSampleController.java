package com.agrinexus.controller;

import com.agrinexus.dto.SoilSampleRequest;
import com.agrinexus.dto.SoilSampleResponse;
import com.agrinexus.service.SoilSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/soil-samples")
@RequiredArgsConstructor
public class SoilSampleController {

    private final SoilSampleService soilSampleService;

    /*
     * Create a soil sample.
     *
     * POST /api/soil-samples
     */
    @PostMapping
    public ResponseEntity<SoilSampleResponse> createSoilSample(
            @RequestBody SoilSampleRequest request
    ) {

        SoilSampleResponse response =
                soilSampleService.createSoilSample(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get soil sample by ID.
     *
     * GET /api/soil-samples/{sampleId}
     */
    @GetMapping("/{sampleId}")
    public ResponseEntity<SoilSampleResponse> getSoilSampleById(
            @PathVariable UUID sampleId
    ) {

        return ResponseEntity.ok(
                soilSampleService.getSoilSampleById(sampleId)
        );
    }

    /*
     * Get all soil samples for a farm.
     *
     * GET /api/soil-samples/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SoilSampleResponse>> getFarmSoilSamples(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                soilSampleService.getFarmSoilSamples(farmId)
        );
    }

    /*
     * Get latest soil sample for a farm.
     *
     * GET /api/soil-samples/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<SoilSampleResponse> getLatestSoilSample(
            @PathVariable UUID farmId
    ) {

        SoilSampleResponse response =
                soilSampleService.getLatestSoilSample(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get soil samples by source.
     *
     * Example:
     * GET /api/soil-samples/farm/{farmId}/source/LABORATORY
     */
    @GetMapping("/farm/{farmId}/source/{source}")
    public ResponseEntity<List<SoilSampleResponse>>
    getSoilSamplesBySource(
            @PathVariable UUID farmId,
            @PathVariable String source
    ) {

        return ResponseEntity.ok(
                soilSampleService.getSoilSamplesBySource(
                        farmId,
                        source
                )
        );
    }

    /*
     * Get soil samples within a date range.
     *
     * Example:
     * GET /api/soil-samples/farm/{farmId}/range
     *     ?startDate=2026-01-01&endDate=2026-08-28
     */
    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<SoilSampleResponse>>
    getSoilSamplesByDateRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                soilSampleService.getSoilSamplesByDateRange(
                        farmId,
                        startDate,
                        endDate
                )
        );
    }

    /*
     * Update soil sample.
     *
     * PUT /api/soil-samples/{sampleId}
     */
    @PutMapping("/{sampleId}")
    public ResponseEntity<SoilSampleResponse> updateSoilSample(
            @PathVariable UUID sampleId,
            @RequestBody SoilSampleRequest request
    ) {

        return ResponseEntity.ok(
                soilSampleService.updateSoilSample(
                        sampleId,
                        request
                )
        );
    }

    /*
     * Delete soil sample.
     *
     * DELETE /api/soil-samples/{sampleId}
     */
    @DeleteMapping("/{sampleId}")
    public ResponseEntity<String> deleteSoilSample(
            @PathVariable UUID sampleId
    ) {

        soilSampleService.deleteSoilSample(sampleId);

        return ResponseEntity.ok(
                "Soil sample deleted successfully"
        );
    }
}