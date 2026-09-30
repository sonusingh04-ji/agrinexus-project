package com.agrinexus.controller;

import com.agrinexus.dto.FarmRequest;
import com.agrinexus.dto.FarmResponse;
import com.agrinexus.service.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;


    /*
     * ============================================================
     * CREATE FARM
     * ============================================================
     *
     * POST /api/farms
     *
     * Creates a new farm for a farmer.
     */
    @PostMapping
    public ResponseEntity<FarmResponse> createFarm(
            @Valid @RequestBody FarmRequest request
    ) {

        FarmResponse response =
                farmService.createFarm(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /*
     * ============================================================
     * GET ALL FARMS OF A FARMER
     * ============================================================
     *
     * GET /api/farms/farmer/{farmerId}
     *
     * Returns every farm belonging to the specified farmer.
     */
    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<FarmResponse>> getFarmsByFarmer(
            @PathVariable UUID farmerId
    ) {

        List<FarmResponse> farms =
                farmService.getFarmsByFarmer(farmerId);

        return ResponseEntity.ok(farms);
    }


    /*
     * ============================================================
     * GET FARM BY ID
     * ============================================================
     *
     * GET /api/farms/{farmId}
     *
     * Returns one specific farm.
     */
    @GetMapping("/{farmId}")
    public ResponseEntity<FarmResponse> getFarmById(
            @PathVariable UUID farmId
    ) {

        FarmResponse response =
                farmService.getFarmById(farmId);

        return ResponseEntity.ok(response);
    }


    /*
     * ============================================================
     * DELETE FARM
     * ============================================================
     *
     * DELETE /api/farms/{farmId}
     *
     * Permanently deletes the specified farm.
     */
    @DeleteMapping("/{farmId}")
    public ResponseEntity<Void> deleteFarm(
            @PathVariable UUID farmId
    ) {

        farmService.deleteFarm(farmId);

        return ResponseEntity.noContent().build();
    }
}