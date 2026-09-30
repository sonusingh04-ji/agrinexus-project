package com.agrinexus.controller;

import com.agrinexus.dto.SolarPumpResponse;
import com.agrinexus.service.SolarPumpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/solar-pump")
@RequiredArgsConstructor
public class SolarPumpController {

    private final SolarPumpService service;

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<SolarPumpResponse> evaluate(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.evaluate(farmId)
        );
    }
}