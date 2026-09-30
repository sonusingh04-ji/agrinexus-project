package com.agrinexus.controller;

import com.agrinexus.dto.FarmDecisionSimulatorRequest;
import com.agrinexus.dto.FarmDecisionSimulatorResponse;
import com.agrinexus.service.FarmDecisionSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decision-simulator")
@RequiredArgsConstructor
public class FarmDecisionSimulatorController {

    private final FarmDecisionSimulatorService
            farmDecisionSimulatorService;

    /*
     * Compare two farm-decision scenarios.
     *
     * POST /api/decision-simulator/compare
     */
    @PostMapping("/compare")
    public ResponseEntity<FarmDecisionSimulatorResponse>
    compareScenarios(
            @RequestBody FarmDecisionSimulatorRequest request
    ) {

        FarmDecisionSimulatorResponse response =
                farmDecisionSimulatorService
                        .simulateDecision(request);

        return ResponseEntity.ok(response);
    }
}