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
public class DigitalMemoryResponse {

    private UUID farmId;

    private String farmName;

    private String location;

    private LocalDateTime generatedAt;

    /*
     * =========================================================
     * Summary
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

    /*
     * =========================================================
     * Historical records
     * =========================================================
     */

    private List<CropMemoryItem> crops;

    private List<SoilMemoryItem> soil;

    private List<IrrigationMemoryItem> irrigationEvents;

    private List<IrrigationTelemetryMemoryItem> irrigationTelemetry;

    private List<FertilizerMemoryItem> fertilizerRecommendations;

    private List<FertilizerScheduleMemoryItem> fertilizerSchedules;

    private List<WeatherMemoryItem> weather;

    private List<NutrientBalanceMemoryItem> nutrientBalances;

    private List<WeatherRiskMemoryItem> weatherRisks;

    /*
     * =========================================================
     * Provenance / uncertainty
     * =========================================================
     */

    private String dataStatus;

    private String source;

    private String uncertaintyNote;


    /*
     * =========================================================
     * Nested records
     * =========================================================
     */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CropMemoryItem {

        private UUID id;

        private UUID cropId;

        private String cropName;

        private String season;

        private LocalDate sowingDate;

        private LocalDate harvestDate;

        private String cropStage;

        private Boolean residuePresent;

        private String residueType;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SoilMemoryItem {

        private UUID id;

        private LocalDate sampleDate;

        private String soilType;

        private Double pH;

        private Double electricalConductivity;

        private Double organicCarbon;

        private Double nitrogen;

        private Double phosphorus;

        private Double potassium;

        private Double zinc;

        private Double sulphur;

        private Double iron;

        private Double manganese;

        private Double copper;

        private String source;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IrrigationMemoryItem {

        private UUID id;

        private String deviceId;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Double durationMinutes;

        private Double volumeLiters;

        private String triggerReason;

        private String completionStatus;

        private Double moistureBefore;

        private Double moistureAfter;

        private Double targetMoisture;

        private Boolean pumpWasOn;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IrrigationTelemetryMemoryItem {

        private UUID id;

        private String deviceId;

        private LocalDateTime recordedAt;

        private Double vwc15cm;

        private Double vwc30cm;

        private Double vwc60cm;

        private Integer pumpState;

        private Double flowRateLpm;

        private Double linePressureBar;

        private Double waterTableDepthM;

        private Double pumpRuntimeMinutes;

        private Double rainfallMm;

        private Double et0Mm;

        private Double etcMm;

        private Double solarIrradianceWm2;

        private Double solarPowerKw;

        private Double cloudCoverPercent;

        private Double storageLevelPercent;

        private Double batterySocPercent;

        private String cropStage;

        private Double kc;

        private String source;

        private String sensorStatus;

        private String payloadReference;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FertilizerMemoryItem {

        private UUID id;

        private UUID cropId;

        private String cropName;

        private String cropStage;

        private Double fieldArea;

        private String fieldAreaUnit;

        private Double nitrogen;

        private Double phosphorus;

        private Double potassium;

        private Double sulphur;

        private Double zinc;

        private String status;

        private String recommendation;

        private String reason;

        private String dataStatus;

        private String source;

        private String uncertaintyNote;

        private LocalDateTime generatedAt;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FertilizerScheduleMemoryItem {

        private UUID id;

        private UUID cropId;

        private String cropName;

        private UUID recommendationId;

        private String stage;

        private LocalDate plannedDate;

        private String actionType;

        private String productName;

        private Double nitrogen;

        private Double phosphorus;

        private Double potassium;

        private Double sulphur;

        private Double zinc;

        private String dataStatus;

        private String status;

        private String weatherCondition;

        private String reason;

        private String recommendation;

        private Boolean reassessmentRequired;

        private String source;

        private String uncertaintyNote;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherMemoryItem {

        private UUID id;

        private LocalDateTime recordedAt;

        private Double temperature;

        private Double feelsLike;

        private Integer humidity;

        private Double rainfall;

        private Double windSpeed;

        private Integer windDirection;

        private Double pressure;

        private String weatherCondition;

        private String weatherDescription;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NutrientBalanceMemoryItem {

        private UUID id;

        private UUID cropId;

        private String cropName;

        private LocalDate periodStart;

        private LocalDate periodEnd;

        private Double nitrogenInput;

        private Double phosphorusInput;

        private Double potassiumInput;

        private Double sulphurInput;

        private Double zincInput;

        private Double nitrogenUptake;

        private Double phosphorusUptake;

        private Double potassiumUptake;

        private Double sulphurUptake;

        private Double zincUptake;

        private Double nitrogenResidueReturn;

        private Double phosphorusResidueReturn;

        private Double potassiumResidueReturn;

        private Double sulphurResidueReturn;

        private Double zincResidueReturn;

        private Double nitrogenBiologicalFixation;

        private Double nitrogenBalance;

        private Double phosphorusBalance;

        private Double potassiumBalance;

        private Double sulphurBalance;

        private Double zincBalance;

        private String dataStatus;

        private String source;

        private String notes;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherRiskMemoryItem {

        private UUID id;

        private String riskType;

        private String riskLevel;

        private Double riskScore;

        private String title;

        private String description;

        private String recommendation;

        private LocalDateTime detectedAt;

        private LocalDateTime validUntil;
    }
}