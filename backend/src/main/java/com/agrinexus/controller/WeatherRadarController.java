    package com.agrinexus.controller;

    import com.agrinexus.dto.WeatherRadarResponse;
    import com.agrinexus.service.WeatherRadarService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.UUID;

    @RestController
    @RequestMapping("/api/weather-radar")
    @RequiredArgsConstructor
    public class WeatherRadarController {

        private final WeatherRadarService weatherRadarService;


        /*
         * Get complete Weather Risk Radar data for a farm.
         *
         * Includes:
         * - Farm information
         * - Latest weather
         * - Latest satellite data
         * - Combined risk analysis
         * - Active weather risks
         * - Unread farmer alerts
         * - Overall summary
         */
        @GetMapping("/farm/{farmId}")
        public ResponseEntity<WeatherRadarResponse> getWeatherRadar(
                @PathVariable UUID farmId
        ) {

            WeatherRadarResponse response =
                    weatherRadarService.getWeatherRadar(farmId);

            return ResponseEntity.ok(response);
        }
    }