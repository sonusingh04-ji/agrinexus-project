package com.agrinexus.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRadarResponse {

    private UUID farmId;

    private String farmName;

    private String location;

    /*
     * Latest weather information
     */
    private WeatherSummary latestWeather;

    /*
     * Latest satellite field information
     */
    private SatelliteSummary latestSatelliteData;

    /*
     * Latest combined crop risk analysis
     */
    private RiskSummary combinedRisk;

    /*
     * Current active weather risks
     */
    private List<ActiveRiskSummary> activeRisks;

    /*
     * Current unread farmer alerts
     */
    private List<AlertSummary> unreadAlerts;

    /*
     * Quick summary for dashboard/frontend
     */
    private WeatherRadarSummary summary;


    /*
     * Latest Weather Summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherSummary {

        private Double temperature;

        private Double feelsLike;

        private Integer humidity;

        private Double rainfall;

        private Double windSpeed;

        private String weatherCondition;

        private String weatherDescription;
    }


    /*
     * Latest Satellite Summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SatelliteSummary {

        private Double ndvi;

        private Double vegetationHealth;

        private Double soilMoisture;

        private Double cloudCoverage;

        private String healthStatus;

        private String recommendation;
    }


    /*
     * Combined Risk Summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskSummary {

        private Double combinedRiskScore;

        private String riskLevel;

        private String title;

        private String recommendation;
    }


    /*
     * Active Weather Risk Summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveRiskSummary {

        private UUID id;

        private String riskType;

        private String riskLevel;

        private Double riskScore;

        private String title;

        private String recommendation;
    }


    /*
     * Farmer Alert Summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertSummary {

        private UUID id;

        private String riskLevel;

        private String title;

        private String message;

        private Boolean isRead;
    }


    /*
     * Overall module summary
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherRadarSummary {

        private String overallRiskLevel;

        private Double overallRiskScore;

        private Integer activeWeatherRiskCount;

        private Integer unreadAlertCount;

        private String fieldHealthStatus;
    }
}