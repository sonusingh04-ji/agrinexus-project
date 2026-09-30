package com.agrinexus.controller;

import com.agrinexus.dto.TillageAdvisorRequest;
import com.agrinexus.dto.TillageAdvisorResponse;
import com.agrinexus.service.TillageAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tillage-advisor")
@RequiredArgsConstructor
public class TillageAdvisorController {

    private final TillageAdvisorService tillageAdvisorService;

    /*
     * Generate tillage advice for a farm.
     *
     * POST /api/tillage-advisor/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<TillageAdvisorResponse> getTillageAdvice(
            @RequestBody TillageAdvisorRequest request
    ) {

        TillageAdvisorResponse response =
                tillageAdvisorService.getTillageAdvice(request);

        return ResponseEntity.ok(response);
    }
}