package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowingAdvisorResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    /*
     * Final decision.
     *
     * SOW_NOW
     * WAIT
     * UNSUITABLE
     * INSUFFICIENT_DATA
     */
    private String decision;

    /*
     * Recommended sowing window.
     */
    private String recommendedWindow;

    /*
     * Crop season.
     */
    private String season;

    /*
     * Current field information.
     */
    private Double currentMoisture;

    private Double currentTemperature;

    private Integer currentHumidity;

    private Double recentRainfall;

    private Double forecastRainfall;

    private Double forecastConfidence;

    /*
     * Crop suitability information.
     */
    private Double idealTemperatureMin;

    private Double idealTemperatureMax;

    private Double soilPh;

    private Double cropSoilPhMin;

    private Double cropSoilPhMax;

    /*
     * Decision explanation.
     */
    private String reason;

    private String contributingFactors;

    /*
     * Farmer-facing recommendation.
     */
    private String recommendation;

    /*
     * Provenance.
     */
    private String dataUsed;

    private String dataStatus;

    /*
     * Methodology version.
     */
    private String methodologyVersion;

    /*
     * Confidence.
     */
    private Double confidence;

    /*
     * Limitations / uncertainty.
     */
    private String uncertaintyNote;

    /*
     * Evaluation time.
     */
    private LocalDateTime evaluatedAt;

    /*
     * Validity.
     */
    private LocalDateTime validUntil;
}