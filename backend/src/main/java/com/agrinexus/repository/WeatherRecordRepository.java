package com.agrinexus.repository;

import com.agrinexus.entity.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeatherRecordRepository
        extends JpaRepository<WeatherRecord, UUID> {

    List<WeatherRecord> findByFarmIdOrderByRecordedAtDesc(UUID farmId);
}