package com.agrinexus.controller;

import com.agrinexus.dto.AgriculturalReferenceRequest;
import com.agrinexus.dto.AgriculturalReferenceResponse;
import com.agrinexus.enums.AgriculturalReferenceType;
import com.agrinexus.service.AgriculturalReferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agricultural-references")
@RequiredArgsConstructor
public class AgriculturalReferenceController {

    private final AgriculturalReferenceService service;

    @PostMapping
    public ResponseEntity<AgriculturalReferenceResponse> create(
            @Valid @RequestBody AgriculturalReferenceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgriculturalReferenceResponse> getById(
            @PathVariable UUID id
    ) {
        AgriculturalReferenceResponse response = service.getById(id);
        return response == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(response);
    }

    @GetMapping("/crop/{cropId}")
    public ResponseEntity<List<AgriculturalReferenceResponse>> getByCrop(
            @PathVariable UUID cropId
    ) {
        return ResponseEntity.ok(service.getByCrop(cropId));
    }

    @GetMapping("/crop/{cropId}/type/{referenceType}")
    public ResponseEntity<List<AgriculturalReferenceResponse>> getByCropAndType(
            @PathVariable UUID cropId,
            @PathVariable AgriculturalReferenceType referenceType
    ) {
        return ResponseEntity.ok(service.getByCropAndType(cropId, referenceType));
    }

    @GetMapping("/resolve")
    public ResponseEntity<AgriculturalReferenceResponse> resolve(
            @RequestParam UUID cropId,
            @RequestParam AgriculturalReferenceType referenceType,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String districtName,
            @RequestParam(required = false) String subDistrictName,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String cropStage
    ) {
        AgriculturalReferenceResponse response = service.resolve(
                cropId,
                referenceType,
                stateName,
                districtName,
                subDistrictName,
                season,
                cropStage
        );

        return response == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgriculturalReferenceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AgriculturalReferenceRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
