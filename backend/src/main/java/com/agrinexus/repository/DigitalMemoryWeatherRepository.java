package com.agrinexus.repository;

import com.agrinexus.entity.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryWeatherRepository
        extends JpaRepository<WeatherRecord, UUID> {

    List<WeatherRecord>
    findByFarmIdOrderByRecordedAtDesc(
            UUID farmId
    );
}