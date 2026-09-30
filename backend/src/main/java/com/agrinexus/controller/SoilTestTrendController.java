package com.agrinexus.controller;

import com.agrinexus.dto.SoilTestTrendResponse;
import com.agrinexus.service.SoilTestTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/soil-test-trends")
@RequiredArgsConstructor
public class SoilTestTrendController {

    private final SoilTestTrendService service;


    /*
     * Get soil-test trends and reminder status.
     *
     * GET /api/soil-test-trends/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<SoilTestTrendResponse> getTrend(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getSoilTestTrend(farmId)
        );
    }
}