package com.agrinexus.controller;

import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.service.LiveWeatherForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weather-forecast/live")
@RequiredArgsConstructor
public class LiveWeatherForecastController {

    private final LiveWeatherForecastService
            liveWeatherForecastService;

    /*
     * =========================================================
     * FETCH + SAVE LIVE 7-DAY FORECAST
     * =========================================================
     *
     * POST /api/weather-forecast/live/farm/{farmId}
     */

    @PostMapping("/farm/{farmId}")
    public ResponseEntity<List<WeatherForecastData>>
    synchronizeForecast(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                liveWeatherForecastService
                        .fetchAndSaveForecast(
                                farmId
                        )
        );
    }
}