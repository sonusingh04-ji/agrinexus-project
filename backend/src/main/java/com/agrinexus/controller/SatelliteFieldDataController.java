package com.agrinexus.controller;

import com.agrinexus.dto.SatelliteFieldDataRequest;
import com.agrinexus.dto.SatelliteFieldDataResponse;
import com.agrinexus.service.SatelliteFieldDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/satellite")
@RequiredArgsConstructor
public class SatelliteFieldDataController {

    private final SatelliteFieldDataService satelliteFieldDataService;

    @PostMapping
    public ResponseEntity<SatelliteFieldDataResponse>
    createSatelliteFieldData(
            @Valid @RequestBody SatelliteFieldDataRequest request
    ) {

        SatelliteFieldDataResponse response =
                satelliteFieldDataService
                        .createSatelliteFieldData(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SatelliteFieldDataResponse>>
    getSatelliteFieldDataByFarmId(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                satelliteFieldDataService
                        .getSatelliteFieldDataByFarmId(farmId)
        );
    }
}