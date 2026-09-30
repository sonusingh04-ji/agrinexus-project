package com.agrinexus.controller;

import com.agrinexus.dto.AIContextRequest;
import com.agrinexus.dto.AIContextResponse;
import com.agrinexus.service.AIContextSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/context")
@RequiredArgsConstructor
public class AIContextSnapshotController {

    private final AIContextSnapshotService service;

    @PostMapping
    public ResponseEntity<AIContextResponse> create(
            @RequestBody AIContextRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AIContextResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<AIContextResponse> getLatest(
            @PathVariable UUID farmId
    ) {

        AIContextResponse response =
                service.getLatestByFarm(farmId);

        if (response == null) {
            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(response);
    }
}