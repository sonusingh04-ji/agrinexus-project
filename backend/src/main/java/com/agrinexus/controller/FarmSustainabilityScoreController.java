package com.agrinexus.controller;

import com.agrinexus.dto.FarmSustainabilityScoreResponse;
import com.agrinexus.entity.FarmSustainabilityScore;
import com.agrinexus.service.FarmSustainabilityScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sustainability")
@RequiredArgsConstructor
public class FarmSustainabilityScoreController {

    private final FarmSustainabilityScoreService sustainabilityService;

    /*
     * -------------------------------------------------------------
     * Calculate a new sustainability score.
     *
     * POST /api/sustainability/farm/{farmId}/evaluate
     * -------------------------------------------------------------
     */
    @PostMapping("/farm/{farmId}/evaluate")
    public ResponseEntity<FarmSustainabilityScoreResponse> evaluate(
            @PathVariable UUID farmId
    ) {

        FarmSustainabilityScore score =
                sustainabilityService.evaluate(
                        farmId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        toResponse(score)
                );
    }

    /*
     * -------------------------------------------------------------
     * Get latest sustainability score.
     *
     * GET /api/sustainability/farm/{farmId}/latest
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<FarmSustainabilityScoreResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        FarmSustainabilityScore score =
                sustainabilityService.getLatest(
                        farmId
                );

        if (score == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                toResponse(score)
        );
    }

    /*
     * -------------------------------------------------------------
     * Get complete sustainability history.
     *
     * GET /api/sustainability/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FarmSustainabilityScoreResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                sustainabilityService
                        .getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * -------------------------------------------------------------
     * Get sustainability score by ID.
     *
     * GET /api/sustainability/{id}
     * -------------------------------------------------------------
     */
    @GetMapping("/{id}")
    public ResponseEntity<FarmSustainabilityScoreResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        sustainabilityService.getById(
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
    private FarmSustainabilityScoreResponse toResponse(
            FarmSustainabilityScore score
    ) {

        return FarmSustainabilityScoreResponse.builder()

                .id(
                        score.getId()
                )

                .farmId(
                        score.getFarm() != null
                                ? score.getFarm().getId()
                                : null
                )

                .cropId(
                        score.getCrop() != null
                                ? score.getCrop().getId()
                                : null
                )

                .cropName(
                        score.getCrop() != null
                                ? score.getCrop().getName()
                                : null
                )

                .overallScore(
                        score.getOverallScore()
                )

                .soilScore(
                        score.getSoilScore()
                )

                .waterScore(
                        score.getWaterScore()
                )

                .energyScore(
                        score.getEnergyScore()
                )

                .cropPracticeScore(
                        score.getCropPracticeScore()
                )

                .fieldPracticeScore(
                        score.getFieldPracticeScore()
                )

                .sustainabilityLevel(
                        score.getSustainabilityLevel()
                )

                .soilExplanation(
                        score.getSoilExplanation()
                )

                .waterExplanation(
                        score.getWaterExplanation()
                )

                .energyExplanation(
                        score.getEnergyExplanation()
                )

                .cropPracticeExplanation(
                        score.getCropPracticeExplanation()
                )

                .fieldPracticeExplanation(
                        score.getFieldPracticeExplanation()
                )

                .recommendation(
                        score.getRecommendation()
                )

                .dataUsed(
                        score.getDataUsed()
                )

                .dataStatus(
                        score.getDataStatus()
                )

                .methodologyVersion(
                        score.getMethodologyVersion()
                )

                .confidence(
                        score.getConfidence()
                )

                .uncertaintyNote(
                        score.getUncertaintyNote()
                )

                .evaluatedAt(
                        score.getEvaluatedAt()
                )

                .validUntil(
                        score.getValidUntil()
                )

                .build();
    }
}