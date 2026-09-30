package com.agrinexus.repository;

import com.agrinexus.entity.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WeatherForecastRepository
        extends JpaRepository<WeatherForecast, UUID> {

    List<WeatherForecast> findByFarmIdOrderByForecastForAsc(
            UUID farmId
    );
}