package com.agrinexus.controller;

import com.agrinexus.dto.CropHistoryRequest;
import com.agrinexus.dto.CropHistoryResponse;
import com.agrinexus.service.CropHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crop-history")
@RequiredArgsConstructor
public class CropHistoryController {

    private final CropHistoryService cropHistoryService;

    /*
     * Create a crop history record.
     *
     * POST /api/crop-history
     */
    @PostMapping
    public ResponseEntity<CropHistoryResponse> createCropHistory(
            @RequestBody CropHistoryRequest request
    ) {

        CropHistoryResponse response =
                cropHistoryService.createCropHistory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get crop history by ID.
     *
     * GET /api/crop-history/{historyId}
     */
    @GetMapping("/{historyId}")
    public ResponseEntity<CropHistoryResponse> getCropHistoryById(
            @PathVariable UUID historyId
    ) {

        return ResponseEntity.ok(
                cropHistoryService.getCropHistoryById(historyId)
        );
    }

    /*
     * Get complete crop history for a farm.
     *
     * GET /api/crop-history/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<CropHistoryResponse>> getFarmCropHistory(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                cropHistoryService.getFarmCropHistory(farmId)
        );
    }

    /*
     * Get latest crop history for a farm.
     *
     * GET /api/crop-history/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<CropHistoryResponse> getLatestCropHistory(
            @PathVariable UUID farmId
    ) {

        CropHistoryResponse response =
                cropHistoryService.getLatestCropHistory(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get history records for a specific crop.
     *
     * GET /api/crop-history/crop/{cropId}
     */
    @GetMapping("/crop/{cropId}")
    public ResponseEntity<List<CropHistoryResponse>> getCropHistoryByCrop(
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                cropHistoryService.getCropHistoryByCrop(cropId)
        );
    }

    /*
     * Get history where crop residue is present.
     *
     * GET /api/crop-history/farm/{farmId}/residue
     */
    @GetMapping("/farm/{farmId}/residue")
    public ResponseEntity<List<CropHistoryResponse>>
    getFarmHistoryWithResidue(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                cropHistoryService.getFarmHistoryWithResidue(farmId)
        );
    }

    /*
     * Update crop history.
     *
     * PUT /api/crop-history/{historyId}
     */
    @PutMapping("/{historyId}")
    public ResponseEntity<CropHistoryResponse> updateCropHistory(
            @PathVariable UUID historyId,
            @RequestBody CropHistoryRequest request
    ) {

        return ResponseEntity.ok(
                cropHistoryService.updateCropHistory(
                        historyId,
                        request
                )
        );
    }

    /*
     * Delete crop history.
     *
     * DELETE /api/crop-history/{historyId}
     */
    @DeleteMapping("/{historyId}")
    public ResponseEntity<String> deleteCropHistory(
            @PathVariable UUID historyId
    ) {

        cropHistoryService.deleteCropHistory(historyId);

        return ResponseEntity.ok(
                "Crop history deleted successfully"
        );
    }
}