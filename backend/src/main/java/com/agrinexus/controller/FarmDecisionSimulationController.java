package com.agrinexus.controller;

import com.agrinexus.dto.FarmDecisionSimulationRequest;
import com.agrinexus.dto.FarmDecisionSimulationResponse;
import com.agrinexus.entity.FarmDecisionSimulation;
import com.agrinexus.service.FarmDecisionSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decision-simulator")
@RequiredArgsConstructor
public class FarmDecisionSimulationController {

    private final FarmDecisionSimulationService simulationService;

    /*
     * -------------------------------------------------------------
     * Run a scenario comparison.
     *
     * POST /api/decision-simulator/simulate
     * -------------------------------------------------------------
     */
    @PostMapping("/simulate")
    public ResponseEntity<FarmDecisionSimulationResponse> simulate(
            @RequestBody FarmDecisionSimulationRequest request
    ) {

        if (request == null
                || request.getFarmId() == null) {

            throw new RuntimeException(
                    "farmId is required"
            );
        }

        if (request.getScenarioA() == null
                || request.getScenarioA().isBlank()) {

            throw new RuntimeException(
                    "scenarioA is required"
            );
        }

        if (request.getScenarioB() == null
                || request.getScenarioB().isBlank()) {

            throw new RuntimeException(
                    "scenarioB is required"
            );
        }

        FarmDecisionSimulation simulation =
                simulationService.simulate(
                        request.getFarmId(),
                        request.getCropId(),
                        request.getScenarioA(),
                        request.getScenarioB()
                );

        return ResponseEntity.ok(
                toResponse(
                        simulation
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * Latest simulation for a farm.
     *
     * GET /api/decision-simulator/farm/{farmId}/latest
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<FarmDecisionSimulationResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        FarmDecisionSimulation simulation =
                simulationService.getLatest(
                        farmId
                );

        if (simulation == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                toResponse(
                        simulation
                )
        );
    }

    /*
     * -------------------------------------------------------------
     * All simulations for a farm.
     *
     * GET /api/decision-simulator/farm/{farmId}
     * -------------------------------------------------------------
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FarmDecisionSimulationResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                simulationService
                        .getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * -------------------------------------------------------------
     * Get simulation by ID.
     *
     * GET /api/decision-simulator/{id}
     * -------------------------------------------------------------
     */
    @GetMapping("/{id}")
    public ResponseEntity<FarmDecisionSimulationResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        simulationService.getById(
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
    private FarmDecisionSimulationResponse toResponse(
            FarmDecisionSimulation simulation
    ) {

        return FarmDecisionSimulationResponse.builder()

                .id(
                        simulation.getId()
                )

                .farmId(
                        simulation.getFarm() != null
                                ? simulation.getFarm().getId()
                                : null
                )

                .cropId(
                        simulation.getCrop() != null
                                ? simulation.getCrop().getId()
                                : null
                )

                .cropName(
                        simulation.getCrop() != null
                                ? simulation.getCrop().getName()
                                : null
                )

                .scenario(
                        simulation.getScenario()
                )

                .comparisonScenario(
                        simulation.getComparisonScenario()
                )

                .conclusion(
                        simulation.getConclusion()
                )

                .currentMoisture(
                        simulation.getCurrentMoisture()
                )

                .forecastRainfall(
                        simulation.getForecastRainfall()
                )

                .forecastConfidence(
                        simulation.getForecastConfidence()
                )

                .waterDifference(
                        simulation.getWaterDifference()
                )

                .energyCostDifference(
                        simulation.getEnergyCostDifference()
                )

                .timingDifference(
                        simulation.getTimingDifference()
                )

                .scenarioAExplanation(
                        simulation.getScenarioAExplanation()
                )

                .scenarioBExplanation(
                        simulation.getScenarioBExplanation()
                )

                .recommendation(
                        simulation.getRecommendation()
                )

                .assumptions(
                        simulation.getAssumptions()
                )

                .dataUsed(
                        simulation.getDataUsed()
                )

                .dataStatus(
                        simulation.getDataStatus()
                )

                .methodologyVersion(
                        simulation.getMethodologyVersion()
                )

                .confidence(
                        simulation.getConfidence()
                )

                .uncertaintyNote(
                        simulation.getUncertaintyNote()
                )

                .simulatedAt(
                        simulation.getSimulatedAt()
                )

                .validUntil(
                        simulation.getValidUntil()
                )

                .build();
    }
}