package com.agrinexus.controller;

import com.agrinexus.dto.*;
import com.agrinexus.service.VillageCooperativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/village-cooperatives")
@RequiredArgsConstructor
public class VillageCooperativeController {

    private final VillageCooperativeService service;


    /*
     * =========================================================
     * CREATE COOPERATIVE
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<VillageCooperativeResponse>
    createCooperative(
            @RequestBody VillageCooperativeRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.createCooperative(
                                request
                        )
                );
    }


    /*
     * GET COOPERATIVE
     */

    @GetMapping("/{cooperativeId}")
    public ResponseEntity<VillageCooperativeResponse>
    getCooperative(
            @PathVariable UUID cooperativeId
    ) {

        return ResponseEntity.ok(
                service.getCooperative(
                        cooperativeId
                )
        );
    }


    /*
     * GET BY VILLAGE
     */

    @GetMapping("/village/{village}")
    public ResponseEntity<List<VillageCooperativeResponse>>
    getByVillage(
            @PathVariable String village
    ) {

        return ResponseEntity.ok(
                service.getByVillage(
                        village
                )
        );
    }


    /*
     * GET ACTIVE
     */

    @GetMapping("/active")
    public ResponseEntity<List<VillageCooperativeResponse>>
    getActiveCooperatives() {

        return ResponseEntity.ok(
                service.getActiveCooperatives()
        );
    }


    /*
     * UPDATE
     */

    @PutMapping("/{cooperativeId}")
    public ResponseEntity<VillageCooperativeResponse>
    updateCooperative(
            @PathVariable UUID cooperativeId,
            @RequestBody VillageCooperativeRequest request
    ) {

        return ResponseEntity.ok(
                service.updateCooperative(
                        cooperativeId,
                        request
                )
        );
    }


    /*
     * DEACTIVATE
     */

    @DeleteMapping("/{cooperativeId}")
    public ResponseEntity<String>
    deactivateCooperative(
            @PathVariable UUID cooperativeId
    ) {

        service.deactivateCooperative(
                cooperativeId
        );

        return ResponseEntity.ok(
                "Village cooperative deactivated successfully"
        );
    }


    /*
     * =========================================================
     * MEMBERS
     * =========================================================
     */

    @PostMapping("/members")
    public ResponseEntity<CooperativeMemberResponse>
    addMember(
            @RequestBody CooperativeMemberRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.addMember(
                                request
                        )
                );
    }


    @GetMapping("/{cooperativeId}/members")
    public ResponseEntity<List<CooperativeMemberResponse>>
    getMembers(
            @PathVariable UUID cooperativeId
    ) {

        return ResponseEntity.ok(
                service.getMembers(
                        cooperativeId
                )
        );
    }


    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<String>
    removeMember(
            @PathVariable UUID memberId
    ) {

        service.removeMember(
                memberId
        );

        return ResponseEntity.ok(
                "Cooperative member deactivated successfully"
        );
    }


    /*
     * =========================================================
     * SHARED RESOURCES
     * =========================================================
     */

    @PostMapping("/resources")
    public ResponseEntity<SharedFarmResourceResponse>
    addResource(
            @RequestBody SharedFarmResourceRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.addResource(
                                request
                        )
                );
    }


    @GetMapping("/{cooperativeId}/resources")
    public ResponseEntity<List<SharedFarmResourceResponse>>
    getResources(
            @PathVariable UUID cooperativeId
    ) {

        return ResponseEntity.ok(
                service.getResources(
                        cooperativeId
                )
        );
    }


    @PutMapping("/resources/{resourceId}/status")
    public ResponseEntity<SharedFarmResourceResponse>
    updateResourceStatus(
            @PathVariable UUID resourceId,
            @RequestParam String status
    ) {

        return ResponseEntity.ok(
                service.updateResourceStatus(
                        resourceId,
                        status
                )
        );
    }


    /*
     * =========================================================
     * RESOURCE USAGE
     * =========================================================
     */

    @PostMapping("/usage")
    public ResponseEntity<SharedResourceUsageResponse>
    recordUsage(
            @RequestBody SharedResourceUsageRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.recordUsage(
                                request
                        )
                );
    }


    @GetMapping("/resources/{resourceId}/usage")
    public ResponseEntity<List<SharedResourceUsageResponse>>
    getResourceUsage(
            @PathVariable UUID resourceId
    ) {

        return ResponseEntity.ok(
                service.getResourceUsage(
                        resourceId
                )
        );
    }


    @GetMapping("/farms/{farmId}/usage")
    public ResponseEntity<List<SharedResourceUsageResponse>>
    getFarmUsage(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getFarmUsage(
                        farmId
                )
        );
    }


    @GetMapping("/farmers/{farmerId}/usage")
    public ResponseEntity<List<SharedResourceUsageResponse>>
    getFarmerUsage(
            @PathVariable UUID farmerId
    ) {

        return ResponseEntity.ok(
                service.getFarmerUsage(
                        farmerId
                )
        );
    }


    /*
     * =========================================================
     * SUMMARY
     * =========================================================
     */

    @GetMapping("/{cooperativeId}/summary")
    public ResponseEntity<CooperativeSummaryResponse>
    getSummary(
            @PathVariable UUID cooperativeId
    ) {

        return ResponseEntity.ok(
                service.getSummary(
                        cooperativeId
                )
        );
    }
}