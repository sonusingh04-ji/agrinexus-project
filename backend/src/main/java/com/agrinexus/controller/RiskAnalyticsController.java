package com.agrinexus.controller;

import com.agrinexus.dto.RiskAnalyticsResponse;
import com.agrinexus.service.RiskAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/risk-analytics")
@RequiredArgsConstructor
public class RiskAnalyticsController {

    private final RiskAnalyticsService riskAnalyticsService;


    /*
     * Get complete risk analytics for a farm.
     *
     * Includes:
     * - Current risk
     * - Average risk score
     * - Highest risk score
     * - Lowest risk score
     * - Total analyses
     * - Risk level counts
     * - Historical risk data
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<RiskAnalyticsResponse>
    getFarmRiskAnalytics(
            @PathVariable UUID farmId
    ) {

        RiskAnalyticsResponse response =
                riskAnalyticsService
                        .getFarmRiskAnalytics(farmId);

        return ResponseEntity.ok(response);
    }
}