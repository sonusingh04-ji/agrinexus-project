package com.agrinexus.controller;

import com.agrinexus.dto.IrrigationConfigRequest;
import com.agrinexus.dto.IrrigationConfigResponse;
import com.agrinexus.service.IrrigationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/irrigation-config")
@RequiredArgsConstructor
public class IrrigationConfigController {

    private final IrrigationConfigService service;

    @PostMapping
    public ResponseEntity<IrrigationConfigResponse> create(
            @RequestBody IrrigationConfigRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<IrrigationConfigResponse> getByFarm(
            @PathVariable UUID farmId
    ) {

        IrrigationConfigResponse response =
                service.getByFarmId(farmId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/farm/{farmId}")
    public ResponseEntity<IrrigationConfigResponse> update(
            @PathVariable UUID farmId,
            @RequestBody IrrigationConfigRequest request
    ) {

        return ResponseEntity.ok(
                service.update(
                        farmId,
                        request
                )
        );
    }

    @DeleteMapping("/farm/{farmId}")
    public ResponseEntity<String> delete(
            @PathVariable UUID farmId
    ) {

        service.delete(farmId);

        return ResponseEntity.ok(
                "Irrigation configuration deleted successfully"
        );
    }
}