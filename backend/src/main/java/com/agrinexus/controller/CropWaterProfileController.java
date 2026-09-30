package com.agrinexus.controller;

import com.agrinexus.dto.CropWaterProfileRequest;
import com.agrinexus.dto.CropWaterProfileResponse;
import com.agrinexus.service.CropWaterProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crop-water-profiles")
@RequiredArgsConstructor
public class CropWaterProfileController {

    private final CropWaterProfileService service;

    @PostMapping
    public ResponseEntity<CropWaterProfileResponse> create(
            @RequestBody CropWaterProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    /**
     * Backward-compatible endpoint: returns the most specific available profile.
     */
    @GetMapping("/crop/{cropId}")
    public ResponseEntity<CropWaterProfileResponse> getByCrop(
            @PathVariable UUID cropId
    ) {
        CropWaterProfileResponse response = service.getByCropId(cropId);
        return response == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(response);
    }

    /**
     * Returns every reference profile for a crop.
     */
    @GetMapping("/crop/{cropId}/all")
    public ResponseEntity<List<CropWaterProfileResponse>> getAllByCrop(
            @PathVariable UUID cropId
    ) {
        return ResponseEntity.ok(service.getAllByCropId(cropId));
    }

    /**
     * Resolve the best profile for crop + geography + season.
     * More specific records beat generic fallback records.
     */
    @GetMapping("/crop/{cropId}/resolve")
    public ResponseEntity<CropWaterProfileResponse> resolve(
            @PathVariable UUID cropId,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String districtName,
            @RequestParam(required = false) String season
    ) {
        CropWaterProfileResponse response =
                service.resolve(cropId, stateName, districtName, season);

        return response == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(response);
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<CropWaterProfileResponse> update(
            @PathVariable UUID profileId,
            @RequestBody CropWaterProfileRequest request
    ) {
        return ResponseEntity.ok(service.update(profileId, request));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<String> delete(
            @PathVariable UUID profileId
    ) {
        service.delete(profileId);
        return ResponseEntity.ok("Crop water profile deleted successfully");
    }
}
