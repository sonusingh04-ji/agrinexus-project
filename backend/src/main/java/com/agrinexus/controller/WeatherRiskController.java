    package com.agrinexus.controller;

    import com.agrinexus.dto.WeatherRiskRequest;
    import com.agrinexus.dto.WeatherRiskResponse;
    import com.agrinexus.enums.RiskLevel;
    import com.agrinexus.service.WeatherRiskService;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;
    import java.util.UUID;

    @RestController
    @RequestMapping("/api/weather-risks")
    @RequiredArgsConstructor
    public class WeatherRiskController {

        private final WeatherRiskService weatherRiskService;

        @PostMapping
        public ResponseEntity<WeatherRiskResponse> createWeatherRisk(
                @Valid @RequestBody WeatherRiskRequest request
        ) {
            WeatherRiskResponse response =
                    weatherRiskService.createWeatherRisk(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);
        }

        @GetMapping("/farm/{farmId}")
        public ResponseEntity<List<WeatherRiskResponse>>
        getRisksByFarmId(@PathVariable UUID farmId) {

            return ResponseEntity.ok(
                    weatherRiskService.getRisksByFarmId(farmId)
            );
        }

        @GetMapping("/farm/{farmId}/level/{riskLevel}")
        public ResponseEntity<List<WeatherRiskResponse>>
        getRisksByFarmIdAndLevel(
                @PathVariable UUID farmId,
                @PathVariable RiskLevel riskLevel
        ) {

            return ResponseEntity.ok(
                    weatherRiskService.getRisksByFarmIdAndLevel(
                            farmId,
                            riskLevel
                    )
            );
        }
    }