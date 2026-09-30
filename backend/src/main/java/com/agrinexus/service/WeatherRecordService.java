package com.agrinexus.service;

import com.agrinexus.dto.WeatherRecordRequest;
import com.agrinexus.dto.WeatherRecordResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.mapper.WeatherRecordMapper;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherRecordService {

    private final WeatherRecordRepository weatherRecordRepository;
    private final FarmRepository farmRepository;

    private final WeatherRiskAnalysisService weatherRiskAnalysisService;

    public WeatherRecordResponse createWeatherRecord(
            WeatherRecordRequest request
    ) {

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        WeatherRecord weatherRecord =
                WeatherRecordMapper.toEntity(request, farm);

        WeatherRecord savedRecord =
                weatherRecordRepository.save(weatherRecord);

        weatherRiskAnalysisService.analyzeWeather(savedRecord);

        return WeatherRecordMapper.toResponse(savedRecord);
    }

    public List<WeatherRecordResponse> getWeatherRecordsByFarmId(
            UUID farmId
    ) {

        return weatherRecordRepository
                .findByFarmIdOrderByRecordedAtDesc(farmId)
                .stream()
                .map(WeatherRecordMapper::toResponse)
                .toList();
    }
}