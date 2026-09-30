package com.agrinexus.repository;

import com.agrinexus.entity.WeatherForecastData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WeatherForecastDataRepository
        extends JpaRepository<WeatherForecastData, UUID> {

    List<WeatherForecastData>
    findByFarmIdOrderByForecastDateAsc(
            UUID farmId
    );

    List<WeatherForecastData>
    findByFarmIdAndForecastDateBetweenOrderByForecastDateAsc(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Get the nearest upcoming forecast.
     */
    WeatherForecastData
    findFirstByFarmIdAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
            UUID farmId,
            LocalDate forecastDate
    );
}