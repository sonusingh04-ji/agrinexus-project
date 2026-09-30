package com.agrinexus.controller;

import com.agrinexus.dto.WaterStorageProfileRequest;
import com.agrinexus.dto.WaterStorageProfileResponse;
import com.agrinexus.service.WaterStorageProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/water-storage-profiles")
@RequiredArgsConstructor
public class WaterStorageProfileController {

    private final WaterStorageProfileService service;

    @PostMapping
    public ResponseEntity<WaterStorageProfileResponse> create(
            @RequestBody WaterStorageProfileRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<WaterStorageProfileResponse> getByFarm(
            @PathVariable UUID farmId
    ) {

        WaterStorageProfileResponse response =
                service.getByFarmId(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/farm/{farmId}")
    public ResponseEntity<WaterStorageProfileResponse> update(
            @PathVariable UUID farmId,
            @RequestBody WaterStorageProfileRequest request
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
                "Water storage profile deleted successfully"
        );
    }
}