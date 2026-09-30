package com.agrinexus.controller;

import com.agrinexus.dto.SolarPumpProfileRequest;
import com.agrinexus.dto.SolarPumpProfileResponse;
import com.agrinexus.service.SolarPumpProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/solar-pump-profiles")
@RequiredArgsConstructor
public class SolarPumpProfileController {

    private final SolarPumpProfileService service;

    @PostMapping
    public ResponseEntity<SolarPumpProfileResponse> create(
            @RequestBody SolarPumpProfileRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<SolarPumpProfileResponse> getByFarm(
            @PathVariable UUID farmId
    ) {

        SolarPumpProfileResponse response =
                service.getByFarmId(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/farm/{farmId}")
    public ResponseEntity<SolarPumpProfileResponse> update(
            @PathVariable UUID farmId,
            @RequestBody SolarPumpProfileRequest request
    ) {

        return ResponseEntity.ok(
                service.update(
                        farmId,
                        request
                )
        );
    }

    @DeleteMapping("/farm/{farmId}")
    public ResponseEntity<String> delete(
            @PathVariable UUID farmId
    ) {

        service.delete(farmId);

        return ResponseEntity.ok(
                "Solar pump profile deleted successfully"
        );
    }
}