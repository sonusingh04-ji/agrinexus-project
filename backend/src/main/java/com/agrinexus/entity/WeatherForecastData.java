package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "weather_forecast_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecastData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false)
    private LocalDate forecastDate;

    /*
     * Forecast rainfall in mm.
     */
    private Double rainfallMm;

    /*
     * Probability of precipitation, 0–1.
     */
    private Double precipitationProbability;

    /*
     * Forecast confidence, 0–1.
     */
    private Double forecastConfidence;

    /*
     * Forecast ET0 in mm/day when available.
     */
    private Double et0Mm;

    /*
     * Forecast solar irradiance in W/m².
     */
    private Double solarIrradianceWm2;

    /*
     * Forecast maximum temperature.
     */
    private Double temperatureMax;

    /*
     * Forecast minimum temperature.
     */
    private Double temperatureMin;

    /*
     * Forecast wind speed.
     */
    private Double windSpeed;

    /*
     * Source, e.g. WEATHER_API.
     */
    @Column(length = 100)
    private String source;

    /*
     * Data status:
     * FORECAST
     * ESTIMATED
     * TEST
     */
    @Column(length = 30)
    private String dataStatus;

    @Column(length = 2000)
    private String notes;
}