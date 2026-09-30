package com.agrinexus.controller;

import com.agrinexus.dto.LulcResponse;
import com.agrinexus.service.LulcMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lulc")
@RequiredArgsConstructor
public class LulcController {

    private final LulcMappingService
            lulcMappingService;


    /*
     * =========================================================
     * FETCH REAL COPERNICUS LULC DATA
     * =========================================================
     *
     * POST
     *
     * /api/lulc/farm/{farmId}/sync
     */

    @PostMapping(
            "/farm/{farmId}/sync"
    )
    public ResponseEntity<LulcResponse> syncFarm(
            @PathVariable UUID farmId
    ) {

        LulcResponse response =
                lulcMappingService
                        .syncFarm(
                                farmId
                        );


        return ResponseEntity.ok(
                response
        );
    }


    /*
     * =========================================================
     * LATEST STORED LULC
     * =========================================================
     *
     * GET
     *
     * /api/lulc/farm/{farmId}
     */

    @GetMapping(
            "/farm/{farmId}"
    )
    public ResponseEntity<LulcResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        LulcResponse response =
                lulcMappingService
                        .getLatest(
                                farmId
                        );


        return ResponseEntity.ok(
                response
        );
    }


    /*
     * =========================================================
     * HISTORY
     * =========================================================
     *
     * GET
     *
     * /api/lulc/farm/{farmId}/history
     */

    @GetMapping(
            "/farm/{farmId}/history"
    )
    public ResponseEntity<List<LulcResponse>> getHistory(
            @PathVariable UUID farmId
    ) {

        List<LulcResponse> response =
                lulcMappingService
                        .getHistory(
                                farmId
                        );


        return ResponseEntity.ok(
                response
        );
    }
}