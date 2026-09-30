package com.agrinexus.service;

import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.enums.RiskType;
import com.agrinexus.repository.WeatherRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherRiskAnalysisService {

    private final WeatherRiskRepository weatherRiskRepository;

    @Transactional
    public List<WeatherRisk> analyzeWeather(
            WeatherRecord weatherRecord
    ) {

        if (weatherRecord == null
                || weatherRecord.getFarm() == null
                || weatherRecord.getFarm().getId() == null) {

            throw new IllegalArgumentException(
                    "Weather record and farm are required for risk analysis."
            );
        }

        /*
         * ---------------------------------------------------------
         * REMOVE OLD ACTIVE CURRENT-WEATHER RISKS
         * ---------------------------------------------------------
         *
         * This prevents duplicate active risks when live weather
         * is refreshed repeatedly.
         *
         * Forecast risks are not touched here.
         */
        List<WeatherRisk> existingActiveRisks =
                weatherRiskRepository
                        .findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
                                weatherRecord.getFarm().getId(),
                                LocalDateTime.now()
                        )
                        .stream()
                        .filter(risk ->
                                !Boolean.TRUE.equals(
                                        risk.getForecastRisk()
                                )
                        )
                        .toList();

        if (!existingActiveRisks.isEmpty()) {

            weatherRiskRepository.deleteAll(
                    existingActiveRisks
            );

            weatherRiskRepository.flush();
        }

        /*
         * ---------------------------------------------------------
         * DETECT CURRENT RISKS
         * ---------------------------------------------------------
         */

        List<WeatherRisk> detectedRisks =
                new ArrayList<>();

        analyzeHeatStress(
                weatherRecord,
                detectedRisks
        );

        analyzeDrought(
                weatherRecord,
                detectedRisks
        );

        analyzeWaterlogging(
                weatherRecord,
                detectedRisks
        );

        analyzeHighWind(
                weatherRecord,
                detectedRisks
        );

        analyzeFrost(
                weatherRecord,
                detectedRisks
        );

        analyzeDiseaseRisk(
                weatherRecord,
                detectedRisks
        );

        analyzePestRisk(
                weatherRecord,
                detectedRisks
        );

        /*
         * ---------------------------------------------------------
         * SAVE NEW CURRENT RISKS
         * ---------------------------------------------------------
         */

        if (detectedRisks.isEmpty()) {

            return List.of();
        }

        return weatherRiskRepository.saveAll(
                detectedRisks
        );
    }

    private void analyzeHeatStress(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getTemperature() == null) {
            return;
        }

        double temperature =
                weatherRecord.getTemperature();

        if (temperature >= 45) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.HEAT_STRESS,
                            RiskLevel.CRITICAL,
                            "Critical Heat Stress Warning",
                            "Extremely high temperature may cause severe crop stress.",
                            "Provide crop-specific protective measures and closely monitor soil moisture.",
                            95.0
                    )
            );

        } else if (temperature >= 40) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.HEAT_STRESS,
                            RiskLevel.HIGH,
                            "High Heat Stress Warning",
                            "High temperature may increase heat stress on crops.",
                            "Monitor soil moisture and follow the recommended irrigation schedule.",
                            82.0
                    )
            );

        } else if (temperature >= 35) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.HEAT_STRESS,
                            RiskLevel.MEDIUM,
                            "Moderate Heat Stress Risk",
                            "Elevated temperature may create moderate heat stress.",
                            "Monitor field conditions and irrigation requirements.",
                            60.0
                    )
            );
        }
    }

    private void analyzeDrought(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getTemperature() == null
                || weatherRecord.getRainfall() == null) {

            return;
        }

        if (weatherRecord.getTemperature() >= 35
                && weatherRecord.getRainfall() <= 0.0) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.DROUGHT,
                            RiskLevel.HIGH,
                            "Drought Risk Warning",
                            "High temperature combined with no rainfall may increase drought conditions.",
                            "Check soil moisture and use the recommended irrigation plan.",
                            80.0
                    )
            );
        }
    }

    private void analyzeWaterlogging(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getRainfall() == null) {
            return;
        }

        double rainfall =
                weatherRecord.getRainfall();

        if (rainfall >= 100) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.WATERLOGGING,
                            RiskLevel.CRITICAL,
                            "Critical Waterlogging Warning",
                            "Very heavy rainfall may cause severe water accumulation.",
                            "Check drainage immediately and prevent prolonged standing water.",
                            95.0
                    )
            );

        } else if (rainfall >= 50) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.WATERLOGGING,
                            RiskLevel.HIGH,
                            "High Waterlogging Risk",
                            "Heavy rainfall may lead to water accumulation in the field.",
                            "Inspect field drainage and monitor low-lying areas.",
                            80.0
                    )
            );
        }
    }

    private void analyzeHighWind(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getWindSpeed() == null) {
            return;
        }

        double windSpeed =
                weatherRecord.getWindSpeed();

        if (windSpeed >= 50) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.HIGH_WIND,
                            RiskLevel.CRITICAL,
                            "Critical High Wind Warning",
                            "Very strong wind may damage crops and farm infrastructure.",
                            "Avoid field spraying and secure vulnerable farm structures.",
                            95.0
                    )
            );

        } else if (windSpeed >= 30) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.HIGH_WIND,
                            RiskLevel.HIGH,
                            "High Wind Warning",
                            "Strong wind may damage sensitive crops.",
                            "Monitor crops and avoid unnecessary spraying operations.",
                            80.0
                    )
            );
        }
    }

    private void analyzeFrost(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getTemperature() == null) {
            return;
        }

        double temperature =
                weatherRecord.getTemperature();

        if (temperature <= 0) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.FROST,
                            RiskLevel.CRITICAL,
                            "Critical Frost Warning",
                            "Freezing temperature may severely affect frost-sensitive crops.",
                            "Follow crop-specific frost protection measures.",
                            95.0
                    )
            );

        } else if (temperature <= 5) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.FROST,
                            RiskLevel.HIGH,
                            "Frost Risk Warning",
                            "Very low temperature may create frost risk.",
                            "Monitor crop conditions and prepare appropriate frost protection.",
                            80.0
                    )
            );
        }
    }

    private void analyzeDiseaseRisk(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getTemperature() == null
                || weatherRecord.getHumidity() == null) {

            return;
        }

        double temperature =
                weatherRecord.getTemperature();

        double humidity =
                weatherRecord.getHumidity();

        if (temperature >= 20
                && temperature <= 30
                && humidity >= 80) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.DISEASE_RISK,
                            RiskLevel.HIGH,
                            "High Crop Disease Risk",
                            "Warm and highly humid weather may increase the risk of crop diseases.",
                            "Inspect crops regularly and take preventive measures according to the crop type.",
                            85.0
                    )
            );

        } else if (temperature >= 15
                && temperature <= 35
                && humidity >= 65) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.DISEASE_RISK,
                            RiskLevel.MEDIUM,
                            "Moderate Crop Disease Risk",
                            "Current weather conditions may support the development of crop diseases.",
                            "Monitor crop leaves and field conditions for early signs of disease.",
                            65.0
                    )
            );
        }
    }

    private void analyzePestRisk(
            WeatherRecord weatherRecord,
            List<WeatherRisk> risks
    ) {

        if (weatherRecord.getTemperature() == null
                || weatherRecord.getHumidity() == null) {

            return;
        }

        double temperature =
                weatherRecord.getTemperature();

        double humidity =
                weatherRecord.getHumidity();

        if (temperature >= 25
                && temperature <= 35
                && humidity >= 70) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.PEST_RISK,
                            RiskLevel.HIGH,
                            "High Pest Activity Risk",
                            "Warm and humid weather may increase pest activity in the field.",
                            "Inspect crops regularly and take crop-specific preventive measures if pest activity is observed.",
                            85.0
                    )
            );

        } else if (temperature >= 20
                && temperature <= 38
                && humidity >= 55) {

            risks.add(
                    createRisk(
                            weatherRecord,
                            RiskType.PEST_RISK,
                            RiskLevel.MEDIUM,
                            "Moderate Pest Activity Risk",
                            "Current weather conditions may support increased pest activity.",
                            "Monitor crops and check for early signs of pest activity.",
                            65.0
                    )
            );
        }
    }

    private WeatherRisk createRisk(
            WeatherRecord weatherRecord,
            RiskType riskType,
            RiskLevel riskLevel,
            String title,
            String description,
            String recommendation,
            double riskScore
    ) {

        return WeatherRisk.builder()
                .farm(
                        weatherRecord.getFarm()
                )
                .riskType(
                        riskType
                )
                .riskLevel(
                        riskLevel
                )
                .title(
                        title
                )
                .description(
                        description
                )
                .recommendation(
                        recommendation
                )
                .riskScore(
                        riskScore
                )
                .detectedAt(
                        LocalDateTime.now()
                )
                .validUntil(
                        LocalDateTime.now().plusHours(24)
                )
                .forecastRisk(
                        false
                )
                .forecastFor(
                        null
                )
                .build();
    }
}