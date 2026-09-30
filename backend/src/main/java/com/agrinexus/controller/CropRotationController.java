package com.agrinexus.controller;

import com.agrinexus.dto.CropRotationRequest;
import com.agrinexus.dto.CropRotationResponse;
import com.agrinexus.entity.CropRotationDecision;
import com.agrinexus.service.CropRotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crop-rotation")
@RequiredArgsConstructor
public class CropRotationController {

    private final CropRotationService cropRotationService;

    /*
     * -------------------------------------------------------------
     * Generate ranked next-crop recommendations.
     *
     * POST /api/crop-rotation/recommend
     * -------------------------------------------------------------
     */
    @PostMapping("/recommend")
    public ResponseEntity<CropRotationResponse> recommendNextCrops(
            @RequestBody CropRotationRequest request
    ) {

        if (request == null
                || request.getFarmId() == null) {

            throw new RuntimeException(
                    "farmId is required"
            );
        }

        CropRotationDecision decision =
                cropRotationService.evaluate(
                        request.getFarmId(),
                        request.getPreviousCropId(),
                        request.getTargetSeason(),
                        request.getFarmerConstraint()
                );

        return ResponseEntity.ok(
                toResponse(decision)
        );
    }

    /*
     * -------------------------------------------------------------
     * Get latest rotation recommendation.
     *
     * GET /api/crop-rotation/farm/{farmId}/latest
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<CropRotationResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        CropRotationDecision decision =
                cropRotationService.getLatest(
                        farmId
                );

        if (decision == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                toResponse(decision)
        );
    }

    /*
     * -------------------------------------------------------------
     * Get all rotation decisions for a farm.
     *
     * GET /api/crop-rotation/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<CropRotationResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                cropRotationService
                        .getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * -------------------------------------------------------------
     * Get rotation decision by ID.
     *
     * GET /api/crop-rotation/{id}
     * -------------------------------------------------------------
     */
    @GetMapping("/{id}")
    public ResponseEntity<CropRotationResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        cropRotationService.getById(
                                id
                        )
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Entity -> Response DTO.
     * -------------------------------------------------------------
     */
    private CropRotationResponse toResponse(
            CropRotationDecision decision
    ) {

        return CropRotationResponse.builder()

                .id(
                        decision.getId()
                )

                .farmId(
                        decision.getFarm() != null
                                ? decision.getFarm().getId()
                                : null
                )

                .previousCropId(
                        decision.getPreviousCrop() != null
                                ? decision.getPreviousCrop().getId()
                                : null
                )

                .previousCropName(
                        decision.getPreviousCrop() != null
                                ? decision.getPreviousCrop().getName()
                                : null
                )

                .recommendedCropId(
                        decision.getRecommendedCrop() != null
                                ? decision.getRecommendedCrop().getId()
                                : null
                )

                .recommendedCropName(
                        decision.getRecommendedCrop() != null
                                ? decision.getRecommendedCrop().getName()
                                : null
                )

                .targetSeason(
                        decision.getTargetSeason()
                )

                .recommendationScore(
                        decision.getRecommendationScore()
                )

                .reason(
                        decision.getReason()
                )

                .contributingFactors(
                        decision.getContributingFactors()
                )

                .rankedCandidates(
                        decision.getRankedCandidates()
                )

                .soilPh(
                        decision.getSoilPh()
                )

                .soilNitrogen(
                        decision.getSoilNitrogen()
                )

                .soilPhosphorus(
                        decision.getSoilPhosphorus()
                )

                .soilPotassium(
                        decision.getSoilPotassium()
                )

                .currentMoisture(
                        decision.getCurrentMoisture()
                )

                .recentRainfall(
                        decision.getRecentRainfall()
                )

                .farmerConstraint(
                        decision.getFarmerConstraint()
                )

                .dataStatus(
                        decision.getDataStatus()
                )

                .methodologyVersion(
                        decision.getMethodologyVersion()
                )

                .confidence(
                        decision.getConfidence()
                )

                .uncertaintyNote(
                        decision.getUncertaintyNote()
                )

                .evaluatedAt(
                        decision.getEvaluatedAt()
                )

                .validUntil(
                        decision.getValidUntil()
                )

                .build();
    }
}