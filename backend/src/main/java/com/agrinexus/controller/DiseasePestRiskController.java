package com.agrinexus.controller;

import com.agrinexus.dto.DiseasePestRiskRequest;
import com.agrinexus.dto.DiseasePestRiskResponse;
import com.agrinexus.entity.DiseasePestRisk;
import com.agrinexus.service.DiseasePestRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disease-pest")
@RequiredArgsConstructor
public class DiseasePestRiskController {

    private final DiseasePestRiskService service;

    /*
     * Evaluate current disease/pest environmental risk.
     *
     * POST /api/disease-pest/evaluate
     */
    @PostMapping("/evaluate")
    public ResponseEntity<List<DiseasePestRiskResponse>> evaluate(
            @RequestBody DiseasePestRiskRequest request
    ) {

        List<DiseasePestRisk> risks =
                service.evaluate(
                        request.getFarmId(),
                        request.getCropId()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        risks.stream()
                                .map(this::toResponse)
                                .toList()
                );
    }

    /*
     * Get risk by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DiseasePestRiskResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        service.getById(id)
                )
        );
    }

    /*
     * Get all risks for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<DiseasePestRiskResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Get HIGH risk results.
     */
    @GetMapping("/farm/{farmId}/high")
    public ResponseEntity<List<DiseasePestRiskResponse>> getHighRisk(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getHighRisk(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Get forecast-based risk results.
     */
    @GetMapping("/farm/{farmId}/forecast")
    public ResponseEntity<List<DiseasePestRiskResponse>> getForecastRisks(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getForecastRisks(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Entity → Response DTO.
     */
    private DiseasePestRiskResponse toResponse(
            DiseasePestRisk risk
    ) {

        return DiseasePestRiskResponse.builder()
                .id(
                        risk.getId()
                )
                .farmId(
                        risk.getFarm() != null
                                ? risk.getFarm().getId()
                                : null
                )
                .cropId(
                        risk.getCrop() != null
                                ? risk.getCrop().getId()
                                : null
                )
                .cropName(
                        risk.getCrop() != null
                                ? risk.getCrop().getName()
                                : null
                )
                .riskCategory(
                        risk.getRiskCategory()
                )
                .riskName(
                        risk.getRiskName()
                )
                .riskLevel(
                        risk.getRiskLevel()
                )
                .riskScore(
                        risk.getRiskScore()
                )
                .reason(
                        risk.getReason()
                )
                .recommendation(
                        risk.getRecommendation()
                )
                .scoutingAdvice(
                        risk.getScoutingAdvice()
                )
                .contributingFactors(
                        risk.getContributingFactors()
                )
                .source(
                        risk.getSource()
                )
                .dataStatus(
                        risk.getDataStatus()
                )
                .uncertaintyNote(
                        risk.getUncertaintyNote()
                )
                .forecastBased(
                        risk.getForecastBased()
                )
                .evaluatedAt(
                        risk.getEvaluatedAt()
                )
                .validUntil(
                        risk.getValidUntil()
                )
                .build();
    }
}