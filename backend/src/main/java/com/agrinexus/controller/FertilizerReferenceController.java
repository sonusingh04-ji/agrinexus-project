package com.agrinexus.controller;

import com.agrinexus.dto.FertilizerReferenceRequest;
import com.agrinexus.dto.FertilizerReferenceResponse;
import com.agrinexus.service.FertilizerReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fertilizer-references")
@RequiredArgsConstructor
public class FertilizerReferenceController {

    private final FertilizerReferenceService
            fertilizerReferenceService;


    /*
     * =============================================================
     * CREATE
     *
     * POST /api/fertilizer-references
     *
     * New references always start as REVIEW_REQUIRED.
     * =============================================================
     */
    @PostMapping
    public ResponseEntity<FertilizerReferenceResponse>
    createReference(
            @Valid
            @RequestBody
            FertilizerReferenceRequest request
    ) {

        FertilizerReferenceResponse response =
                fertilizerReferenceService
                        .createReference(
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /*
     * =============================================================
     * GET BY ID
     *
     * GET /api/fertilizer-references/{referenceId}
     * =============================================================
     */
    @GetMapping("/{referenceId}")
    public ResponseEntity<FertilizerReferenceResponse>
    getReferenceById(
            @PathVariable UUID referenceId
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .getReferenceById(
                                referenceId
                        )
        );
    }


    /*
     * =============================================================
     * GET VERIFIED REFERENCES FOR CROP
     *
     * GET
     * /api/fertilizer-references/crop/{cropId}
     * =============================================================
     */
    @GetMapping("/crop/{cropId}")
    public ResponseEntity<List<FertilizerReferenceResponse>>
    getVerifiedReferences(
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .getVerifiedReferences(
                                cropId
                        )
        );
    }


    /*
     * =============================================================
     * GET VERIFIED REFERENCES FOR CROP + REGION
     *
     * GET
     * /api/fertilizer-references/crop/{cropId}/region/{region}
     * =============================================================
     */
    @GetMapping("/crop/{cropId}/region/{region}")
    public ResponseEntity<List<FertilizerReferenceResponse>>
    getVerifiedReferencesByRegion(
            @PathVariable UUID cropId,
            @PathVariable String region
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .getVerifiedReferences(
                                cropId,
                                region
                        )
        );
    }


    /*
     * =============================================================
     * UPDATE
     *
     * PUT /api/fertilizer-references/{referenceId}
     *
     * REVIEW_REQUIRED references can be edited.
     * VERIFIED references cannot be directly edited.
     * =============================================================
     */
    @PutMapping("/{referenceId}")
    public ResponseEntity<FertilizerReferenceResponse>
    updateReference(
            @PathVariable UUID referenceId,
            @Valid
            @RequestBody
            FertilizerReferenceRequest request
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .updateReference(
                                referenceId,
                                request
                        )
        );
    }


    /*
     * =============================================================
     * VERIFY
     *
     * PATCH /api/fertilizer-references/{referenceId}/verify
     *
     * REVIEW_REQUIRED -> VERIFIED
     * =============================================================
     */
    @PatchMapping("/{referenceId}/verify")
    public ResponseEntity<FertilizerReferenceResponse>
    verifyReference(
            @PathVariable UUID referenceId
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .verifyReference(
                                referenceId
                        )
        );
    }


    /*
     * =============================================================
     * RETIRE
     *
     * PATCH /api/fertilizer-references/{referenceId}/retire
     *
     * We keep the record for provenance instead of deleting it.
     * =============================================================
     */
    @PatchMapping("/{referenceId}/retire")
    public ResponseEntity<FertilizerReferenceResponse>
    retireReference(
            @PathVariable UUID referenceId
    ) {

        return ResponseEntity.ok(
                fertilizerReferenceService
                        .retireReference(
                                referenceId
                        )
        );
    }
}