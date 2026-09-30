package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldHealthPassportResponse {

    /*
     * =========================================================
     * Farm identity
     * =========================================================
     */

    private UUID farmId;

    private String farmName;

    private String location;

    private Double latitude;

    private Double longitude;

    private LocalDateTime generatedAt;


    /*
     * =========================================================
     * Overall field health
     * =========================================================
     */

    /*
     * HEALTHY
     * STABLE
     * ATTENTION_REQUIRED
     * CRITICAL
     * INSUFFICIENT_DATA
     */
    private String overallHealthStatus;

    /*
     * 0 - 100
     *
     * This is a transparent summary indicator,
     * not a crop-yield prediction.
     */
    private Double healthScore;

    private String healthSummary;


    /*
     * =========================================================
     * Current crop
     * =========================================================
     */

    private UUID currentCropId;

    private String currentCropName;

    private String currentSeason;

    private String currentCropStage;


    /*
     * =========================================================
     * Latest soil condition
     * =========================================================
     */

    private UUID latestSoilSampleId;

    private LocalDate latestSoilSampleDate;

    private String soilType;

    private Double soilPh;

    private Double electricalConductivity;

    private Double organicCarbon;

    private Double soilNitrogen;

    private Double soilPhosphorus;

    private Double soilPotassium;

    private Double soilZinc;

    private Double soilSulphur;

    private Double soilIron;

    private Double soilManganese;

    private Double soilCopper;

    private String soilSource;


    /*
     * =========================================================
     * Soil trend / recovery
     * =========================================================
     */

    private String soilTrend;

    private String recoveryFocusArea;

    private Double recoveryBaseline;

    private Double recoveryCurrent;

    private Double recoveryTarget;

    private String recoveryUnit;

    private String recoveryAction;

    private String recoveryRecommendation;


    /*
     * =========================================================
     * Current irrigation condition
     * =========================================================
     */

    private LocalDateTime latestIrrigationReadingAt;

    private Double currentVwc15cm;

    private Double currentVwc30cm;

    private Double currentVwc60cm;

    private Integer pumpState;

    private Double rainfallMm;

    private Double et0Mm;

    private Double etcMm;

    private String irrigationSource;

    private String irrigationSensorStatus;


    /*
     * =========================================================
     * Fertilizer / nutrient condition
     * =========================================================
     */

    private Double nitrogenBalance;

    private Double phosphorusBalance;

    private Double potassiumBalance;

    private Double sulphurBalance;

    private Double zincBalance;

    private String nitrogenStatus;

    private String phosphorusStatus;

    private String potassiumStatus;

    private String sulphurStatus;

    private String zincStatus;


    /*
     * =========================================================
     * Weather / risk
     * =========================================================
     */

    private LocalDateTime latestWeatherTime;

    private Double latestTemperature;

    private Double latestHumidity;

    private Double latestRainfall;

    private Double latestWindSpeed;

    private String latestWeatherCondition;

    private Integer activeWeatherRiskCount;

    private String highestWeatherRisk;

    private Double highestWeatherRiskScore;


    /*
     * =========================================================
     * Historical counts
     * =========================================================
     */

    private Integer cropHistoryCount;

    private Integer soilSampleCount;

    private Integer irrigationEventCount;

    private Integer irrigationTelemetryCount;

    private Integer fertilizerRecommendationCount;

    private Integer fertilizerScheduleCount;

    private Integer weatherRecordCount;

    private Integer nutrientBalanceCount;

    private Integer weatherRiskCount;

    private Integer recoveryPlanCount;


    /*
     * =========================================================
     * Historical crop timeline
     * =========================================================
     */

    private List<CropPassportItem> cropHistory;


    /*
     * =========================================================
     * Recovery plans
     * =========================================================
     */

    private List<RecoveryPassportItem> recoveryPlans;


    /*
     * =========================================================
     * Provenance / limitations
     * =========================================================
     */

    private String dataStatus;

    private String source;

    private String uncertaintyNote;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CropPassportItem {

        private UUID id;

        private UUID cropId;

        private String cropName;

        private String season;

        private LocalDate sowingDate;

        private LocalDate harvestDate;

        private String cropStage;

        private Boolean residuePresent;

        private String residueType;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecoveryPassportItem {

        private UUID id;

        private Integer seasonNumber;

        private String season;

        private LocalDate startDate;

        private LocalDate endDate;

        private String focusArea;

        private Double baselineValue;

        private Double currentValue;

        private Double targetValue;

        private String unit;

        private String trend;

        private String actionType;

        private String recommendation;

        private String status;

        private String dataStatus;
    }
}