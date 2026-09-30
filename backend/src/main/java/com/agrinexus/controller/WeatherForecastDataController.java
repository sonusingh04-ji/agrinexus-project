package com.agrinexus.controller;

import com.agrinexus.dto.WeatherForecastDataRequest;
import com.agrinexus.dto.WeatherForecastDataResponse;
import com.agrinexus.service.WeatherForecastDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weather-forecast")
@RequiredArgsConstructor
public class WeatherForecastDataController {

    private final WeatherForecastDataService service;

    @PostMapping
    public ResponseEntity<WeatherForecastDataResponse> create(
            @RequestBody WeatherForecastDataRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<WeatherForecastDataResponse>>
    getFarmForecast(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getFarmForecast(farmId)
        );
    }

    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<WeatherForecastDataResponse>>
    getRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                service.getForecastRange(
                        farmId,
                        startDate,
                        endDate
                )
        );
    }
}