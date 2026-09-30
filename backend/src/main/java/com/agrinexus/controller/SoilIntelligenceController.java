package com.agrinexus.controller;

import com.agrinexus.dto.SoilIntelligenceResponse;
import com.agrinexus.service.SoilIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/soil-intelligence")
@RequiredArgsConstructor
public class SoilIntelligenceController {

    private final SoilIntelligenceService soilIntelligenceService;

    /*
     * -------------------------------------------------------------
     * Analyze latest soil sample for a farm.
     *
     * GET
     * /api/soil-intelligence/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<SoilIntelligenceResponse> analyzeFarmSoil(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                soilIntelligenceService.analyze(
                        farmId,
                        null
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Analyze latest soil sample against a selected crop.
     *
     * Example:
     *
     * GET
     * /api/soil-intelligence/farm/{farmId}/crop/{cropId}
     *
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<SoilIntelligenceResponse>
    analyzeFarmSoilForCrop(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                soilIntelligenceService.analyze(
                        farmId,
                        cropId
                )
        );
    }
}