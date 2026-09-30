package com.agrinexus.repository;

import com.agrinexus.entity.WeatherRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryWeatherRiskRepository
        extends JpaRepository<WeatherRisk, UUID> {

    List<WeatherRisk>
    findByFarmIdOrderByDetectedAtDesc(
            UUID farmId
    );
}