package com.agrinexus.service;

import com.agrinexus.dto.WeatherForecastDataRequest;
import com.agrinexus.dto.WeatherForecastDataResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherForecastDataService {

    private final WeatherForecastDataRepository repository;
    private final FarmRepository farmRepository;

    public WeatherForecastDataResponse create(
            WeatherForecastDataRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        WeatherForecastData data =
                WeatherForecastData.builder()
                        .farm(farm)
                        .forecastDate(
                                request.getForecastDate()
                        )
                        .rainfallMm(
                                request.getRainfallMm()
                        )
                        .precipitationProbability(
                                request.getPrecipitationProbability()
                        )
                        .forecastConfidence(
                                request.getForecastConfidence()
                        )
                        .et0Mm(
                                request.getEt0Mm()
                        )
                        .solarIrradianceWm2(
                                request.getSolarIrradianceWm2()
                        )
                        .temperatureMax(
                                request.getTemperatureMax()
                        )
                        .temperatureMin(
                                request.getTemperatureMin()
                        )
                        .windSpeed(
                                request.getWindSpeed()
                        )
                        .source(
                                request.getSource()
                        )
                        .dataStatus(
                                request.getDataStatus()
                        )
                        .notes(
                                request.getNotes()
                        )
                        .build();

        return toResponse(
                repository.save(data)
        );
    }

    public List<WeatherForecastDataResponse> getFarmForecast(
            UUID farmId
    ) {

        verifyFarm(farmId);

        return repository
                .findByFarmIdOrderByForecastDateAsc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WeatherForecastDataResponse> getForecastRange(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        verifyFarm(farmId);

        if (startDate == null || endDate == null) {
            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        return repository
                .findByFarmIdAndForecastDateBetweenOrderByForecastDateAsc(
                        farmId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public WeatherForecastDataResponse getLatestAvailable(
            UUID farmId
    ) {

        verifyFarm(farmId);

        WeatherForecastData data =
                repository
                        .findFirstByFarmIdAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                                farmId,
                                LocalDate.now()
                        );

        return data == null
                ? null
                : toResponse(data);
    }

    private void validate(
            WeatherForecastDataRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }

        if (request.getFarmId() == null) {
            throw new RuntimeException("Farm ID is required");
        }

        if (request.getForecastDate() == null) {
            throw new RuntimeException(
                    "Forecast date is required"
            );
        }

        validatePercentage(
                request.getPrecipitationProbability(),
                "Precipitation probability"
        );

        validatePercentage(
                request.getForecastConfidence(),
                "Forecast confidence"
        );

        validateNonNegative(
                request.getRainfallMm(),
                "Rainfall"
        );

        validateNonNegative(
                request.getEt0Mm(),
                "ET0"
        );

        validateNonNegative(
                request.getSolarIrradianceWm2(),
                "Solar irradiance"
        );

        validateNonNegative(
                request.getWindSpeed(),
                "Wind speed"
        );
    }

    private void validatePercentage(
            Double value,
            String field
    ) {

        if (value != null
                && (value < 0 || value > 1)) {

            throw new RuntimeException(
                    field + " must be between 0 and 1"
            );
        }
    }

    private void validateNonNegative(
            Double value,
            String field
    ) {

        if (value != null && value < 0) {
            throw new RuntimeException(
                    field + " cannot be negative"
            );
        }
    }

    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    private WeatherForecastDataResponse toResponse(
            WeatherForecastData data
    ) {

        return WeatherForecastDataResponse.builder()
                .id(data.getId())
                .farmId(
                        data.getFarm() != null
                                ? data.getFarm().getId()
                                : null
                )
                .forecastDate(
                        data.getForecastDate()
                )
                .rainfallMm(
                        data.getRainfallMm()
                )
                .precipitationProbability(
                        data.getPrecipitationProbability()
                )
                .forecastConfidence(
                        data.getForecastConfidence()
                )
                .et0Mm(
                        data.getEt0Mm()
                )
                .solarIrradianceWm2(
                        data.getSolarIrradianceWm2()
                )
                .temperatureMax(
                        data.getTemperatureMax()
                )
                .temperatureMin(
                        data.getTemperatureMin()
                )
                .windSpeed(
                        data.getWindSpeed()
                )
                .source(
                        data.getSource()
                )
                .dataStatus(
                        data.getDataStatus()
                )
                .notes(
                        data.getNotes()
                )
                .build();
    }
}