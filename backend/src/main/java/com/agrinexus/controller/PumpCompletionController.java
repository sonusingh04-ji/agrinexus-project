package com.agrinexus.controller;

import com.agrinexus.dto.PumpCompletionResponse;
import com.agrinexus.service.PumpCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pump-completion")
@RequiredArgsConstructor
public class PumpCompletionController {

    private final PumpCompletionService service;

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<PumpCompletionResponse> evaluate(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.evaluate(farmId)
        );
    }
}