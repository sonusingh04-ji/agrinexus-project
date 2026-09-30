package com.agrinexus.service;

import com.agrinexus.dto.SatelliteFieldDataRequest;
import com.agrinexus.dto.SatelliteFieldDataResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SatelliteFieldData;
import com.agrinexus.enums.FieldHealthStatus;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SatelliteFieldDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SatelliteFieldDataService {

    private final SatelliteFieldDataRepository satelliteFieldDataRepository;
    private final FarmRepository farmRepository;

    /*
     * ============================================================
     * CREATE SATELLITE FIELD DATA
     * ============================================================
     *
     * Stores satellite-derived field information and calculates
     * the field health status.
     */
    public SatelliteFieldDataResponse createSatelliteFieldData(
            SatelliteFieldDataRequest request
    ) {

        Farm farm = farmRepository
                .findById(request.getFarmId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + request.getFarmId()
                        )
                );

        FieldHealthAnalysisResult analysisResult =
                analyzeFieldHealth(request);

        /*
         * IMPORTANT:
         *
         * Database column health_status is VARCHAR/String.
         *
         * Java analysis uses FieldHealthStatus enum.
         *
         * Therefore:
         *
         * FieldHealthStatus.HEALTHY
         *              ↓
         * "HEALTHY"
         *
         * This keeps PostgreSQL compatible with the enum.
         */
        SatelliteFieldData satelliteFieldData =
                SatelliteFieldData.builder()
                        .farm(farm)
                        .ndvi(request.getNdvi())
                        .vegetationHealth(request.getVegetationHealth())
                        .soilMoisture(request.getSoilMoisture())
                        .cloudCoverage(request.getCloudCoverage())

                        .healthStatus(
                                analysisResult
                                        .getHealthStatus()
                                        .name()
                        )

                        .recommendation(
                                analysisResult.getRecommendation()
                        )

                        .recordedAt(LocalDateTime.now())
                        .build();

        SatelliteFieldData savedData =
                satelliteFieldDataRepository.save(
                        satelliteFieldData
                );

        return toResponse(savedData);
    }


    /*
     * ============================================================
     * GET SATELLITE DATA BY FARM
     * ============================================================
     *
     * Returns latest satellite observations first.
     */
    public List<SatelliteFieldDataResponse>
    getSatelliteFieldDataByFarmId(UUID farmId) {

        return satelliteFieldDataRepository
                .findByFarmIdOrderByRecordedAtDesc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    /*
     * ============================================================
     * FIELD HEALTH ANALYSIS
     * ============================================================
     *
     * Calculates field health from satellite indicators.
     *
     * NDVI:
     *   < 0.20  -> CRITICAL
     *   < 0.40  -> POOR
     *   < 0.60  -> MODERATE
     *   >= 0.60 -> HEALTHY
     *
     * Additional indicators:
     *   vegetationHealth
     *   soilMoisture
     */
    private FieldHealthAnalysisResult analyzeFieldHealth(
            SatelliteFieldDataRequest request
    ) {

        Double ndvi = request.getNdvi();

        Double vegetationHealth =
                request.getVegetationHealth();

        Double soilMoisture =
                request.getSoilMoisture();

        FieldHealthStatus healthStatus;

        String recommendation;


        /*
         * ========================================================
         * CRITICAL
         * ========================================================
         */
        if (ndvi != null && ndvi < 0.20) {

            healthStatus =
                    FieldHealthStatus.CRITICAL;

            recommendation =
                    "Critical vegetation stress detected. " +
                            "Inspect the field immediately and check " +
                            "irrigation, crop health, pests, and diseases.";
        }


        /*
         * ========================================================
         * POOR
         * ========================================================
         */
        else if (
                (ndvi != null && ndvi < 0.40)
                        ||
                        (vegetationHealth != null
                                && vegetationHealth < 40)
                        ||
                        (soilMoisture != null
                                && soilMoisture < 20)
        ) {

            healthStatus =
                    FieldHealthStatus.POOR;

            recommendation =
                    "Poor field health detected. " +
                            "Check irrigation, soil moisture, crop " +
                            "nutrition, pests, and diseases.";
        }


        /*
         * ========================================================
         * MODERATE
         * ========================================================
         */
        else if (
                (ndvi != null && ndvi < 0.60)
                        ||
                        (vegetationHealth != null
                                && vegetationHealth < 70)
                        ||
                        (soilMoisture != null
                                && soilMoisture < 40)
        ) {

            healthStatus =
                    FieldHealthStatus.MODERATE;

            recommendation =
                    "Moderate field health detected. " +
                            "Continue monitoring vegetation health " +
                            "and maintain proper irrigation.";
        }


        /*
         * ========================================================
         * HEALTHY
         * ========================================================
         */
        else {

            healthStatus =
                    FieldHealthStatus.HEALTHY;

            recommendation =
                    "Field vegetation appears healthy. " +
                            "Continue regular monitoring and maintain " +
                            "the current crop care plan.";
        }


        return new FieldHealthAnalysisResult(
                healthStatus,
                recommendation
        );
    }


    /*
     * ============================================================
     * ENTITY -> RESPONSE
     * ============================================================
     *
     * Database:
     *
     *     "HEALTHY"
     *
     * Java:
     *
     *     FieldHealthStatus.HEALTHY
     *
     * The response DTO currently expects FieldHealthStatus,
     * so we safely convert the stored String back to the enum.
     */
    private SatelliteFieldDataResponse toResponse(
            SatelliteFieldData data
    ) {

        FieldHealthStatus healthStatus =
                parseHealthStatus(
                        data.getHealthStatus()
                );

        return SatelliteFieldDataResponse.builder()

                .id(data.getId())

                .farmId(
                        data.getFarm().getId()
                )

                .ndvi(
                        data.getNdvi()
                )

                .vegetationHealth(
                        data.getVegetationHealth()
                )

                .soilMoisture(
                        data.getSoilMoisture()
                )

                .cloudCoverage(
                        data.getCloudCoverage()
                )

                .healthStatus(
                        healthStatus
                )

                .recommendation(
                        data.getRecommendation()
                )

                .recordedAt(
                        data.getRecordedAt()
                )

                .build();
    }


    /*
     * ============================================================
     * STRING -> FIELD HEALTH STATUS
     * ============================================================
     *
     * Converts PostgreSQL VARCHAR into Java enum.
     */
    private FieldHealthStatus parseHealthStatus(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            return FieldHealthStatus.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            /*
             * Do not crash the complete API if old or externally
             * imported satellite data contains an unknown status.
             */
            return null;
        }
    }


    /*
     * ============================================================
     * INTERNAL ANALYSIS RESULT
     * ============================================================
     */
    private static class FieldHealthAnalysisResult {

        private final FieldHealthStatus healthStatus;

        private final String recommendation;


        public FieldHealthAnalysisResult(
                FieldHealthStatus healthStatus,
                String recommendation
        ) {

            this.healthStatus =
                    healthStatus;

            this.recommendation =
                    recommendation;
        }


        public FieldHealthStatus getHealthStatus() {

            return healthStatus;
        }


        public String getRecommendation() {

            return recommendation;
        }
    }
}