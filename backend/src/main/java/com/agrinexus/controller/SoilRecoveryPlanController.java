package com.agrinexus.controller;

import com.agrinexus.dto.SoilRecoveryPlanRequest;
import com.agrinexus.dto.SoilRecoveryPlanResponse;
import com.agrinexus.service.SoilRecoveryPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/soil-recovery-plans")
@RequiredArgsConstructor
public class SoilRecoveryPlanController {

    private final SoilRecoveryPlanService soilRecoveryPlanService;

    /*
     * Create a soil recovery plan.
     *
     * POST /api/soil-recovery-plans
     */
    @PostMapping
    public ResponseEntity<SoilRecoveryPlanResponse> createPlan(
            @RequestBody SoilRecoveryPlanRequest request
    ) {

        SoilRecoveryPlanResponse response =
                soilRecoveryPlanService.createPlan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get a recovery plan by ID.
     *
     * GET /api/soil-recovery-plans/{planId}
     */
    @GetMapping("/{planId}")
    public ResponseEntity<SoilRecoveryPlanResponse> getPlanById(
            @PathVariable UUID planId
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getPlanById(planId)
        );
    }

    /*
     * Get all recovery plans for a farm.
     *
     * GET /api/soil-recovery-plans/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getFarmPlans(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getFarmPlans(farmId)
        );
    }

    /*
     * Get recovery plans for a specific crop.
     *
     * GET /api/soil-recovery-plans/farm/{farmId}/crop/{cropId}
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getCropPlans(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getCropPlans(
                        farmId,
                        cropId
                )
        );
    }

    /*
     * Get plans by status.
     *
     * Example:
     * GET /api/soil-recovery-plans/farm/{farmId}/status/ACTIVE
     */
    @GetMapping("/farm/{farmId}/status/{status}")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getPlansByStatus(
            @PathVariable UUID farmId,
            @PathVariable String status
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getPlansByStatus(
                        farmId,
                        status
                )
        );
    }

    /*
     * Get plans by focus area.
     *
     * Example:
     * GET /api/soil-recovery-plans/farm/{farmId}/focus/ORGANIC_CARBON
     */
    @GetMapping("/farm/{farmId}/focus/{focusArea}")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getPlansByFocusArea(
            @PathVariable UUID farmId,
            @PathVariable String focusArea
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getPlansByFocusArea(
                        farmId,
                        focusArea
                )
        );
    }

    /*
     * Get plans by trend.
     *
     * Example:
     * GET /api/soil-recovery-plans/farm/{farmId}/trend/DECLINING
     */
    @GetMapping("/farm/{farmId}/trend/{trend}")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getPlansByTrend(
            @PathVariable UUID farmId,
            @PathVariable String trend
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getPlansByTrend(
                        farmId,
                        trend
                )
        );
    }

    /*
     * Get latest recovery plan for a farm.
     *
     * GET /api/soil-recovery-plans/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<SoilRecoveryPlanResponse> getLatestFarmPlan(
            @PathVariable UUID farmId
    ) {

        SoilRecoveryPlanResponse response =
                soilRecoveryPlanService.getLatestFarmPlan(
                        farmId
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get latest recovery plan for a crop.
     *
     * GET /api/soil-recovery-plans/farm/{farmId}/crop/{cropId}/latest
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}/latest")
    public ResponseEntity<SoilRecoveryPlanResponse> getLatestCropPlan(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        SoilRecoveryPlanResponse response =
                soilRecoveryPlanService.getLatestCropPlan(
                        farmId,
                        cropId
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get all active recovery plans.
     *
     * GET /api/soil-recovery-plans/farm/{farmId}/active
     */
    @GetMapping("/farm/{farmId}/active")
    public ResponseEntity<List<SoilRecoveryPlanResponse>> getActivePlans(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.getActivePlans(farmId)
        );
    }

    /*
     * Update a recovery plan.
     *
     * PUT /api/soil-recovery-plans/{planId}
     */
    @PutMapping("/{planId}")
    public ResponseEntity<SoilRecoveryPlanResponse> updatePlan(
            @PathVariable UUID planId,
            @RequestBody SoilRecoveryPlanRequest request
    ) {

        return ResponseEntity.ok(
                soilRecoveryPlanService.updatePlan(
                        planId,
                        request
                )
        );
    }

    /*
     * Delete a recovery plan.
     *
     * DELETE /api/soil-recovery-plans/{planId}
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<String> deletePlan(
            @PathVariable UUID planId
    ) {

        soilRecoveryPlanService.deletePlan(planId);

        return ResponseEntity.ok(
                "Soil recovery plan deleted successfully"
        );
    }
}