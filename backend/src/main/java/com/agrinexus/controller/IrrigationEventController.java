package com.agrinexus.controller;

import com.agrinexus.dto.IrrigationEventRequest;
import com.agrinexus.dto.IrrigationEventResponse;
import com.agrinexus.service.IrrigationEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/irrigation-events")
@RequiredArgsConstructor
public class IrrigationEventController {

    private final IrrigationEventService service;

    @PostMapping
    public ResponseEntity<IrrigationEventResponse> create(
            @RequestBody IrrigationEventRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createEvent(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IrrigationEventResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<IrrigationEventResponse>> getFarmEvents(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getFarmEvents(farmId)
        );
    }

    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<IrrigationEventResponse>> getRange(
            @PathVariable UUID farmId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {

        return ResponseEntity.ok(
                service.getRange(
                        farmId,
                        start,
                        end
                )
        );
    }
}