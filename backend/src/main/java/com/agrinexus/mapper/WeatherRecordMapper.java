package com.agrinexus.mapper;

import com.agrinexus.dto.WeatherRecordRequest;
import com.agrinexus.dto.WeatherRecordResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRecord;

public class WeatherRecordMapper {

    private WeatherRecordMapper() {
        // Prevent object creation
    }

    public static WeatherRecord toEntity(
            WeatherRecordRequest request,
            Farm farm
    ) {
        if (request == null) {
            return null;
        }

        return WeatherRecord.builder()
                .farm(farm)
                .recordedAt(request.getRecordedAt())
                .temperature(request.getTemperature())
                .feelsLike(request.getFeelsLike())
                .humidity(request.getHumidity())
                .rainfall(request.getRainfall())
                .windSpeed(request.getWindSpeed())
                .windDirection(request.getWindDirection())
                .pressure(request.getPressure())
                .weatherCondition(request.getWeatherCondition())
                .weatherDescription(request.getWeatherDescription())
                .build();
    }

    public static WeatherRecordResponse toResponse(
            WeatherRecord weatherRecord
    ) {
        if (weatherRecord == null) {
            return null;
        }

        return WeatherRecordResponse.builder()
                .id(weatherRecord.getId())
                .farmId(weatherRecord.getFarm().getId())
                .recordedAt(weatherRecord.getRecordedAt())
                .temperature(weatherRecord.getTemperature())
                .feelsLike(weatherRecord.getFeelsLike())
                .humidity(weatherRecord.getHumidity())
                .rainfall(weatherRecord.getRainfall())
                .windSpeed(weatherRecord.getWindSpeed())
                .windDirection(weatherRecord.getWindDirection())
                .pressure(weatherRecord.getPressure())
                .weatherCondition(weatherRecord.getWeatherCondition())
                .weatherDescription(weatherRecord.getWeatherDescription())
                .build();
    }
}