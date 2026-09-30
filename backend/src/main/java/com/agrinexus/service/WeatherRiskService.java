package com.agrinexus.service;

import com.agrinexus.dto.WeatherRiskRequest;
import com.agrinexus.dto.WeatherRiskResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.mapper.WeatherRiskMapper;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherRiskService {

    private final WeatherRiskRepository weatherRiskRepository;
    private final FarmRepository farmRepository;

    public WeatherRiskResponse createWeatherRisk(
            WeatherRiskRequest request
    ) {

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        WeatherRisk weatherRisk =
                WeatherRiskMapper.toEntity(request, farm);

        WeatherRisk savedRisk =
                weatherRiskRepository.save(weatherRisk);

        return WeatherRiskMapper.toResponse(savedRisk);
    }

    public List<WeatherRiskResponse> getRisksByFarmId(
            UUID farmId
    ) {

        return weatherRiskRepository
                .findByFarmIdOrderByDetectedAtDesc(farmId)
                .stream()
                .map(WeatherRiskMapper::toResponse)
                .toList();
    }

    public List<WeatherRiskResponse> getRisksByFarmIdAndLevel(
            UUID farmId,
            RiskLevel riskLevel
    ) {

        return weatherRiskRepository
                .findByFarmIdAndRiskLevelOrderByDetectedAtDesc(
                        farmId,
                        riskLevel
                )
                .stream()
                .map(WeatherRiskMapper::toResponse)
                .toList();
    }
}