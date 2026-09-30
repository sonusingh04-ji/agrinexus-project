package com.agrinexus.controller;

import com.agrinexus.dto.CropRequest;
import com.agrinexus.dto.CropResponse;
import com.agrinexus.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {

    private final CropService cropService;

    /*
     * Create a new crop knowledge record.
     *
     * POST /api/crops
     */
    @PostMapping
    public ResponseEntity<CropResponse> createCrop(
            @RequestBody CropRequest request
    ) {

        CropResponse response =
                cropService.createCrop(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get crop by ID.
     *
     * GET /api/crops/{cropId}
     */
    @GetMapping("/{cropId}")
    public ResponseEntity<CropResponse> getCropById(
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                cropService.getCropById(cropId)
        );
    }

    /*
     * Get all crops.
     *
     * GET /api/crops
     */
    @GetMapping
    public ResponseEntity<List<CropResponse>> getAllCrops() {

        return ResponseEntity.ok(
                cropService.getAllCrops()
        );
    }

    /*
     * Get crops by season.
     *
     * Example:
     * GET /api/crops/season/Rabi
     */
    @GetMapping("/season/{season}")
    public ResponseEntity<List<CropResponse>> getCropsBySeason(
            @PathVariable String season
    ) {

        return ResponseEntity.ok(
                cropService.getCropsBySeason(season)
        );
    }

    /*
     * Get crops by soil type.
     *
     * Example:
     * GET /api/crops/soil/loam
     */
    @GetMapping("/soil/{soilType}")
    public ResponseEntity<List<CropResponse>> getCropsBySoilType(
            @PathVariable String soilType
    ) {

        return ResponseEntity.ok(
                cropService.getCropsBySoilType(soilType)
        );
    }

    /*
     * Get crops suitable for a given soil pH.
     *
     * Example:
     * GET /api/crops/ph/6.8
     */
    @GetMapping("/ph/{pH}")
    public ResponseEntity<List<CropResponse>> getCropsBySoilPh(
            @PathVariable Double pH
    ) {

        return ResponseEntity.ok(
                cropService.getCropsBySoilPh(pH)
        );
    }
}