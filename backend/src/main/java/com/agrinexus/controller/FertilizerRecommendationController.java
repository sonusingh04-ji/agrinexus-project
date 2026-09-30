package com.agrinexus.controller;

import com.agrinexus.dto.FertilizerRecommendationRequest;
import com.agrinexus.dto.FertilizerRecommendationResponse;
import com.agrinexus.service.FertilizerRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fertilizer-recommendations")
@RequiredArgsConstructor
public class FertilizerRecommendationController {

    private final FertilizerRecommendationService
            fertilizerRecommendationService;

    /*
     * Generate fertilizer recommendation.
     *
     * POST /api/fertilizer-recommendations
     */
    @PostMapping
    public ResponseEntity<FertilizerRecommendationResponse>
    generateRecommendation(
            @RequestBody FertilizerRecommendationRequest request
    ) {

        FertilizerRecommendationResponse response =
                fertilizerRecommendationService
                        .generateRecommendation(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get recommendation by ID.
     *
     * GET /api/fertilizer-recommendations/{recommendationId}
     */
    @GetMapping("/{recommendationId}")
    public ResponseEntity<FertilizerRecommendationResponse>
    getRecommendationById(
            @PathVariable UUID recommendationId
    ) {

        return ResponseEntity.ok(
                fertilizerRecommendationService
                        .getRecommendationById(
                                recommendationId
                        )
        );
    }

    /*
     * Get all recommendations for a farm.
     *
     * GET /api/fertilizer-recommendations/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FertilizerRecommendationResponse>>
    getFarmRecommendations(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                fertilizerRecommendationService
                        .getFarmRecommendations(farmId)
        );
    }

    /*
     * Get recommendations for a specific crop.
     *
     * GET /api/fertilizer-recommendations/farm/{farmId}/crop/{cropId}
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<List<FertilizerRecommendationResponse>>
    getCropRecommendations(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                fertilizerRecommendationService
                        .getCropRecommendations(
                                farmId,
                                cropId
                        )
        );
    }

    /*
     * Get the latest fertilizer recommendation for a farm.
     *
     * GET /api/fertilizer-recommendations/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<FertilizerRecommendationResponse>
    getLatestFarmRecommendation(
            @PathVariable UUID farmId
    ) {

        FertilizerRecommendationResponse response =
                fertilizerRecommendationService
                        .getLatestFarmRecommendation(
                                farmId
                        );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
}