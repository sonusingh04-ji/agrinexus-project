package com.agrinexus.controller;

import com.agrinexus.dto.FieldHealthPassportResponse;
import com.agrinexus.service.FieldHealthPassportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/field-health-passport")
@RequiredArgsConstructor
public class FieldHealthPassportController {

    private final FieldHealthPassportService service;


    /*
     * Get complete long-term Field Health Passport.
     *
     * GET /api/field-health-passport/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<FieldHealthPassportResponse> getPassport(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getPassport(farmId)
        );
    }
}