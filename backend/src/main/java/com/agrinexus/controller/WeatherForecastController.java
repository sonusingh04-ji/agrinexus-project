package com.agrinexus.controller;

import com.agrinexus.dto.WeatherForecastRequest;
import com.agrinexus.dto.WeatherForecastResponse;
import com.agrinexus.service.WeatherForecastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weather-forecasts")
@RequiredArgsConstructor
public class WeatherForecastController {

    private final WeatherForecastService weatherForecastService;

    @PostMapping
    public ResponseEntity<WeatherForecastResponse>
    createWeatherForecast(
            @Valid @RequestBody WeatherForecastRequest request
    ) {

        WeatherForecastResponse response =
                weatherForecastService
                        .createWeatherForecast(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<WeatherForecastResponse>>
    getWeatherForecastsByFarmId(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                weatherForecastService
                        .getWeatherForecastsByFarmId(farmId)
        );
    }
}