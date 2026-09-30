package com.agrinexus.controller;

import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.service.CopernicusSatelliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/satellite-observations")
@RequiredArgsConstructor
public class CopernicusSatelliteController {

    private final CopernicusSatelliteService
            copernicusSatelliteService;


    /*
     * ============================================================
     * REAL SATELLITE SYNC
     * ============================================================
     *
     * POST
     *
     * /api/satellite-observations/farm/{farmId}/sync
     */
    @PostMapping("/farm/{farmId}/sync")
    public ResponseEntity<SatelliteObservation> sync(
            @PathVariable UUID farmId
    ) {

        SatelliteObservation observation =
                copernicusSatelliteService
                        .syncFarm(
                                farmId
                        );

        return ResponseEntity.ok(
                observation
        );
    }
}