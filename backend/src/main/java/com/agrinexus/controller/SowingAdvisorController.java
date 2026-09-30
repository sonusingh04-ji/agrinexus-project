package com.agrinexus.controller;

import com.agrinexus.dto.SowingAdvisorRequest;
import com.agrinexus.dto.SowingAdvisorResponse;
import com.agrinexus.entity.SowingAdvisorDecision;
import com.agrinexus.service.SowingAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sowing-advisor")
@RequiredArgsConstructor
public class SowingAdvisorController {

    private final SowingAdvisorService sowingAdvisorService;

    /*
     * -------------------------------------------------------------
     * Generate sowing advice.
     *
     * POST /api/sowing-advisor/recommend
     * -------------------------------------------------------------
     */
    @PostMapping("/recommend")
    public ResponseEntity<SowingAdvisorResponse> getSowingAdvice(
            @RequestBody SowingAdvisorRequest request
    ) {

        if (request == null
                || request.getFarmId() == null
                || request.getCropId() == null) {

            throw new RuntimeException(
                    "farmId and cropId are required"
            );
        }

        SowingAdvisorDecision decision =
                sowingAdvisorService.evaluate(
                        request.getFarmId(),
                        request.getCropId()
                );

        return ResponseEntity.ok(
                toResponse(decision)
        );
    }

    /*
     * -------------------------------------------------------------
     * Get latest sowing decision for a farm.
     *
     * GET /api/sowing-advisor/farm/{farmId}/latest
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<SowingAdvisorResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        SowingAdvisorDecision decision =
                sowingAdvisorService.getLatest(
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
     * Get all sowing decisions for a farm.
     *
     * GET /api/sowing-advisor/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SowingAdvisorResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                sowingAdvisorService
                        .getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * -------------------------------------------------------------
     * Get sowing decision by ID.
     *
     * GET /api/sowing-advisor/{id}
     * -------------------------------------------------------------
     */
    @GetMapping("/{id}")
    public ResponseEntity<SowingAdvisorResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        sowingAdvisorService.getById(
                                id
                        )
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Entity -> Response DTO
     * -------------------------------------------------------------
     */
    private SowingAdvisorResponse toResponse(
            SowingAdvisorDecision decision
    ) {

        return SowingAdvisorResponse.builder()

                .id(
                        decision.getId()
                )

                .farmId(
                        decision.getFarm() != null
                                ? decision.getFarm().getId()
                                : null
                )

                .cropId(
                        decision.getCrop() != null
                                ? decision.getCrop().getId()
                                : null
                )

                .cropName(
                        decision.getCrop() != null
                                ? decision.getCrop().getName()
                                : null
                )

                .decision(
                        decision.getDecision()
                )

                .recommendedWindow(
                        decision.getRecommendedWindow()
                )

                .season(
                        decision.getSeason()
                )

                .currentMoisture(
                        decision.getCurrentMoisture()
                )

                .currentTemperature(
                        decision.getCurrentTemperature()
                )

                .currentHumidity(
                        decision.getCurrentHumidity()
                )

                .recentRainfall(
                        decision.getRecentRainfall()
                )

                .forecastRainfall(
                        decision.getForecastRainfall()
                )

                .forecastConfidence(
                        decision.getForecastConfidence()
                )

                .idealTemperatureMin(
                        decision.getIdealTemperatureMin()
                )

                .idealTemperatureMax(
                        decision.getIdealTemperatureMax()
                )

                .soilPh(
                        decision.getSoilPh()
                )

                .cropSoilPhMin(
                        decision.getCropSoilPhMin()
                )

                .cropSoilPhMax(
                        decision.getCropSoilPhMax()
                )

                .reason(
                        decision.getReason()
                )

                .contributingFactors(
                        decision.getContributingFactors()
                )

                .recommendation(
                        decision.getRecommendation()
                )

                .dataUsed(
                        decision.getDataUsed()
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