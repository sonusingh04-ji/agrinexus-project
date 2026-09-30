package com.agrinexus.service;

import com.agrinexus.dto.TillageAdvisorRequest;
import com.agrinexus.dto.TillageAdvisorResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SatelliteFieldData;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilSampleRepository;
import com.agrinexus.repository.SatelliteFieldDataRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TillageAdvisorService {

    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final CropHistoryRepository cropHistoryRepository;
    private final SoilSampleRepository soilSampleRepository;
    private final SatelliteFieldDataRepository satelliteFieldDataRepository;
    private final WeatherRecordRepository weatherRecordRepository;

    /*
     * Generate tillage advice for a farm.
     *
     * This is an explainable rule-based MVP.
     */
    public TillageAdvisorResponse getTillageAdvice(
            TillageAdvisorRequest request
    ) {

        /*
         * ---------------------------------------------------------
         * 1. Validate required input
         * ---------------------------------------------------------
         */
        if (request.getFarmId() == null) {
            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getUpcomingCropId() == null) {
            throw new RuntimeException(
                    "Upcoming crop ID is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * 2. Find farm
         * ---------------------------------------------------------
         */
        Farm farm =
                farmRepository.findById(
                                request.getFarmId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        /*
         * ---------------------------------------------------------
         * 3. Find upcoming crop
         * ---------------------------------------------------------
         */
        Crop upcomingCrop =
                cropRepository.findById(
                                request.getUpcomingCropId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Upcoming crop not found with ID: "
                                                + request.getUpcomingCropId()
                                )
                        );

        /*
         * ---------------------------------------------------------
         * 4. Determine previous crop
         *
         * Priority:
         *    1. Explicit previousCropId
         *    2. Latest crop history
         * ---------------------------------------------------------
         */
        CropHistory latestHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                request.getFarmId()
                        );

        Crop previousCropEntity = null;

        if (request.getPreviousCropId() != null) {

            previousCropEntity =
                    cropRepository.findById(
                                    request.getPreviousCropId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Previous crop not found with ID: "
                                                    + request.getPreviousCropId()
                                    )
                            );

        } else if (latestHistory != null
                && latestHistory.getCrop() != null) {

            previousCropEntity =
                    latestHistory.getCrop();
        }

        String previousCrop =
                previousCropEntity != null
                        ? previousCropEntity.getName()
                        : null;

        /*
         * ---------------------------------------------------------
         * 5. Determine residue information
         *
         * Priority:
         *    1. Request value
         *    2. Latest crop history
         * ---------------------------------------------------------
         */
        Boolean residuePresent =
                request.getResiduePresent();

        String residueType =
                request.getResidueType();

        if (latestHistory != null) {

            if (residuePresent == null) {
                residuePresent =
                        latestHistory.getResiduePresent();
            }

            if (residueType == null) {
                residueType =
                        latestHistory.getResidueType();
            }
        }

        /*
         * Default value.
         */
        if (residuePresent == null) {
            residuePresent = false;
        }

        /*
         * ---------------------------------------------------------
         * 6. Latest soil sample
         * ---------------------------------------------------------
         */
        var latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                request.getFarmId()
                        );

        /*
         * ---------------------------------------------------------
         * 7. Current soil moisture
         *
         * Priority:
         *    1. Request value
         *    2. Latest satellite field data
         * ---------------------------------------------------------
         */
        Double soilMoisture =
                request.getSoilMoisture();

        if (soilMoisture == null) {

            SatelliteFieldData latestSatellite =
                    satelliteFieldDataRepository
                            .findByFarmIdOrderByRecordedAtDesc(
                                    request.getFarmId()
                            )
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (latestSatellite != null) {
                soilMoisture =
                        latestSatellite.getSoilMoisture();
            }
        }

        /*
         * ---------------------------------------------------------
         * 8. Current weather
         *
         * Priority:
         *    1. Request value
         *    2. Latest WeatherRecord
         * ---------------------------------------------------------
         */
        String weatherCondition =
                request.getWeatherCondition();

        if (weatherCondition == null
                || weatherCondition.isBlank()) {

            WeatherRecord latestWeather =
                    weatherRecordRepository
                            .findByFarmIdOrderByRecordedAtDesc(
                                    request.getFarmId()
                            )
                            .stream()
                            .findFirst()
                            .orElse(null);

            if (latestWeather != null) {
                weatherCondition =
                        latestWeather.getWeatherCondition();
            }
        }

        /*
         * ---------------------------------------------------------
         * 9. Soil type
         *
         * Priority:
         *    1. Request value
         *    2. Latest soil sample
         * ---------------------------------------------------------
         */
        String soilType =
                request.getSoilType();

        if (soilType == null
                || soilType.isBlank()) {

            if (latestSoil != null) {
                soilType =
                        latestSoil.getSoilType();
            }
        }

        /*
         * ---------------------------------------------------------
         * 10. Decision variables
         * ---------------------------------------------------------
         */
        String decision;
        String recommendation;
        String reason;
        String tillageType;

        /*
         * ---------------------------------------------------------
         * RULE 1
         *
         * Very low soil moisture.
         * ---------------------------------------------------------
         */
        if (soilMoisture != null
                && soilMoisture < 15.0) {

            decision = "WAIT";

            tillageType = "WAIT";

            recommendation =
                    "Wait before tillage and assess soil "
                            + "moisture before field preparation.";

            reason =
                    "Current recorded soil moisture is "
                            + soilMoisture
                            + "%. The field appears dry, so "
                            + "tillage should not be recommended "
                            + "automatically without field-specific "
                            + "moisture guidance.";

            /*
             * ---------------------------------------------------------
             * RULE 2
             *
             * Very wet soil.
             * ---------------------------------------------------------
             */
        } else if (soilMoisture != null
                && soilMoisture > 80.0) {

            decision = "WAIT";

            tillageType = "WAIT";

            recommendation =
                    "Do not till under the current very wet "
                            + "condition. Wait for suitable soil "
                            + "moisture.";

            reason =
                    "Current recorded soil moisture is "
                            + soilMoisture
                            + "%. Tillage under excessively wet "
                            + "conditions can damage soil structure.";

            /*
             * ---------------------------------------------------------
             * RULE 3
             *
             * Unsuitable weather.
             *
             * Weather check is intentionally before residue rules.
             * ---------------------------------------------------------
             */
        } else if (weatherCondition != null
                && isUnsuitableWeather(weatherCondition)) {

            decision = "WAIT";

            tillageType = "WAIT";

            recommendation =
                    "Wait for more suitable weather conditions "
                            + "before field preparation.";

            reason =
                    "Current weather is described as "
                            + weatherCondition
                            + ", which may be unsuitable for "
                            + "field operations.";

            /*
             * ---------------------------------------------------------
             * RULE 4
             *
             * Rice → Wheat + Rice residue.
             * ---------------------------------------------------------
             */
        } else if (residuePresent
                && previousCrop != null
                && previousCrop.equalsIgnoreCase("Rice")
                && upcomingCrop.getName() != null
                && upcomingCrop.getName()
                .equalsIgnoreCase("Wheat")) {

            decision = "RECOMMEND";

            tillageType = "ZERO_TILLAGE";

            recommendation =
                    "Consider zero tillage or minimum tillage "
                            + "for Wheat following Rice residue.";

            reason =
                    "Rice residue is present and Wheat is the "
                            + "upcoming crop. The supplied crop "
                            + "reference identifies zero/minimum "
                            + "tillage as a common approach in the "
                            + "rice-wheat rotation.";

            /*
             * ---------------------------------------------------------
             * RULE 5
             *
             * Generic residue case.
             * ---------------------------------------------------------
             */
        } else if (residuePresent) {

            decision = "RECOMMEND";

            tillageType = "MINIMUM_TILLAGE";

            recommendation =
                    "Consider minimum tillage to account for "
                            + "the crop residue while preparing "
                            + "the field.";

            reason =
                    "Crop residue is present in the field, so "
                            + "minimum tillage may be preferable "
                            + "to unnecessary full tillage, subject "
                            + "to crop- and soil-specific guidance.";

            /*
             * ---------------------------------------------------------
             * RULE 6
             *
             * Normal conditions.
             * ---------------------------------------------------------
             */
        } else {

            decision = "RECOMMEND";

            tillageType = "FULL_TILLAGE";

            recommendation =
                    "Field preparation can be considered, "
                            + "subject to local agronomic guidance.";

            reason =
                    "No major tillage restriction was detected "
                            + "from the currently available field "
                            + "information.";
        }

        /*
         * ---------------------------------------------------------
         * 11. Build response
         * ---------------------------------------------------------
         */
        return TillageAdvisorResponse.builder()
                .farmId(farm.getId())
                .upcomingCropId(
                        upcomingCrop.getId()
                )
                .upcomingCropName(
                        upcomingCrop.getName()
                )
                .previousCrop(previousCrop)
                .decision(decision)
                .recommendation(recommendation)
                .reason(reason)
                .soilMoisture(soilMoisture)
                .soilType(soilType)
                .residuePresent(residuePresent)
                .residueType(residueType)
                .weatherCondition(weatherCondition)
                .tillageType(tillageType)
                .build();
    }

    /*
     * Basic weather suitability check.
     */
    private boolean isUnsuitableWeather(
            String weatherCondition
    ) {

        String weather =
                weatherCondition.toLowerCase();

        return weather.contains("heavy rain")
                || weather.contains("extreme heat")
                || weather.contains("severe heat")
                || weather.contains("strong wind")
                || weather.contains("high wind")
                || weather.contains("storm");
    }
}