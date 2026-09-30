package com.agrinexus.service;

import com.agrinexus.entity.WeatherForecast;
import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.enums.RiskType;
import com.agrinexus.repository.WeatherRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FutureWeatherRiskPredictionService {

    private final WeatherRiskRepository weatherRiskRepository;

    public List<WeatherRisk> predictFutureRisks(
            WeatherForecast weatherForecast
    ) {

        List<WeatherRisk> predictedRisks = new ArrayList<>();

        predictHeatStress(weatherForecast, predictedRisks);
        predictDrought(weatherForecast, predictedRisks);
        predictWaterlogging(weatherForecast, predictedRisks);
        predictHighWind(weatherForecast, predictedRisks);
        predictFrost(weatherForecast, predictedRisks);
        predictDiseaseRisk(weatherForecast, predictedRisks);
        predictPestRisk(weatherForecast, predictedRisks);

        return weatherRiskRepository.saveAll(predictedRisks);
    }

    private void predictHeatStress(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getTemperature() == null) {
            return;
        }

        double temperature = forecast.getTemperature();

        if (temperature >= 45) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.HEAT_STRESS,
                    RiskLevel.CRITICAL,
                    "Future Critical Heat Stress Warning",
                    "Extremely high temperature is forecast and may cause severe crop stress.",
                    "Prepare irrigation and crop protection measures before the forecasted heat period.",
                    95.0
            ));

        } else if (temperature >= 40) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.HEAT_STRESS,
                    RiskLevel.HIGH,
                    "Future High Heat Stress Warning",
                    "High temperature is forecast and may create serious heat stress for crops.",
                    "Plan irrigation and monitor crop conditions before the heat period.",
                    82.0
            ));

        } else if (temperature >= 35) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.HEAT_STRESS,
                    RiskLevel.MEDIUM,
                    "Future Moderate Heat Stress Risk",
                    "Elevated temperature is forecast and may create moderate crop stress.",
                    "Monitor soil moisture and prepare irrigation if needed.",
                    60.0
            ));
        }
    }

    private void predictDrought(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getTemperature() == null
                || forecast.getRainfall() == null) {
            return;
        }

        if (forecast.getTemperature() >= 35
                && forecast.getRainfall() == 0) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.DROUGHT,
                    RiskLevel.HIGH,
                    "Future Drought Risk Warning",
                    "High temperature and no rainfall are forecast, which may increase drought conditions.",
                    "Prepare the recommended irrigation plan and monitor soil moisture.",
                    80.0
            ));
        }
    }

    private void predictWaterlogging(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getRainfall() == null) {
            return;
        }

        double rainfall = forecast.getRainfall();

        if (rainfall >= 100) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.WATERLOGGING,
                    RiskLevel.CRITICAL,
                    "Future Critical Waterlogging Warning",
                    "Very heavy rainfall is forecast and may cause severe water accumulation.",
                    "Inspect drainage systems before the forecasted rainfall.",
                    95.0
            ));

        } else if (rainfall >= 50) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.WATERLOGGING,
                    RiskLevel.HIGH,
                    "Future High Waterlogging Risk",
                    "Heavy rainfall is forecast and may lead to water accumulation.",
                    "Prepare field drainage and monitor low-lying areas.",
                    80.0
            ));
        }
    }

    private void predictHighWind(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getWindSpeed() == null) {
            return;
        }

        double windSpeed = forecast.getWindSpeed();

        if (windSpeed >= 50) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.HIGH_WIND,
                    RiskLevel.CRITICAL,
                    "Future Critical High Wind Warning",
                    "Very strong wind is forecast and may damage crops or farm infrastructure.",
                    "Secure vulnerable structures and avoid spraying before strong winds.",
                    95.0
            ));

        } else if (windSpeed >= 30) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.HIGH_WIND,
                    RiskLevel.HIGH,
                    "Future High Wind Warning",
                    "Strong wind is forecast and may damage sensitive crops.",
                    "Avoid spraying and monitor vulnerable crops.",
                    80.0
            ));
        }
    }

    private void predictFrost(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getTemperature() == null) {
            return;
        }

        double temperature = forecast.getTemperature();

        if (temperature <= 0) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.FROST,
                    RiskLevel.CRITICAL,
                    "Future Critical Frost Warning",
                    "Freezing temperature is forecast and may severely affect frost-sensitive crops.",
                    "Prepare crop-specific frost protection before the forecasted freezing period.",
                    95.0
            ));

        } else if (temperature <= 5) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.FROST,
                    RiskLevel.HIGH,
                    "Future Frost Risk Warning",
                    "Very low temperature is forecast and may create frost risk.",
                    "Prepare appropriate crop protection before the low-temperature period.",
                    80.0
            ));
        }
    }

    private void predictDiseaseRisk(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getHumidity() == null
                || forecast.getTemperature() == null) {
            return;
        }

        double humidity = forecast.getHumidity();
        double temperature = forecast.getTemperature();

        if (humidity >= 80
                && temperature >= 20
                && temperature <= 32) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.DISEASE_RISK,
                    RiskLevel.HIGH,
                    "Future Crop Disease Risk",
                    "Warm and humid weather is forecast and may increase the chance of crop disease.",
                    "Inspect crops early and follow preventive disease management practices.",
                    78.0
            ));
        }
    }

    private void predictPestRisk(
            WeatherForecast forecast,
            List<WeatherRisk> risks
    ) {

        if (forecast.getHumidity() == null
                || forecast.getTemperature() == null) {
            return;
        }

        double humidity = forecast.getHumidity();
        double temperature = forecast.getTemperature();

        if (temperature >= 24
                && temperature <= 35
                && humidity >= 50
                && humidity <= 80) {

            risks.add(createForecastRisk(
                    forecast,
                    RiskType.PEST_RISK,
                    RiskLevel.MEDIUM,
                    "Future Pest Risk",
                    "Forecast weather conditions may support increased pest activity.",
                    "Monitor crops regularly and take preventive action if pest activity increases.",
                    65.0
            ));
        }
    }

    private WeatherRisk createForecastRisk(
            WeatherForecast forecast,
            RiskType riskType,
            RiskLevel riskLevel,
            String title,
            String description,
            String recommendation,
            double riskScore
    ) {

        return WeatherRisk.builder()
                .farm(forecast.getFarm())
                .riskType(riskType)
                .riskLevel(riskLevel)
                .title(title)
                .description(description)
                .recommendation(recommendation)
                .riskScore(riskScore)
                .forecastRisk(true)
                .forecastFor(forecast.getForecastFor())
                .validUntil(
                        forecast.getForecastFor()
                                .plusHours(24)
                )
                .detectedAt(LocalDateTime.now())
                .build();
    }
}