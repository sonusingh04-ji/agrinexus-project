package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_forecasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    private LocalDateTime forecastFor;

    private Double temperature;

    private Double feelsLike;

    private Double humidity;

    private Double rainfall;

    private Double windSpeed;

    private String weatherCondition;

    private String weatherDescription;

    private LocalDateTime createdAt;
}