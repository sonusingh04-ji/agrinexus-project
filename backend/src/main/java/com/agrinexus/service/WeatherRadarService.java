package com.agrinexus.service;

import com.agrinexus.dto.WeatherRadarResponse;
import com.agrinexus.entity.*;
import com.agrinexus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherRadarService {

    private final FarmRepository farmRepository;

    private final WeatherRecordRepository weatherRecordRepository;

    private final SatelliteFieldDataRepository
            satelliteFieldDataRepository;

    private final CombinedRiskAnalysisRepository
            combinedRiskAnalysisRepository;

    private final WeatherRiskRepository
            weatherRiskRepository;

    private final FarmerAlertRepository
            farmerAlertRepository;


    /*
     * ============================================================
     * WEATHER RISK RADAR
     * ============================================================
     */
    public WeatherRadarResponse getWeatherRadar(
            UUID farmId
    ) {


        /*
         * ========================================================
         * GET FARM
         * ========================================================
         */
        Farm farm =
                farmRepository
                        .findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found"
                                )
                        );


        /*
         * ========================================================
         * GET LATEST WEATHER
         * ========================================================
         */
        WeatherRecord latestWeather =
                weatherRecordRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * ========================================================
         * GET LATEST SATELLITE DATA
         * ========================================================
         */
        SatelliteFieldData latestSatellite =
                satelliteFieldDataRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * ========================================================
         * GET LATEST COMBINED RISK
         * ========================================================
         */
        CombinedRiskAnalysis latestCombinedRisk =
                combinedRiskAnalysisRepository
                        .findByFarmIdOrderByAnalyzedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * ========================================================
         * GET ACTIVE WEATHER RISKS
         * ========================================================
         */
        List<WeatherRisk> activeRisks =
                weatherRiskRepository
                        .findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
                                farmId,
                                LocalDateTime.now()
                        );


        /*
         * ========================================================
         * GET UNREAD FARMER ALERTS
         * ========================================================
         */
        List<FarmerAlert> unreadAlerts =
                farmerAlertRepository
                        .findByFarmIdAndIsReadFalseOrderByCreatedAtDesc(
                                farmId
                        );


        /*
         * ========================================================
         * BUILD WEATHER RADAR RESPONSE
         * ========================================================
         */
        return WeatherRadarResponse.builder()


                /*
                 * ------------------------------------------------
                 * FARM
                 * ------------------------------------------------
                 */
                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .location(
                        farm.getLocation()
                )


                /*
                 * ------------------------------------------------
                 * LATEST WEATHER
                 * ------------------------------------------------
                 */
                .latestWeather(
                        latestWeather != null
                                ? WeatherRadarResponse
                                .WeatherSummary
                                .builder()

                                .temperature(
                                        latestWeather
                                                .getTemperature()
                                )

                                .feelsLike(
                                        latestWeather
                                                .getFeelsLike()
                                )

                                .humidity(
                                        latestWeather
                                                .getHumidity()
                                )

                                .rainfall(
                                        latestWeather
                                                .getRainfall()
                                )

                                .windSpeed(
                                        latestWeather
                                                .getWindSpeed()
                                )

                                .weatherCondition(
                                        latestWeather
                                                .getWeatherCondition()
                                )

                                .weatherDescription(
                                        latestWeather
                                                .getWeatherDescription()
                                )

                                .build()

                                : null
                )


                /*
                 * ------------------------------------------------
                 * LATEST SATELLITE DATA
                 * ------------------------------------------------
                 */
                .latestSatelliteData(
                        latestSatellite != null
                                ? WeatherRadarResponse
                                .SatelliteSummary
                                .builder()

                                .ndvi(
                                        latestSatellite
                                                .getNdvi()
                                )

                                .vegetationHealth(
                                        latestSatellite
                                                .getVegetationHealth()
                                )

                                .soilMoisture(
                                        latestSatellite
                                                .getSoilMoisture()
                                )

                                .cloudCoverage(
                                        latestSatellite
                                                .getCloudCoverage()
                                )

                                /*
                                 * IMPORTANT:
                                 *
                                 * getHealthStatus()
                                 * already returns String.
                                 *
                                 * Therefore DO NOT use:
                                 *
                                 * getHealthStatus().name()
                                 */
                                .healthStatus(
                                        latestSatellite
                                                .getHealthStatus()
                                )

                                .recommendation(
                                        latestSatellite
                                                .getRecommendation()
                                )

                                .build()

                                : null
                )


                /*
                 * ------------------------------------------------
                 * LATEST COMBINED RISK
                 * ------------------------------------------------
                 */
                .combinedRisk(
                        latestCombinedRisk != null
                                ? WeatherRadarResponse
                                .RiskSummary
                                .builder()

                                .combinedRiskScore(
                                        latestCombinedRisk
                                                .getCombinedRiskScore()
                                )

                                /*
                                 * CombinedRiskAnalysis
                                 * riskLevel is an enum.
                                 */
                                .riskLevel(
                                        latestCombinedRisk
                                                .getRiskLevel()
                                                .name()
                                )

                                .title(
                                        latestCombinedRisk
                                                .getTitle()
                                )

                                .recommendation(
                                        latestCombinedRisk
                                                .getRecommendation()
                                )

                                .build()

                                : null
                )


                /*
                 * ------------------------------------------------
                 * ACTIVE WEATHER RISKS
                 * ------------------------------------------------
                 */
                .activeRisks(
                        activeRisks
                                .stream()
                                .map(risk ->
                                        WeatherRadarResponse
                                                .ActiveRiskSummary
                                                .builder()

                                                .id(
                                                        risk.getId()
                                                )

                                                .riskType(
                                                        risk.getRiskType()
                                                                .name()
                                                )

                                                .riskLevel(
                                                        risk.getRiskLevel()
                                                                .name()
                                                )

                                                .riskScore(
                                                        risk.getRiskScore()
                                                )

                                                .title(
                                                        risk.getTitle()
                                                )

                                                .recommendation(
                                                        risk.getRecommendation()
                                                )

                                                .build()
                                )
                                .toList()
                )


                /*
                 * ------------------------------------------------
                 * UNREAD FARMER ALERTS
                 * ------------------------------------------------
                 */
                .unreadAlerts(
                        unreadAlerts
                                .stream()
                                .map(alert ->
                                        WeatherRadarResponse
                                                .AlertSummary
                                                .builder()

                                                .id(
                                                        alert.getId()
                                                )

                                                .riskLevel(
                                                        alert.getRiskLevel()
                                                                .name()
                                                )

                                                .title(
                                                        alert.getTitle()
                                                )

                                                .message(
                                                        alert.getMessage()
                                                )

                                                .isRead(
                                                        alert.getIsRead()
                                                )

                                                .build()
                                )
                                .toList()
                )


                /*
                 * ------------------------------------------------
                 * QUICK SUMMARY
                 * ------------------------------------------------
                 */
                .summary(
                        WeatherRadarResponse
                                .WeatherRadarSummary
                                .builder()

                                .overallRiskLevel(
                                        latestCombinedRisk != null
                                                ? latestCombinedRisk
                                                .getRiskLevel()
                                                .name()
                                                : "UNKNOWN"
                                )

                                .overallRiskScore(
                                        latestCombinedRisk != null
                                                ? latestCombinedRisk
                                                .getCombinedRiskScore()
                                                : 0.0
                                )

                                .activeWeatherRiskCount(
                                        activeRisks.size()
                                )

                                .unreadAlertCount(
                                        unreadAlerts.size()
                                )

                                /*
                                 * IMPORTANT:
                                 *
                                 * Satellite health status is
                                 * already stored as String.
                                 */
                                .fieldHealthStatus(
                                        latestSatellite != null
                                                && latestSatellite
                                                .getHealthStatus() != null
                                                ? latestSatellite
                                                .getHealthStatus()
                                                : "UNKNOWN"
                                )

                                .build()
                )

                .build();
    }
}