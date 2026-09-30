package com.agrinexus.service;

import com.agrinexus.dto.DashboardResponse;
import com.agrinexus.entity.*;
import com.agrinexus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FarmRepository farmRepository;

    private final WeatherRecordRepository weatherRecordRepository;

    private final SatelliteFieldDataRepository
            satelliteFieldDataRepository;

    private final WeatherRiskRepository weatherRiskRepository;

    private final CombinedRiskAnalysisRepository
            combinedRiskAnalysisRepository;


    public DashboardResponse getFarmDashboard(
            UUID farmId
    ) {

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
         * LATEST WEATHER
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
         * LATEST SATELLITE DATA
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
         * LATEST COMBINED RISK
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
         * ACTIVE WEATHER RISKS
         * ========================================================
         */
        List<WeatherRisk> activeRisks =
                weatherRiskRepository
                        .findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
                                farmId,
                                LocalDateTime.now()
                        );


        List<String> activeRiskTitles =
                activeRisks
                        .stream()
                        .map(WeatherRisk::getTitle)
                        .toList();


        /*
         * ========================================================
         * BUILD DASHBOARD RESPONSE
         * ========================================================
         */
        return DashboardResponse.builder()


                /*
                 * ------------------------------------------------
                 * FARM INFORMATION
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

                .latitude(
                        farm.getLatitude()
                )

                .longitude(
                        farm.getLongitude()
                )


                /*
                 * ------------------------------------------------
                 * WEATHER INFORMATION
                 * ------------------------------------------------
                 */
                .temperature(
                        latestWeather != null
                                ? latestWeather.getTemperature()
                                : null
                )

                .rainfall(
                        latestWeather != null
                                ? latestWeather.getRainfall()
                                : null
                )

                .windSpeed(
                        latestWeather != null
                                ? latestWeather.getWindSpeed()
                                : null
                )

                .weatherCondition(
                        latestWeather != null
                                ? latestWeather.getWeatherCondition()
                                : null
                )


                /*
                 * ------------------------------------------------
                 * SATELLITE INFORMATION
                 * ------------------------------------------------
                 */
                .ndvi(
                        latestSatellite != null
                                ? latestSatellite.getNdvi()
                                : null
                )

                .vegetationHealth(
                        latestSatellite != null
                                ? latestSatellite
                                .getVegetationHealth()
                                : null
                )

                .soilMoisture(
                        latestSatellite != null
                                ? latestSatellite.getSoilMoisture()
                                : null
                )

                /*
                 * IMPORTANT:
                 *
                 * healthStatus is already a String in the
                 * SatelliteFieldData entity.
                 *
                 * DO NOT call .name().
                 */
                .fieldHealthStatus(
                        latestSatellite != null
                                ? latestSatellite
                                .getHealthStatus()
                                : null
                )


                /*
                 * ------------------------------------------------
                 * COMBINED RISK
                 * ------------------------------------------------
                 */
                .combinedRiskScore(
                        latestCombinedRisk != null
                                ? latestCombinedRisk
                                .getCombinedRiskScore()
                                : null
                )

                .riskLevel(
                        latestCombinedRisk != null
                                ? latestCombinedRisk
                                .getRiskLevel()
                                : null
                )

                .riskTitle(
                        latestCombinedRisk != null
                                ? latestCombinedRisk
                                .getTitle()
                                : null
                )

                .recommendation(
                        latestCombinedRisk != null
                                ? latestCombinedRisk
                                .getRecommendation()
                                : null
                )


                /*
                 * ------------------------------------------------
                 * ACTIVE ALERTS
                 * ------------------------------------------------
                 */
                .activeRiskCount(
                        activeRisks.size()
                )

                .activeRiskTitles(
                        activeRiskTitles
                )


                /*
                 * ------------------------------------------------
                 * LAST UPDATED
                 * ------------------------------------------------
                 */
                .lastUpdated(
                        LocalDateTime.now()
                )

                .build();
    }
}