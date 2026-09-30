package com.agrinexus.controller;

import com.agrinexus.dto.RainfallHarvestingResponse;
import com.agrinexus.service.RainfallHarvestingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rainfall-harvesting")
@RequiredArgsConstructor
public class RainfallHarvestingController {

    private final RainfallHarvestingService service;

    /*
     * Evaluate rainfall harvesting using the
     * stored forecast data for the farm.
     *
     * GET /api/rainfall-harvesting/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<RainfallHarvestingResponse> evaluate(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.evaluate(farmId)
        );
    }
}