package com.agrinexus.controller;

import com.agrinexus.dto.FarmerAlertResponse;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.service.FarmerAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/farmer-alerts")
@RequiredArgsConstructor
public class FarmerAlertController {

    private final FarmerAlertService farmerAlertService;

    /*
     * Get all alerts for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FarmerAlertResponse>> getFarmAlerts(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                farmerAlertService.getFarmAlerts(farmId)
        );
    }

    /*
     * Get unread alerts for a farm.
     */
    @GetMapping("/farm/{farmId}/unread")
    public ResponseEntity<List<FarmerAlertResponse>> getUnreadAlerts(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                farmerAlertService.getUnreadAlerts(farmId)
        );
    }

    /*
     * Get alerts filtered by risk level.
     *
     * Example:
     * /api/farmer-alerts/farm/{farmId}/level/CRITICAL
     */
    @GetMapping("/farm/{farmId}/level/{riskLevel}")
    public ResponseEntity<List<FarmerAlertResponse>> getAlertsByRiskLevel(
            @PathVariable UUID farmId,
            @PathVariable RiskLevel riskLevel
    ) {

        return ResponseEntity.ok(
                farmerAlertService.getAlertsByRiskLevel(
                        farmId,
                        riskLevel
                )
        );
    }

    /*
     * Mark an alert as read.
     */
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<FarmerAlertResponse> markAlertAsRead(
            @PathVariable UUID alertId
    ) {

        return ResponseEntity.ok(
                farmerAlertService.markAlertAsRead(alertId)
        );
    }

    /*
     * Manually generate all important alerts for a farm.
     */
    @PostMapping("/farm/{farmId}/generate")
    public ResponseEntity<String> generateAllAlerts(
            @PathVariable UUID farmId
    ) {

        farmerAlertService.generateAllAlerts(farmId);

        return ResponseEntity.ok(
                "Farmer alerts generated successfully"
        );
    }
    @DeleteMapping("/farm/{farmId}/clear")
    public ResponseEntity<String> clearFarmAlerts(
            @PathVariable UUID farmId
    ) {

        long deletedCount =
                farmerAlertService.deleteAllFarmAlerts(farmId);

        return ResponseEntity.ok(
                deletedCount + " farmer alerts deleted successfully"
        );
    }
}