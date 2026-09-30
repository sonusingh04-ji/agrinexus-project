package com.agrinexus.controller;

import com.agrinexus.dto.LowestCostSafePlanRequest;
import com.agrinexus.dto.LowestCostSafePlanResponse;
import com.agrinexus.service.LowestCostSafePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lowest-cost-safe-plan")
@RequiredArgsConstructor
public class LowestCostSafePlanController {

    private final LowestCostSafePlanService service;


    /*
     * Generate the lowest-cost acceptable plan.
     *
     * POST /api/lowest-cost-safe-plan/evaluate
     */
    @PostMapping("/evaluate")
    public ResponseEntity<LowestCostSafePlanResponse> evaluate(
            @RequestBody LowestCostSafePlanRequest request
    ) {

        return ResponseEntity.ok(
                service.evaluate(request)
        );
    }
}