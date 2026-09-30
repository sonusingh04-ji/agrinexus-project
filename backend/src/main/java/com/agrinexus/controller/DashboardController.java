package com.agrinexus.controller;

import com.agrinexus.dto.DashboardResponse;
import com.agrinexus.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<DashboardResponse> getFarmDashboard(
            @PathVariable UUID farmId
    ) {

        DashboardResponse dashboard =
                dashboardService.getFarmDashboard(farmId);

        return ResponseEntity.ok(dashboard);
    }
}