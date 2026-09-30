package com.agrinexus.controller;

import com.agrinexus.dto.SmartIrrigationRequest;
import com.agrinexus.dto.SmartIrrigationResponse;
import com.agrinexus.service.SmartIrrigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/smart-irrigation")
@RequiredArgsConstructor
public class SmartIrrigationController {

    private final SmartIrrigationService service;

    @PostMapping("/evaluate")
    public ResponseEntity<SmartIrrigationResponse> evaluate(
            @RequestBody SmartIrrigationRequest request
    ) {

        return ResponseEntity.ok(
                service.evaluate(request)
        );
    }
}