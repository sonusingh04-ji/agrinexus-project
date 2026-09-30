package com.agrinexus.controller;

import com.agrinexus.dto.FertilizerInputRequest;
import com.agrinexus.dto.FertilizerInputResponse;
import com.agrinexus.service.FertilizerInputService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fertilizer-inputs")
@RequiredArgsConstructor
public class FertilizerInputController {

    private final FertilizerInputService fertilizerInputService;

    /*
     * Create fertilizer/input record.
     *
     * POST /api/fertilizer-inputs
     */
    @PostMapping
    public ResponseEntity<FertilizerInputResponse> createFertilizerInput(
            @RequestBody FertilizerInputRequest request
    ) {

        FertilizerInputResponse response =
                fertilizerInputService.createFertilizerInput(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get fertilizer input by ID.
     *
     * GET /api/fertilizer-inputs/{inputId}
     */
    @GetMapping("/{inputId}")
    public ResponseEntity<FertilizerInputResponse> getFertilizerInputById(
            @PathVariable UUID inputId
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.getFertilizerInputById(inputId)
        );
    }

    /*
     * Get all fertilizer inputs for a farm.
     *
     * GET /api/fertilizer-inputs/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FertilizerInputResponse>> getFarmFertilizerInputs(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.getFarmFertilizerInputs(farmId)
        );
    }

    /*
     * Get fertilizer inputs for a specific crop.
     *
     * GET /api/fertilizer-inputs/farm/{farmId}/crop/{cropId}
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<List<FertilizerInputResponse>>
    getCropFertilizerInputs(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.getCropFertilizerInputs(
                        farmId,
                        cropId
                )
        );
    }

    /*
     * Get fertilizer inputs by type.
     *
     * Example:
     * /api/fertilizer-inputs/farm/{farmId}/type/ORGANIC
     */
    @GetMapping("/farm/{farmId}/type/{inputType}")
    public ResponseEntity<List<FertilizerInputResponse>>
    getInputsByType(
            @PathVariable UUID farmId,
            @PathVariable String inputType
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.getInputsByType(
                        farmId,
                        inputType
                )
        );
    }

    /*
     * Get fertilizer inputs within a date range.
     *
     * Example:
     * /api/fertilizer-inputs/farm/{farmId}/range
     * ?startDate=2026-01-01&endDate=2026-08-28
     */
    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<FertilizerInputResponse>>
    getInputsByDateRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.getInputsByDateRange(
                        farmId,
                        startDate,
                        endDate
                )
        );
    }

    /*
     * Update fertilizer/input record.
     *
     * PUT /api/fertilizer-inputs/{inputId}
     */
    @PutMapping("/{inputId}")
    public ResponseEntity<FertilizerInputResponse> updateFertilizerInput(
            @PathVariable UUID inputId,
            @RequestBody FertilizerInputRequest request
    ) {

        return ResponseEntity.ok(
                fertilizerInputService.updateFertilizerInput(
                        inputId,
                        request
                )
        );
    }

    /*
     * Delete fertilizer/input record.
     *
     * DELETE /api/fertilizer-inputs/{inputId}
     */
    @DeleteMapping("/{inputId}")
    public ResponseEntity<String> deleteFertilizerInput(
            @PathVariable UUID inputId
    ) {

        fertilizerInputService.deleteFertilizerInput(inputId);

        return ResponseEntity.ok(
                "Fertilizer input deleted successfully"
        );
    }
}