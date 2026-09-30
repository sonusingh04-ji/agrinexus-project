package com.agrinexus.controller;

import com.agrinexus.dto.WeatherRecordRequest;
import com.agrinexus.dto.WeatherRecordResponse;
import com.agrinexus.service.WeatherRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherRecordController {

    private final WeatherRecordService weatherRecordService;

    @PostMapping
    public ResponseEntity<WeatherRecordResponse> createWeatherRecord(
            @Valid @RequestBody WeatherRecordRequest request
    ) {
        WeatherRecordResponse response =
                weatherRecordService.createWeatherRecord(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<WeatherRecordResponse>>
    getWeatherRecordsByFarmId(@PathVariable UUID farmId) {

        return ResponseEntity.ok(
                weatherRecordService.getWeatherRecordsByFarmId(farmId)
        );
    }
}