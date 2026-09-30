package com.agrinexus.service;

import com.agrinexus.entity.CombinedRiskAnalysis;
import com.agrinexus.entity.SatelliteFieldData;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.repository.CombinedRiskAnalysisRepository;
import com.agrinexus.repository.SatelliteFieldDataRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import com.agrinexus.repository.WeatherRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CombinedRiskAnalysisService {

    private final CombinedRiskAnalysisRepository
            combinedRiskAnalysisRepository;

    private final WeatherRecordRepository
            weatherRecordRepository;

    private final SatelliteFieldDataRepository
            satelliteFieldDataRepository;

    private final WeatherRiskRepository
            weatherRiskRepository;

    public CombinedRiskAnalysis analyzeFarmRisk(
            UUID farmId
    ) {

        if (farmId == null) {

            throw new IllegalArgumentException(
                    "farmId is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * LATEST WEATHER
         * ---------------------------------------------------------
         */

        WeatherRecord latestWeather =
                getLatestWeatherRecord(
                        farmId
                );

        /*
         * ---------------------------------------------------------
         * LATEST SATELLITE
         * ---------------------------------------------------------
         */

        SatelliteFieldData latestSatellite =
                getLatestSatelliteData(
                        farmId
                );

        /*
         * ---------------------------------------------------------
         * ACTIVE CURRENT WEATHER RISKS
         * ---------------------------------------------------------
         *
         * Only current weather risks are used.
         *
         * Historical expired risks are ignored.
         * Forecast risks are ignored.
         */

        List<WeatherRisk> activeWeatherRisks =
                weatherRiskRepository
                        .findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
                                farmId,
                                LocalDateTime.now()
                        )
                        .stream()
                        .filter(
                                risk ->
                                        !Boolean.TRUE.equals(
                                                risk.getForecastRisk()
                                        )
                        )
                        .toList();

        double weatherRiskScore =
                calculateHighestWeatherRiskScore(
                        activeWeatherRisks
                );

        /*
         * ---------------------------------------------------------
         * SATELLITE RISK
         * ---------------------------------------------------------
         */

        double satelliteRiskScore =
                calculateSatelliteRiskScore(
                        latestSatellite
                );

        /*
         * ---------------------------------------------------------
         * COMBINED SCORE
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * Missing data must NOT be treated as zero risk.
         *
         * If both sources exist:
         *
         *      Weather     = 60%
         *      Satellite   = 40%
         *
         * If only weather exists:
         *
         *      Weather = 100%
         *
         * If only satellite exists:
         *
         *      Satellite = 100%
         *
         * If neither exists:
         *
         *      No valid analysis is possible.
         */

        double combinedRiskScore =
                calculateCombinedRiskScore(
                        latestWeather,
                        latestSatellite,
                        weatherRiskScore,
                        satelliteRiskScore
                );

        RiskLevel riskLevel =
                determineRiskLevel(
                        combinedRiskScore
                );

        String title =
                generateTitle(
                        riskLevel
                );

        String analysis =
                generateAnalysis(
                        latestWeather,
                        latestSatellite,
                        activeWeatherRisks,
                        weatherRiskScore,
                        satelliteRiskScore,
                        combinedRiskScore
                );

        String recommendation =
                generateRecommendation(
                        riskLevel,
                        latestSatellite,
                        latestWeather,
                        activeWeatherRisks
                );

        /*
         * ---------------------------------------------------------
         * VALID DATA CHECK
         * ---------------------------------------------------------
         */

        if (latestWeather == null
                && latestSatellite == null) {

            throw new IllegalStateException(
                    "No weather or satellite data is available "
                            + "for farm " + farmId
            );
        }

        /*
         * ---------------------------------------------------------
         * BUILD COMBINED ANALYSIS
         * ---------------------------------------------------------
         */

        CombinedRiskAnalysis combinedRiskAnalysis =
                CombinedRiskAnalysis.builder()

                        .farm(
                                latestWeather != null
                                        ? latestWeather.getFarm()
                                        : latestSatellite.getFarm()
                        )

                        .combinedRiskScore(
                                combinedRiskScore
                        )

                        .riskLevel(
                                riskLevel
                        )

                        .title(
                                title
                        )

                        .analysis(
                                analysis
                        )

                        .recommendation(
                                recommendation
                        )

                        .ndvi(
                                latestSatellite != null
                                        ? latestSatellite.getNdvi()
                                        : null
                        )

                        .soilMoisture(
                                latestSatellite != null
                                        ? latestSatellite.getSoilMoisture()
                                        : null
                        )

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

                        .build();

        return combinedRiskAnalysisRepository
                .save(
                        combinedRiskAnalysis
                );
    }

    /*
     * =========================================================
     * LATEST WEATHER
     * =========================================================
     */

    private WeatherRecord getLatestWeatherRecord(
            UUID farmId
    ) {

        return weatherRecordRepository
                .findByFarmIdOrderByRecordedAtDesc(
                        farmId
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    /*
     * =========================================================
     * LATEST SATELLITE
     * =========================================================
     */

    private SatelliteFieldData getLatestSatelliteData(
            UUID farmId
    ) {

        return satelliteFieldDataRepository
                .findByFarmIdOrderByRecordedAtDesc(
                        farmId
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    /*
     * =========================================================
     * WEATHER RISK SCORE
     * =========================================================
     *
     * Highest active weather risk is used.
     *
     * Example:
     *
     * Pest       85
     * Disease    85
     * Heat       60
     *
     * Weather score = 85
     */

    private double calculateHighestWeatherRiskScore(
            List<WeatherRisk> weatherRisks
    ) {

        if (weatherRisks == null
                || weatherRisks.isEmpty()) {

            return 0.0;
        }

        return weatherRisks.stream()

                .map(
                        WeatherRisk::getRiskScore
                )

                .filter(
                        score ->
                                score != null
                                        && score >= 0
                                        && score <= 100
                )

                .max(
                        Comparator.naturalOrder()
                )

                .orElse(0.0);
    }

    /*
     * =========================================================
     * SATELLITE RISK SCORE
     * =========================================================
     */

    private double calculateSatelliteRiskScore(
            SatelliteFieldData satelliteData
    ) {

        if (satelliteData == null) {
            return 0.0;
        }

        double riskScore =
                0.0;

        /*
         * ---------------------------------------------------------
         * NDVI
         * ---------------------------------------------------------
         */

        if (satelliteData.getNdvi() != null) {

            double ndvi =
                    satelliteData.getNdvi();

            if (ndvi < 0.2) {

                riskScore += 40;

            } else if (ndvi < 0.4) {

                riskScore += 30;

            } else if (ndvi < 0.6) {

                riskScore += 15;
            }
        }

        /*
         * ---------------------------------------------------------
         * SOIL MOISTURE
         * ---------------------------------------------------------
         */

        if (satelliteData.getSoilMoisture() != null) {

            double soilMoisture =
                    satelliteData.getSoilMoisture();

            if (soilMoisture < 15) {

                riskScore += 40;

            } else if (soilMoisture < 30) {

                riskScore += 25;

            } else if (soilMoisture < 50) {

                riskScore += 10;
            }
        }

        return Math.min(
                riskScore,
                100.0
        );
    }

    /*
     * =========================================================
     * COMBINED RISK SCORE
     * =========================================================
     *
     * This method is intentionally data-aware.
     *
     * Missing source != zero-risk source.
     */

    private double calculateCombinedRiskScore(
            WeatherRecord weather,
            SatelliteFieldData satellite,
            double weatherRiskScore,
            double satelliteRiskScore
    ) {

        boolean hasWeather =
                weather != null;

        boolean hasSatellite =
                satellite != null;

        /*
         * No source.
         *
         * The caller already prevents this situation,
         * but returning 0 keeps this method safe.
         */

        if (!hasWeather && !hasSatellite) {

            return 0.0;
        }

        /*
         * ---------------------------------------------------------
         * WEATHER ONLY
         * ---------------------------------------------------------
         */

        if (hasWeather && !hasSatellite) {

            return roundScore(
                    weatherRiskScore
            );
        }

        /*
         * ---------------------------------------------------------
         * SATELLITE ONLY
         * ---------------------------------------------------------
         */

        if (!hasWeather && hasSatellite) {

            return roundScore(
                    satelliteRiskScore
            );
        }

        /*
         * ---------------------------------------------------------
         * BOTH SOURCES AVAILABLE
         * ---------------------------------------------------------
         *
         * Weather = 60%
         * Satellite = 40%
         */

        double combinedScore =
                (weatherRiskScore * 0.60)
                        +
                        (satelliteRiskScore * 0.40);

        return roundScore(
                combinedScore
        );
    }

    /*
     * =========================================================
     * ROUND SCORE
     * =========================================================
     */

    private double roundScore(
            double score
    ) {

        return Math.min(
                Math.max(
                        Math.round(
                                score * 100.0
                        ) / 100.0,
                        0.0
                ),
                100.0
        );
    }

    /*
     * =========================================================
     * RISK LEVEL
     * =========================================================
     */

    private RiskLevel determineRiskLevel(
            double combinedRiskScore
    ) {

        if (combinedRiskScore >= 85) {

            return RiskLevel.CRITICAL;
        }

        if (combinedRiskScore >= 65) {

            return RiskLevel.HIGH;
        }

        if (combinedRiskScore >= 40) {

            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }

    /*
     * =========================================================
     * TITLE
     * =========================================================
     */

    private String generateTitle(
            RiskLevel riskLevel
    ) {

        return switch (riskLevel) {

            case CRITICAL ->
                    "Critical Combined Crop Stress Alert";

            case HIGH ->
                    "High Combined Farm Risk Alert";

            case MEDIUM ->
                    "Moderate Combined Farm Risk";

            case LOW ->
                    "Low Combined Farm Risk";
        };
    }

    /*
     * =========================================================
     * ANALYSIS
     * =========================================================
     */

    private String generateAnalysis(
            WeatherRecord weather,
            SatelliteFieldData satellite,
            List<WeatherRisk> activeWeatherRisks,
            double weatherRiskScore,
            double satelliteRiskScore,
            double combinedRiskScore
    ) {

        StringBuilder analysis =
                new StringBuilder();

        analysis.append(
                "Combined farm risk analysis completed. "
        );

        /*
         * ---------------------------------------------------------
         * DATA SOURCES
         * ---------------------------------------------------------
         */

        if (weather != null
                && satellite != null) {

            analysis.append(
                    "Weather data and satellite data were available. "
            );

        } else if (weather != null) {

            analysis.append(
                    "Only weather data was available; "
                            + "satellite data was unavailable, "
                            + "so the weather score was used without "
                            + "penalizing the result for missing data. "
            );

        } else {

            analysis.append(
                    "Only satellite data was available; "
                            + "weather data was unavailable, "
                            + "so the satellite score was used. "
            );
        }

        /*
         * ---------------------------------------------------------
         * WEATHER SCORE
         * ---------------------------------------------------------
         */

        if (weather != null) {

            analysis.append(
                    "Weather risk score: "
                            + weatherRiskScore
                            + ". "
            );
        }

        /*
         * ---------------------------------------------------------
         * SATELLITE SCORE
         * ---------------------------------------------------------
         */

        if (satellite != null) {

            analysis.append(
                    "Satellite risk score: "
                            + satelliteRiskScore
                            + ". "
            );
        }

        /*
         * ---------------------------------------------------------
         * ACTIVE RISK SUMMARY
         * ---------------------------------------------------------
         */

        if (activeWeatherRisks != null
                && !activeWeatherRisks.isEmpty()) {

            WeatherRisk highestRisk =
                    activeWeatherRisks.stream()

                            .filter(
                                    risk ->
                                            risk.getRiskScore()
                                                    != null
                            )

                            .max(
                                    Comparator.comparing(
                                            WeatherRisk::getRiskScore
                                    )
                            )

                            .orElse(null);

            if (highestRisk != null) {

                analysis.append(
                        "Highest active weather risk: "
                                + highestRisk.getTitle()
                                + " with a score of "
                                + highestRisk.getRiskScore()
                                + ". "
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * FINAL SCORE
         * ---------------------------------------------------------
         */

        analysis.append(
                "Final combined risk score: "
                        + combinedRiskScore
                        + ". "
        );

        /*
         * ---------------------------------------------------------
         * CONDITIONS
         * ---------------------------------------------------------
         */

        analysis.append(
                generateConditionSummary(
                        weather,
                        satellite
                )
        );

        return analysis.toString();
    }

    /*
     * =========================================================
     * CONDITION SUMMARY
     * =========================================================
     */

    private String generateConditionSummary(
            WeatherRecord weather,
            SatelliteFieldData satellite
    ) {

        StringBuilder summary =
                new StringBuilder();

        if (weather != null) {

            summary.append(
                    "Latest weather: "
            );

            boolean hasWeatherValue =
                    false;

            if (weather.getTemperature() != null) {

                summary.append(
                        "temperature "
                                + weather.getTemperature()
                                + "°C"
                );

                hasWeatherValue = true;
            }

            if (weather.getRainfall() != null) {

                if (hasWeatherValue) {
                    summary.append(", ");
                }

                summary.append(
                        "rainfall "
                                + weather.getRainfall()
                                + " mm"
                );

                hasWeatherValue = true;
            }

            if (weather.getWindSpeed() != null) {

                if (hasWeatherValue) {
                    summary.append(", ");
                }

                summary.append(
                        "wind speed "
                                + weather.getWindSpeed()
                                + " km/h"
                );

                hasWeatherValue = true;
            }

            if (weather.getHumidity() != null) {

                if (hasWeatherValue) {
                    summary.append(", ");
                }

                summary.append(
                        "humidity "
                                + weather.getHumidity()
                                + "%"
                );
            }

            summary.append(
                    ". "
            );
        }

        if (satellite != null) {

            summary.append(
                    "Latest satellite condition: "
            );

            boolean hasSatelliteValue =
                    false;

            if (satellite.getNdvi() != null) {

                summary.append(
                        "NDVI "
                                + satellite.getNdvi()
                );

                hasSatelliteValue = true;
            }

            if (satellite.getSoilMoisture() != null) {

                if (hasSatelliteValue) {
                    summary.append(", ");
                }

                summary.append(
                        "soil moisture "
                                + satellite.getSoilMoisture()
                                + "%"
                );
            }

            summary.append(
                    "."
            );
        }

        return summary.toString();
    }

    /*
     * =========================================================
     * RECOMMENDATION
     * =========================================================
     */

    private String generateRecommendation(
            RiskLevel riskLevel,
            SatelliteFieldData satellite,
            WeatherRecord weather,
            List<WeatherRisk> activeWeatherRisks
    ) {

        /*
         * Prefer the recommendation from the highest
         * active weather risk when one exists.
         *
         * This makes the radar action-oriented.
         */

        if (activeWeatherRisks != null
                && !activeWeatherRisks.isEmpty()) {

            WeatherRisk highestRisk =
                    activeWeatherRisks.stream()

                            .filter(
                                    risk ->
                                            risk.getRiskScore()
                                                    != null
                            )

                            .max(
                                    Comparator.comparing(
                                            WeatherRisk::getRiskScore
                                    )
                            )

                            .orElse(null);

            if (highestRisk != null
                    && highestRisk.getRecommendation()
                    != null
                    && !highestRisk
                    .getRecommendation()
                    .isBlank()) {

                return highestRisk
                        .getRecommendation();
            }
        }

        return switch (riskLevel) {

            case CRITICAL ->
                    "Take immediate crop-protection measures and closely monitor field conditions.";

            case HIGH ->
                    "Monitor the field closely and follow the recommended crop and irrigation actions.";

            case MEDIUM ->
                    "Continue monitoring weather and field conditions and take preventive action where appropriate.";

            case LOW ->
                    "Current combined risk is low. Continue normal field monitoring.";
        };
    }
}