package com.agrinexus.service;

import com.agrinexus.dto.SmartIrrigationRequest;
import com.agrinexus.dto.SmartIrrigationResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropWaterProfile;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationConfig;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.WeatherForecastData;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.CropWaterProfileRepository;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationConfigRepository;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.WeatherForecastDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmartIrrigationService {

    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final IrrigationConfigRepository irrigationConfigRepository;
    private final IrrigationTelemetryRepository irrigationTelemetryRepository;
    private final CropWaterProfileRepository cropWaterProfileRepository;
    private final CropHistoryRepository cropHistoryRepository;
    private final WeatherForecastDataRepository weatherForecastDataRepository;

    /**
     * Evaluate irrigation requirement for a farm.
     *
     * Decision inputs are resolved from trusted backend records.
     * Numeric request overrides are intentionally not used for
     * production irrigation evaluation.
     */
    public SmartIrrigationResponse evaluate(
            SmartIrrigationRequest request
    ) {

        if (request == null || request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        UUID farmId =
                request.getFarmId();

        /*
         * ---------------------------------------------------------
         * Farm
         * ---------------------------------------------------------
         */
        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );

        /*
         * ---------------------------------------------------------
         * Current crop from real farm crop history
         * ---------------------------------------------------------
         */
        CropHistory activeCropHistory =
                cropHistoryRepository
                        .findFirstByFarmIdAndHarvestDateIsNullOrderBySowingDateDesc(farmId);

        Crop crop = activeCropHistory != null ? activeCropHistory.getCrop() : null;

        if (request.getCropId() != null) {
            Crop requestedCrop =
                    cropRepository.findById(request.getCropId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Crop not found with ID: " + request.getCropId()));

            if (activeCropHistory == null
                    || activeCropHistory.getCrop() == null
                    || !requestedCrop.getId().equals(activeCropHistory.getCrop().getId())) {
                throw new RuntimeException(
                        "Requested crop is not the active crop for this farm");
            }

            crop = requestedCrop;
        }

        String effectiveCropStage =
                request.getCropStage() != null && !request.getCropStage().isBlank()
                        ? request.getCropStage().trim()
                        : activeCropHistory != null
                        ? activeCropHistory.getCropStage()
                        : null;

        String effectiveSeason =
                activeCropHistory != null
                        ? activeCropHistory.getSeason()
                        : crop != null ? crop.getSeason() : null;
        /*
         * ---------------------------------------------------------
         * Irrigation configuration
         * ---------------------------------------------------------
         */
        IrrigationConfig config =
                irrigationConfigRepository.findByFarmId(
                        farmId
                );

        /*
         * ---------------------------------------------------------
         * Latest REAL telemetry only
         * ---------------------------------------------------------
         *
         * Synthetic/test telemetry must NEVER participate in a
         * real farm irrigation decision.
         */
        IrrigationTelemetry telemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        if (telemetry != null
                && (
                "TEST".equalsIgnoreCase(
                        telemetry.getSensorStatus()
                )
                        ||
                        "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                        ||
                        "TEST_TELEMETRY".equalsIgnoreCase(
                                telemetry.getSource()
                        )
        )) {

            telemetry = null;
        }

        /*
         * ---------------------------------------------------------
         * Crop-water profile
         * ---------------------------------------------------------
         */
        CropWaterProfile cropWaterProfile = null;

        if (crop != null) {
            String[] locationParts = parseLocation(farm.getLocation());
            String districtName = locationParts[0];
            String stateName = locationParts[1];

            List<CropWaterProfile> profiles =
                    cropWaterProfileRepository.findByCropId(crop.getId());

            cropWaterProfile = profiles.stream()
                    .filter(profile -> scopeMatches(profile.getStateName(), stateName))
                    .filter(profile -> scopeMatches(profile.getDistrictName(), districtName))
                    .filter(profile -> scopeMatches(profile.getSeason(), effectiveSeason))
                    .max((a, b) -> Integer.compare(
                            profileMatchScore(a, stateName, districtName, effectiveSeason),
                            profileMatchScore(b, stateName, districtName, effectiveSeason)))
                    .orElse(null);
        }

        /*
         * ---------------------------------------------------------
         * Stored weather forecast
         * ---------------------------------------------------------
         *
         * We retrieve the nearest upcoming forecast date.
         */
        LocalDate today =
                LocalDate.now();

        WeatherForecastData nearestForecast =
                weatherForecastDataRepository
                        .findFirstByFarmIdAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                                farmId,
                                today
                        );

        /*
         * ---------------------------------------------------------
         * Stored forecast records for the next 7 days
         * ---------------------------------------------------------
         *
         * Used for the "forecastRain7d" value.
         */
        List<WeatherForecastData> nextSevenDays =
                weatherForecastDataRepository
                        .findByFarmIdAndForecastDateBetweenOrderByForecastDateAsc(
                                farmId,
                                today,
                                today.plusDays(6)
                        );

        /*
         * ---------------------------------------------------------
         * Current soil moisture
         * ---------------------------------------------------------
         *
         * Source:
         * Latest accepted real telemetry
         */
        Double vwc = telemetry != null ? telemetry.getVwc30cm() : null;

        /*
         * ---------------------------------------------------------
         * Field capacity
         * ---------------------------------------------------------
         *
         * Source:
         * Stored farm irrigation configuration
         */
        Double fc = config != null ? config.getFieldCapacity() : null;

        /*
         * ---------------------------------------------------------
         * Wilting point
         * ---------------------------------------------------------
         */
        Double pwp = config != null ? config.getWiltingPoint() : null;

        /*
         * ---------------------------------------------------------
         * MAD
         * ---------------------------------------------------------
         */
        Double mad = config != null ? config.getMaxAllowableDepletion() : null;

        /*
         * ---------------------------------------------------------
         * ET0
         * ---------------------------------------------------------
         *
         * Source:
         * Stored forecast, falling back to accepted telemetry
         */
        Double et0 = null;

        if (nearestForecast != null) {

            et0 =
                    nearestForecast.getEt0Mm();
        }

        if (et0 == null
                && telemetry != null) {

            et0 =
                    telemetry.getEt0Mm();
        }

        /*
         * ---------------------------------------------------------
         * Crop coefficient Kc
         * ---------------------------------------------------------
         *
         * Source order:
         * accepted telemetry → resolved crop-water reference profile
         */
        Double kc = null;

        if (telemetry != null) {

            kc =
                    telemetry.getKc();
        }

        if (kc == null
                && cropWaterProfile != null) {

            kc =
                    resolveKcByStage(
                            cropWaterProfile,
                            effectiveCropStage
                    );
        }

        /*
         * ---------------------------------------------------------
         * Forecast rainfall
         * ---------------------------------------------------------
         *
         * Source order:
         * stored forecast sum → accepted telemetry rainfall
         */
        Double forecastRain7d = null;

        if (!nextSevenDays.isEmpty()) {

            forecastRain7d =
                    nextSevenDays.stream()
                            .map(WeatherForecastData::getRainfallMm)
                            .filter(value -> value != null)
                            .mapToDouble(Double::doubleValue)
                            .sum();
        }

        if (forecastRain7d == null
                && telemetry != null) {

            forecastRain7d =
                    telemetry.getRainfallMm();
        }

        /*
         * ---------------------------------------------------------
         * Forecast confidence
         * ---------------------------------------------------------
         *
         * For multiple forecasts we use the lowest available
         * confidence because it is the conservative interpretation
         * for decision support.
         *
         * Source:
         * stored forecast confidence
         */
        Double forecastConfidence = null;

        if (!nextSevenDays.isEmpty()) {

            forecastConfidence =
                    nextSevenDays.stream()
                            .map(
                                    WeatherForecastData
                                            ::getForecastConfidence
                            )
                            .filter(value -> value != null)
                            .min(Double::compareTo)
                            .orElse(null);
        }

        /*
         * ---------------------------------------------------------
         * Recent rainfall
         * ---------------------------------------------------------
         */
        Double recentRainfall = telemetry != null ? telemetry.getRainfallMm() : null;

        /*
         * ---------------------------------------------------------
         * Root-zone depth
         * ---------------------------------------------------------
         *
         * Source order:
         * stored configuration → resolved crop profile by stage
         */
        Double rootDepthCm = config != null ? config.getRootZoneDepthCm() : null;

        if (rootDepthCm == null
                && cropWaterProfile != null) {

            rootDepthCm =
                    resolveRootDepthByStage(
                            cropWaterProfile,
                            effectiveCropStage
                    );
        }

        /*
         * ---------------------------------------------------------
         * Calculate ETC
         * ---------------------------------------------------------
         */
        Double etc = null;

        if (et0 != null
                && kc != null) {

            etc =
                    et0 * kc;
        }

        /*
         * ---------------------------------------------------------
         * Decision variables
         * ---------------------------------------------------------
         */
        String status =
                "INSUFFICIENT_DATA";

        String action =
                "Additional soil-water or weather data is required.";

        Double depletion = null;

        /*
         * ---------------------------------------------------------
         * Water-balance calculation
         * ---------------------------------------------------------
         */
        if (vwc != null
                && fc != null
                && pwp != null
                && mad != null
                && fc > pwp) {

            /*
             * Current water available above PWP.
             */
            Double availableWater =
                    vwc - pwp;

            /*
             * Total plant-available water.
             */
            Double totalAvailable =
                    fc - pwp;

            /*
             * Fraction depleted.
             */
            depletion =
                    1.0
                            - (
                            availableWater
                                    / totalAvailable
                    );

            /*
             * Protect against numeric values outside
             * the normal 0–1 range.
             */
            depletion =
                    Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    depletion
                            )
                    );

            /*
             * -----------------------------------------------------
             * Irrigation classification
             * -----------------------------------------------------
             */
            if (depletion >= mad) {

                status =
                        "IRRIGATION_RECOMMENDED";

                action =
                        "Soil moisture has reached the configured depletion threshold. Evaluate irrigation.";

            } else if (depletion >= mad * 0.70) {

                status =
                        "MONITOR";

                action =
                        "Soil moisture is depleting. Continue monitoring before irrigation.";

            } else {

                status =
                        "SUFFICIENT";

                action =
                        "Soil water is currently within the configured range.";
            }
        }

        /*
         * ---------------------------------------------------------
         * Confidence-aware forecast handling
         * ---------------------------------------------------------
         *
         * If a stored forecast exists but confidence is lower
         * than the configured minimum, do not allow that forecast
         * to silently override the soil-water decision.
         */
        Double minimumConfidence =
                config != null
                        ? config.getMinimumForecastConfidence()
                        : null;

        boolean forecastReliable =
                forecastConfidence == null
                        || minimumConfidence == null
                        || forecastConfidence >= minimumConfidence;

        /*
         * ---------------------------------------------------------
         * Rainfall override
         * ---------------------------------------------------------
         *
         * Rainfall can soften an irrigation recommendation,
         * but only when:
         *
         * 1. rainfall exists
         * 2. ETc exists
         * 3. forecast confidence is acceptable
         * 4. forecast rainfall is at least the near-term crop demand
         */
        if ((status.equals("IRRIGATION_RECOMMENDED")
                || status.equals("MONITOR"))
                && forecastRain7d != null
                && etc != null
                && forecastRain7d >= etc
                && forecastReliable) {

            status =
                    "MONITOR";

            action =
                    "Forecast rainfall may satisfy near-term crop demand. Review the forecast before irrigating.";
        }

        /*
         * ---------------------------------------------------------
         * Forecast quality warning
         * ---------------------------------------------------------
         */
        if (forecastConfidence != null
                && minimumConfidence != null
                && forecastConfidence < minimumConfidence) {

            if (status.equals("IRRIGATION_RECOMMENDED")) {

                action =
                        action
                                + " Forecast confidence is below the configured minimum, so rainfall should not be relied upon without further verification.";
            } else {

                action =
                        action
                                + " Forecast confidence is below the configured minimum.";
            }
        }

        /*
         * ---------------------------------------------------------
         * Data provenance
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        request,
                        config,
                        telemetry,
                        nearestForecast,
                        cropWaterProfile
                );

        /*
         * ---------------------------------------------------------
         * Uncertainty message
         * ---------------------------------------------------------
         */
        String uncertaintyNote =
                buildUncertaintyNote(
                        config,
                        telemetry,
                        nearestForecast,
                        forecastConfidence,
                        minimumConfidence,
                        rootDepthCm
                );

        /*
         * ---------------------------------------------------------
         * Response
         * ---------------------------------------------------------
         */
        return SmartIrrigationResponse.builder()
                .farmId(
                        farm.getId()
                )
                .cropId(
                        crop != null
                                ? crop.getId()
                                : null
                )
                .cropName(
                        crop != null
                                ? crop.getName()
                                : null
                )
                .cropStage(
                        effectiveCropStage
                )
                .currentVwc(
                        vwc
                )
                .fieldCapacity(
                        fc
                )
                .wiltingPoint(
                        pwp
                )
                .mad(
                        mad
                )
                .depletionLevel(
                        depletion
                )
                .et0(
                        et0
                )
                .kc(
                        kc
                )
                .etc(
                        etc
                )
                .forecastRain7d(
                        forecastRain7d
                )
                .forecastConfidence(
                        forecastConfidence
                )
                .fieldStatus(
                        status
                )
                .actionItem(
                        action
                )
                .dataStatus(
                        dataStatus
                )
                .uncertaintyNote(
                        uncertaintyNote
                )
                .evaluatedAt(
                        LocalDateTime.now()
                )
                .build();
    }

    /*
     * -------------------------------------------------------------
     * Resolve Kc according to crop stage.
     * -------------------------------------------------------------
     */
    private Double resolveKcByStage(
            CropWaterProfile profile,
            String cropStage
    ) {

        if (cropStage == null
                || cropStage.isBlank()) {

            return null;
        }

        String stage =
                cropStage.trim()
                        .toUpperCase();

        switch (stage) {

            case "INITIAL":
            case "GERMINATION":
            case "SEEDLING":
            case "PLANTING":
            case "SOWING":
            case "PRE_SOWING":
                return profile.getKcInitial();

            case "DEVELOPMENT":
            case "VEGETATIVE":
            case "TILLERING":
                return profile.getKcDevelopment();

            case "MID":
            case "MID_SEASON":
            case "FLOWERING":
            case "GRAIN_FILLING":
                return profile.getKcMid();

            case "LATE":
            case "LATE_SEASON":
            case "MATURITY":
            case "HARVEST":
                return profile.getKcEnd();

            default:
                return profile.getKcInitial();
        }
    }

    /*
     * -------------------------------------------------------------
     * Resolve root depth according to crop stage.
     * -------------------------------------------------------------
     */
    private Double resolveRootDepthByStage(
            CropWaterProfile profile,
            String cropStage
    ) {

        if (cropStage == null
                || cropStage.isBlank()) {

            return null;
        }

        String stage =
                cropStage.trim()
                        .toUpperCase();

        switch (stage) {

            case "INITIAL":
            case "GERMINATION":
            case "SEEDLING":
            case "PLANTING":
            case "SOWING":
            case "PRE_SOWING":
                return profile.getRootDepthInitialCm();

            case "DEVELOPMENT":
            case "VEGETATIVE":
            case "TILLERING":
                return profile.getRootDepthMidCm();

            case "MID":
            case "MID_SEASON":
            case "FLOWERING":
            case "GRAIN_FILLING":
                return profile.getRootDepthMidCm();

            case "LATE":
            case "LATE_SEASON":
            case "MATURITY":
            case "HARVEST":
                return profile.getRootDepthEndCm();

            default:
                return profile.getRootDepthInitialCm();
        }
    }

    private String[] parseLocation(String location) {
        if (location == null || location.isBlank()) {
            return new String[]{null, null};
        }

        String[] parts = location.split(",");
        if (parts.length >= 2) {
            String district = parts[parts.length - 2].trim();
            String state = parts[parts.length - 1].trim();
            return new String[]{district, state};
        }

        return new String[]{null, parts[0].trim()};
    }

    private boolean scopeMatches(String profileValue, String requestedValue) {
        if (profileValue == null || profileValue.isBlank()) {
            return true;
        }
        return requestedValue != null
                && normalizeScope(profileValue).equals(normalizeScope(requestedValue));
    }

    private int profileMatchScore(
            CropWaterProfile profile,
            String stateName,
            String districtName,
            String season
    ) {
        int score = 0;
        if (equalsScope(profile.getStateName(), stateName)) score += 4;
        if (equalsScope(profile.getDistrictName(), districtName)) score += 4;
        if (equalsScope(profile.getSeason(), season)) score += 2;
        return score;
    }

    private boolean equalsScope(String a, String b) {
        return a != null && !a.isBlank() && b != null && !b.isBlank()
                && normalizeScope(a).equals(normalizeScope(b));
    }

    private String normalizeScope(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    /*
     * -------------------------------------------------------------
     * Determine data provenance.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            SmartIrrigationRequest request,
            IrrigationConfig config,
            IrrigationTelemetry telemetry,
            WeatherForecastData forecast,
            CropWaterProfile cropWaterProfile
    ) {

        boolean requestData = false;

        boolean testTelemetry =
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                );

        boolean testForecast =
                forecast != null
                        && (
                        "TEST".equalsIgnoreCase(
                                forecast.getDataStatus()
                        )
                                || "TEST_FORECAST".equalsIgnoreCase(
                                forecast.getSource()
                        )
                );

        boolean testConfiguration =
                config != null
                        && (
                        "TEST".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "ESTIMATED".equalsIgnoreCase(
                                config.getDataStatus()
                        )
                                || "TEST_CONFIGURATION".equalsIgnoreCase(
                                config.getSource()
                        )
                );

        /*
         * Any synthetic/test source makes the overall response TEST.
         */
        if (testTelemetry
                || testForecast
                || testConfiguration) {

            return "TEST";
        }

        /*
         * Explicit request + stored values together.
         */
        if (requestData
                && (telemetry != null
                || forecast != null
                || cropWaterProfile != null)) {

            return "MIXED";
        }

        /*
         * Real sensor data.
         */
        if (telemetry != null) {

            return "MEASURED";
        }

        /*
         * Forecast-only decision context.
         */
        if (forecast != null) {

            return "FORECAST";
        }

        /*
         * Configuration/reference-only context.
         */
        if (config != null
                || cropWaterProfile != null) {

            return "REFERENCE";
        }

        return "INSUFFICIENT_DATA";
    }

    /*
     * -------------------------------------------------------------
     * Build uncertainty message.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            IrrigationConfig config,
            IrrigationTelemetry telemetry,
            WeatherForecastData forecast,
            Double forecastConfidence,
            Double minimumConfidence,
            Double rootDepthCm
    ) {

        StringBuilder note =
                new StringBuilder();

        note.append(
                "This is rule-based irrigation decision support."
        );

        if (telemetry != null
                && (
                "TEST".equalsIgnoreCase(
                        telemetry.getSensorStatus()
                )
                        || "TEST_SENSOR".equalsIgnoreCase(
                        telemetry.getSource()
                )
        )) {

            note.append(
                    " Current telemetry is synthetic test data."
            );
        }

        if (forecast != null
                && (
                "TEST".equalsIgnoreCase(
                        forecast.getDataStatus()
                )
                        || "TEST_FORECAST".equalsIgnoreCase(
                        forecast.getSource()
                )
        )) {

            note.append(
                    " Weather forecast data is synthetic test data."
            );
        }

        if (minimumConfidence != null
                && forecastConfidence != null
                && forecastConfidence < minimumConfidence) {

            note.append(
                    " Forecast confidence is below the configured minimum."
            );
        }

        if (rootDepthCm == null) {

            note.append(
                    " Root-zone depth is not available in the current decision inputs."
            );
        }

        note.append(
                " Local soil calibration, crop stage, sensor quality and reliable weather forecasts should be validated before farm action."
        );

        return note.toString();
    }
}