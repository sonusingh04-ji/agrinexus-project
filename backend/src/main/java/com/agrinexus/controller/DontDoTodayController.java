package com.agrinexus.controller;

import com.agrinexus.dto.DontDoTodayResponse;
import com.agrinexus.service.DontDoTodayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dont-do-today")
@RequiredArgsConstructor
public class DontDoTodayController {

    private final DontDoTodayService service;

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<DontDoTodayResponse> evaluate(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.evaluate(farmId)
        );
    }
}