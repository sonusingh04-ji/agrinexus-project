package com.agrinexus.controller;

import com.agrinexus.dto.NutrientBalanceRequest;
import com.agrinexus.dto.NutrientBalanceResponse;
import com.agrinexus.service.NutrientBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nutrient-balances")
@RequiredArgsConstructor
public class NutrientBalanceController {

    private final NutrientBalanceService nutrientBalanceService;

    /*
     * =============================================================
     * CREATE NUTRIENT BALANCE
     * =============================================================
     *
     * POST /api/nutrient-balances
     *
     * Creates a nutrient balance from explicitly supplied
     * accounting components.
     */
    @PostMapping
    public ResponseEntity<NutrientBalanceResponse> createNutrientBalance(
            @RequestBody NutrientBalanceRequest request
    ) {

        NutrientBalanceResponse response =
                nutrientBalanceService.createNutrientBalance(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /*
     * =============================================================
     * GENERATE FROM REAL FERTILIZER INPUTS
     * =============================================================
     *
     * POST
     * /api/nutrient-balances/farm/{farmId}/generate-from-fertilizer-inputs
     *
     * Optional:
     * ?cropId=<realCropId>
     *
     * This endpoint aggregates only nutrient values that are
     * already stored in real FertilizerInput records.
     *
     * It does NOT fabricate:
     * - crop uptake
     * - residue nutrient return
     * - biological nitrogen fixation
     * - net nutrient balance
     *
     * If there are no real fertilizer-input records with usable
     * dates, the endpoint returns 204 No Content.
     */
    @PostMapping(
            "/farm/{farmId}/generate-from-fertilizer-inputs"
    )
    public ResponseEntity<NutrientBalanceResponse>
    generateFromFertilizerInputs(
            @PathVariable UUID farmId,

            @RequestParam(
                    required = false
            )
            UUID cropId
    ) {

        NutrientBalanceResponse response =
                nutrientBalanceService
                        .generateFromFertilizerInputs(
                                farmId,
                                cropId
                        );

        if (response == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /*
     * =============================================================
     * GET NUTRIENT BALANCE BY ID
     * =============================================================
     *
     * GET /api/nutrient-balances/{balanceId}
     */
    @GetMapping("/{balanceId}")
    public ResponseEntity<NutrientBalanceResponse>
    getNutrientBalanceById(
            @PathVariable UUID balanceId
    ) {

        return ResponseEntity.ok(
                nutrientBalanceService
                        .getNutrientBalanceById(
                                balanceId
                        )
        );
    }


    /*
     * =============================================================
     * GET ALL NUTRIENT BALANCES FOR A FARM
     * =============================================================
     *
     * GET /api/nutrient-balances/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<NutrientBalanceResponse>>
    getFarmNutrientBalances(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                nutrientBalanceService
                        .getFarmNutrientBalances(
                                farmId
                        )
        );
    }


    /*
     * =============================================================
     * GET NUTRIENT BALANCES FOR A SPECIFIC CROP
     * =============================================================
     *
     * GET
     * /api/nutrient-balances/farm/{farmId}/crop/{cropId}
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<List<NutrientBalanceResponse>>
    getCropNutrientBalances(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                nutrientBalanceService
                        .getCropNutrientBalances(
                                farmId,
                                cropId
                        )
        );
    }


    /*
     * =============================================================
     * GET LATEST FARM BALANCE
     * =============================================================
     *
     * GET /api/nutrient-balances/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<NutrientBalanceResponse>
    getLatestFarmBalance(
            @PathVariable UUID farmId
    ) {

        NutrientBalanceResponse response =
                nutrientBalanceService
                        .getLatestFarmBalance(
                                farmId
                        );

        if (response == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                response
        );
    }


    /*
     * =============================================================
     * GET LATEST CROP BALANCE
     * =============================================================
     *
     * GET
     * /api/nutrient-balances/farm/{farmId}/crop/{cropId}/latest
     */
    @GetMapping(
            "/farm/{farmId}/crop/{cropId}/latest"
    )
    public ResponseEntity<NutrientBalanceResponse>
    getLatestCropBalance(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        NutrientBalanceResponse response =
                nutrientBalanceService
                        .getLatestCropBalance(
                                farmId,
                                cropId
                        );

        if (response == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                response
        );
    }


    /*
     * =============================================================
     * GET BALANCES BY DATE RANGE
     * =============================================================
     *
     * GET
     * /api/nutrient-balances/farm/{farmId}/range
     *
     * ?startDate=2025-01-01
     * &endDate=2026-08-28
     */
    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<NutrientBalanceResponse>>
    getBalancesByDateRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                nutrientBalanceService
                        .getBalancesByDateRange(
                                farmId,
                                startDate,
                                endDate
                        )
        );
    }


    /*
     * =============================================================
     * UPDATE NUTRIENT BALANCE
     * =============================================================
     *
     * PUT /api/nutrient-balances/{balanceId}
     */
    @PutMapping("/{balanceId}")
    public ResponseEntity<NutrientBalanceResponse>
    updateNutrientBalance(
            @PathVariable UUID balanceId,
            @RequestBody NutrientBalanceRequest request
    ) {

        return ResponseEntity.ok(
                nutrientBalanceService
                        .updateNutrientBalance(
                                balanceId,
                                request
                        )
        );
    }


    /*
     * =============================================================
     * DELETE NUTRIENT BALANCE
     * =============================================================
     *
     * DELETE /api/nutrient-balances/{balanceId}
     */
    @DeleteMapping("/{balanceId}")
    public ResponseEntity<String>
    deleteNutrientBalance(
            @PathVariable UUID balanceId
    ) {

        nutrientBalanceService
                .deleteNutrientBalance(
                        balanceId
                );

        return ResponseEntity.ok(
                "Nutrient balance deleted successfully"
        );
    }
}