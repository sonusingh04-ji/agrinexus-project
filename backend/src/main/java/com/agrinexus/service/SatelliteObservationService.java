package com.agrinexus.service;

import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SatelliteObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SatelliteObservationService {

    private final SatelliteObservationRepository observationRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;

    /*
     * -------------------------------------------------------------
     * Create satellite observation.
     * -------------------------------------------------------------
     */
    @Transactional
    public SatelliteObservation create(
            UUID farmId,
            UUID cropId,
            LocalDateTime observedAt,
            String satelliteSource,
            String sceneId,
            Double ndvi,
            Double ndwi,
            Double cloudCoverPercent,
            Double spatialResolutionMeters,
            String dataStatus,
            String dataSource,
            Double confidence,
            Boolean forecastBased
    ) {

        Farm farm =
                farmRepository.findById(
                        farmId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + farmId
                        )
                );

        Crop crop = null;

        if (cropId != null) {

            crop =
                    cropRepository.findById(
                            cropId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Crop not found with ID: "
                                            + cropId
                            )
                    );
        }

        /*
         * Calculate crop condition from NDVI.
         *
         * These are broad screening thresholds.
         * They are NOT a crop-specific diagnosis.
         */
        String cropCondition =
                classifyCropCondition(
                        ndvi
                );

        /*
         * Compare with latest previous observation.
         */
        SatelliteObservation previous =
                observationRepository
                        .findFirstByFarmIdOrderByObservedAtDesc(
                                farmId
                        )
                        .orElse(null);

        String changeStatus =
                determineChangeStatus(
                        ndvi,
                        previous != null
                                ? previous.getNdvi()
                                : null
                );

        Double changePercent =
                calculateChangePercent(
                        ndvi,
                        previous != null
                                ? previous.getNdvi()
                                : null
                );

        String interpretation =
                buildInterpretation(
                        cropCondition,
                        changeStatus
                );

        String recommendation =
                buildRecommendation(
                        cropCondition,
                        changeStatus
                );

        String uncertaintyNote =
                buildUncertaintyNote(
                        dataStatus,
                        dataSource
                );

        Double latitude =
                farm.getLatitude();

        Double longitude =
                farm.getLongitude();

        SatelliteObservation observation =
                SatelliteObservation.builder()
                        .farm(farm)
                        .crop(crop)
                        .observedAt(
                                observedAt != null
                                        ? observedAt
                                        : LocalDateTime.now()
                        )
                        .satelliteSource(
                                safeValue(
                                        satelliteSource,
                                        "UNKNOWN"
                                )
                        )
                        .sceneId(
                                sceneId
                        )
                        .ndvi(
                                ndvi
                        )
                        .ndwi(
                                ndwi
                        )
                        .cropCondition(
                                cropCondition
                        )
                        .changeStatus(
                                changeStatus
                        )
                        .changePercent(
                                changePercent
                        )
                        .cloudCoverPercent(
                                cloudCoverPercent
                        )
                        .spatialResolutionMeters(
                                spatialResolutionMeters
                        )
                        .dataStatus(
                                safeValue(
                                        dataStatus,
                                        "TEST"
                                )
                        )
                        .dataSource(
                                safeValue(
                                        dataSource,
                                        "TEST"
                                )
                        )
                        .confidence(
                                confidence
                        )
                        .interpretation(
                                interpretation
                        )
                        .recommendation(
                                recommendation
                        )
                        .uncertaintyNote(
                                uncertaintyNote
                        )
                        .latitude(
                                latitude
                        )
                        .longitude(
                                longitude
                        )
                        .forecastBased(
                                forecastBased != null
                                        ? forecastBased
                                        : false
                        )
                        .build();

        return observationRepository.save(
                observation
        );
    }

    /*
     * -------------------------------------------------------------
     * Latest observation.
     * -------------------------------------------------------------
     */
    public SatelliteObservation getLatest(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return observationRepository
                .findFirstByFarmIdOrderByObservedAtDesc(
                        farmId
                )
                .orElse(null);
    }

    /*
     * -------------------------------------------------------------
     * All observations for farm.
     * -------------------------------------------------------------
     */
    public List<SatelliteObservation> getByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return observationRepository
                .findByFarmIdOrderByObservedAtDesc(
                        farmId
                );
    }

    /*
     * -------------------------------------------------------------
     * Declining crop-condition observations.
     * -------------------------------------------------------------
     */
    public List<SatelliteObservation> getDeclining(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );

        return observationRepository
                .findByFarmIdAndChangeStatusOrderByObservedAtDesc(
                        farmId,
                        "DECLINING"
                );
    }

    /*
     * -------------------------------------------------------------
     * Get observations by source.
     * -------------------------------------------------------------
     */
    public List<SatelliteObservation> getBySource(
            UUID farmId,
            String source
    ) {

        verifyFarm(
                farmId
        );

        return observationRepository
                .findByFarmIdAndDataSourceOrderByObservedAtDesc(
                        farmId,
                        source
                );
    }

    /*
     * -------------------------------------------------------------
     * Get observations by data status.
     * -------------------------------------------------------------
     */
    public List<SatelliteObservation> getByDataStatus(
            UUID farmId,
            String dataStatus
    ) {

        verifyFarm(
                farmId
        );

        return observationRepository
                .findByFarmIdAndDataStatusOrderByObservedAtDesc(
                        farmId,
                        dataStatus
                );
    }

    /*
     * -------------------------------------------------------------
     * Get observation by ID.
     * -------------------------------------------------------------
     */
    public SatelliteObservation getById(
            UUID id
    ) {

        return observationRepository.findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Satellite observation not found with ID: "
                                        + id
                        )
                );
    }

    /*
     * -------------------------------------------------------------
     * NDVI classification.
     *
     * NOTE:
     * These broad thresholds are screening thresholds.
     * They should be calibrated for crop, sensor and region
     * before operational use.
     * -------------------------------------------------------------
     */
    private String classifyCropCondition(
            Double ndvi
    ) {

        if (ndvi == null) {

            return "UNKNOWN";
        }

        if (ndvi >= 0.70) {

            return "HEALTHY";
        }

        if (ndvi >= 0.50) {

            return "NORMAL";
        }

        if (ndvi >= 0.30) {

            return "STRESSED";
        }

        return "SEVERELY_STRESSED";
    }

    /*
     * -------------------------------------------------------------
     * Compare current NDVI with previous NDVI.
     * -------------------------------------------------------------
     */
    private String determineChangeStatus(
            Double currentNdvi,
            Double previousNdvi
    ) {

        if (currentNdvi == null
                || previousNdvi == null) {

            return "UNKNOWN";
        }

        double difference =
                currentNdvi
                        - previousNdvi;

        /*
         * Small variations are treated as stable.
         */
        if (difference > 0.05) {

            return "IMPROVING";
        }

        if (difference < -0.05) {

            return "DECLINING";
        }

        return "STABLE";
    }

    /*
     * -------------------------------------------------------------
     * NDVI change percentage.
     * -------------------------------------------------------------
     */
    private Double calculateChangePercent(
            Double currentNdvi,
            Double previousNdvi
    ) {

        if (currentNdvi == null
                || previousNdvi == null
                || previousNdvi == 0.0) {

            return null;
        }

        return
                (
                        (
                                currentNdvi
                                        - previousNdvi
                        )
                                / Math.abs(
                                previousNdvi
                        )
                )
                        * 100.0;
    }

    /*
     * -------------------------------------------------------------
     * Interpretation.
     * -------------------------------------------------------------
     */
    private String buildInterpretation(
            String cropCondition,
            String changeStatus
    ) {

        if ("HEALTHY".equals(
                cropCondition
        )) {

            if ("DECLINING".equals(
                    changeStatus
            )) {

                return
                        "Vegetation currently falls in a relatively healthy range, "
                                + "but the latest observation shows a decline compared with the previous observation.";
            }

            return
                    "Vegetation currently falls in a relatively healthy range.";
        }

        if ("NORMAL".equals(
                cropCondition
        )) {

            if ("DECLINING".equals(
                    changeStatus
            )) {

                return
                        "Vegetation is currently in a normal range, but the latest observation indicates a decline.";
            }

            return
                    "Vegetation is currently in a normal screening range.";
        }

        if ("STRESSED".equals(
                cropCondition
        )) {

            return
                    "Satellite-derived vegetation indicators suggest possible crop stress.";
        }

        if ("SEVERELY_STRESSED".equals(
                cropCondition
        )) {

            return
                    "Satellite-derived vegetation indicators suggest substantial crop stress.";
        }

        return
                "A reliable vegetation-condition interpretation is not available from the current observation.";
    }

    /*
     * -------------------------------------------------------------
     * Farmer recommendation.
     * -------------------------------------------------------------
     */
    private String buildRecommendation(
            String cropCondition,
            String changeStatus
    ) {

        if ("DECLINING".equals(
                changeStatus
        )) {

            return
                    "Inspect the field for changes in crop condition, "
                            + "water stress, nutrient issues, pests or disease symptoms before taking corrective action.";
        }

        if ("SEVERELY_STRESSED".equals(
                cropCondition
        )) {

            return
                    "Inspect the crop and field conditions promptly and compare with local observations before taking corrective action.";
        }

        if ("STRESSED".equals(
                cropCondition
        )) {

            return
                    "Scout the field and compare satellite indicators with current soil, weather and crop observations.";
        }

        if ("HEALTHY".equals(
                cropCondition
        )) {

            return
                    "Continue normal monitoring and compare future observations for meaningful changes.";
        }

        return
                "Continue field monitoring and validate the satellite signal with local observations.";
    }

    /*
     * -------------------------------------------------------------
     * Uncertainty / provenance note.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            String dataStatus,
            String dataSource
    ) {

        StringBuilder note =
                new StringBuilder();

        note.append(
                "Satellite-derived indicators are screening signals and should not be treated as a standalone diagnosis."
        );

        note.append(
                " Validate important changes with field observations and appropriate agricultural guidance."
        );

        if ("TEST".equalsIgnoreCase(
                dataStatus
        )) {

            note.append(
                    " This observation is synthetic test data."
            );
        }

        if (dataSource != null
                && !dataSource.isBlank()) {

            note.append(
                    " Data source: "
            );

            note.append(
                    dataSource
            );

            note.append(".");
        }

        return note.toString();
    }

    /*
     * -------------------------------------------------------------
     * Farm verification.
     * -------------------------------------------------------------
     */
    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    /*
     * -------------------------------------------------------------
     * Safe value helper.
     * -------------------------------------------------------------
     */
    private String safeValue(
            String value,
            String fallback
    ) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return value.trim();
    }
}