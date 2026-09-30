package com.agrinexus.service;

import com.agrinexus.dto.WeatherForecastRequest;
import com.agrinexus.dto.WeatherForecastResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherForecast;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherForecastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherForecastService {

    private final WeatherForecastRepository weatherForecastRepository;
    private final FarmRepository farmRepository;

    private final FutureWeatherRiskPredictionService
            futureWeatherRiskPredictionService;

    public WeatherForecastResponse createWeatherForecast(
            WeatherForecastRequest request
    ) {

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        WeatherForecast weatherForecast =
                WeatherForecast.builder()
                        .farm(farm)
                        .forecastFor(request.getForecastFor())
                        .temperature(request.getTemperature())
                        .feelsLike(request.getFeelsLike())
                        .humidity(request.getHumidity())
                        .rainfall(request.getRainfall())
                        .windSpeed(request.getWindSpeed())
                        .weatherCondition(
                                request.getWeatherCondition()
                        )
                        .weatherDescription(
                                request.getWeatherDescription()
                        )
                        .createdAt(LocalDateTime.now())
                        .build();

        WeatherForecast savedForecast =
                weatherForecastRepository.save(weatherForecast);

        // Automatically predict future weather risks
        futureWeatherRiskPredictionService
                .predictFutureRisks(savedForecast);

        return toResponse(savedForecast);
    }

    public List<WeatherForecastResponse>
    getWeatherForecastsByFarmId(UUID farmId) {

        return weatherForecastRepository
                .findByFarmIdOrderByForecastForAsc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private WeatherForecastResponse toResponse(
            WeatherForecast weatherForecast
    ) {

        return WeatherForecastResponse.builder()
                .id(weatherForecast.getId())
                .farmId(weatherForecast.getFarm().getId())
                .forecastFor(weatherForecast.getForecastFor())
                .temperature(weatherForecast.getTemperature())
                .feelsLike(weatherForecast.getFeelsLike())
                .humidity(weatherForecast.getHumidity())
                .rainfall(weatherForecast.getRainfall())
                .windSpeed(weatherForecast.getWindSpeed())
                .weatherCondition(
                        weatherForecast.getWeatherCondition()
                )
                .weatherDescription(
                        weatherForecast.getWeatherDescription()
                )
                .createdAt(weatherForecast.getCreatedAt())
                .build();
    }
}