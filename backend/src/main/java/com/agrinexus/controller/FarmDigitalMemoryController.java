package com.agrinexus.controller;

import com.agrinexus.dto.DigitalMemoryResponse;
import com.agrinexus.service.FarmDigitalMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/digital-memory")
@RequiredArgsConstructor
public class FarmDigitalMemoryController {

    private final FarmDigitalMemoryService service;

    /*
     * Get the complete digital memory of a farm.
     *
     * GET /api/digital-memory/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<DigitalMemoryResponse> getFarmMemory(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getFarmMemory(farmId)
        );
    }
}