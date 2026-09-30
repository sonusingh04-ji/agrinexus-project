package com.agrinexus.controller;

import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.mapper.WeatherRecordMapper;
import com.agrinexus.dto.WeatherRecordResponse;
import com.agrinexus.service.LiveWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/weather/live")
@RequiredArgsConstructor
public class LiveWeatherController {

    private final LiveWeatherService liveWeatherService;

    @PostMapping("/farm/{farmId}")
    public ResponseEntity<WeatherRecordResponse>
    fetchCurrentWeather(
            @PathVariable UUID farmId
    ) {

        WeatherRecord weatherRecord =
                liveWeatherService
                        .fetchAndSaveCurrentWeather(
                                farmId
                        );

        return ResponseEntity.ok(
                WeatherRecordMapper.toResponse(
                        weatherRecord
                )
        );
    }
}