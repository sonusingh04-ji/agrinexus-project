package com.agrinexus.service;

import com.agrinexus.dto.FertilizerScheduleRequest;
import com.agrinexus.dto.FertilizerScheduleResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FertilizerRecommendation;
import com.agrinexus.entity.FertilizerSchedule;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.FertilizerRecommendationRepository;
import com.agrinexus.repository.FertilizerScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FertilizerScheduleService {

    /*
     * =============================================================
     * AGRINEXUS FERTILIZER SCHEDULING
     * =============================================================
     *
     * Methodology:
     * AGRINEXUS-FERT-SCHEDULE-V2
     *
     * Main improvements:
     *
     * 1. Preserve missing nutrient values instead of converting
     *    them silently to zero.
     *
     * 2. Use linked fertilizer recommendation as the primary
     *    evidence source when available.
     *
     * 3. Apply a conservative weather screening gate.
     *
     * 4. HOLD the event when clearly unsuitable weather is supplied.
     *
     * 5. Preserve source/provenance.
     *
     * 6. Always provide an uncertainty statement.
     *
     * 7. Never invent fertilizer quantities.
     * =============================================================
     */
    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-FERT-SCHEDULE-V2";

    /*
     * Weather conditions under which a fertilizer application
     * should conservatively be held for reassessment.
     *
     * These are screening conditions only. They are not presented
     * as universal crop/product-specific application thresholds.
     */
    private static final List<String> HOLD_WEATHER_KEYWORDS =
            List.of(
                    "heavy rain",
                    "very heavy rain",
                    "extreme rain",
                    "flood",
                    "flooding",
                    "storm",
                    "severe storm",
                    "cyclone",
                    "hail",
                    "thunderstorm"
            );

    private final FertilizerScheduleRepository fertilizerScheduleRepository;

    private final FarmRepository farmRepository;

    private final CropRepository cropRepository;

    private final FertilizerRecommendationRepository
            fertilizerRecommendationRepository;

    /*
     * =============================================================
     * CREATE SCHEDULE
     * =============================================================
     */
    @Transactional
    public FertilizerScheduleResponse createSchedule(
            FertilizerScheduleRequest request
    ) {

        validateRequest(request);

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

        Crop crop =
                cropRepository.findById(
                                request.getCropId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop not found with ID: "
                                                + request.getCropId()
                                )
                        );

        /*
         * ---------------------------------------------------------
         * Linked fertilizer recommendation.
         * ---------------------------------------------------------
         */
        FertilizerRecommendation recommendation =
                findRecommendation(
                        request,
                        farm,
                        crop
                );

        /*
         * ---------------------------------------------------------
         * Nutrient values.
         *
         * Priority:
         *
         * 1. Explicitly supplied schedule values
         * 2. Existing recommendation values
         *
         * Missing values remain null.
         *
         * We DO NOT convert missing values into zero.
         * ---------------------------------------------------------
         */
        Double nitrogen =
                resolveNitrogen(
                        request
                );

        Double phosphorus =
                resolvePhosphorus(
                        request
                );

        Double potassium =
                resolvePotassium(
                        request
                );

        Double sulphur =
                safeNullable(
                        request.getSulphur()
                );

        Double zinc =
                safeNullable(
                        request.getZinc()
                );

        /*
         * ---------------------------------------------------------
         * Weather gate.
         * ---------------------------------------------------------
         */
        WeatherDecision weatherDecision =
                evaluateWeatherGate(
                        request.getWeatherCondition()
                );

        /*
         * ---------------------------------------------------------
         * Final status.
         * ---------------------------------------------------------
         */
        String finalStatus =
                determineStatus(
                        request.getStatus(),
                        weatherDecision
                );

        /*
         * ---------------------------------------------------------
         * Source.
         * ---------------------------------------------------------
         */
        String finalSource =
                determineSource(
                        request,
                        recommendation
                );

        /*
         * ---------------------------------------------------------
         * Uncertainty.
         * ---------------------------------------------------------
         */
        String finalUncertainty =
                buildUncertaintyNote(
                        request,
                        recommendation,
                        weatherDecision,
                        nitrogen,
                        phosphorus,
                        potassium
                );

        /*
         * ---------------------------------------------------------
         * Build schedule entity.
         * ---------------------------------------------------------
         */
        FertilizerSchedule schedule =
                FertilizerSchedule.builder()

                        .farm(
                                farm
                        )

                        .crop(
                                crop
                        )

                        /*
                         * Entity field is
                         * fertilizerRecommendation.
                         */
                        .fertilizerRecommendation(
                                recommendation
                        )

                        .stage(
                                request.getStage()
                        )

                        .plannedDate(
                                request.getPlannedDate()
                        )

                        .actionType(
                                request.getActionType()
                        )

                        .productName(
                                request.getProductName()
                        )

                        /*
                         * Preserve null as missing.
                         */
                        .nitrogen(
                                nitrogen
                        )

                        .phosphorus(
                                phosphorus
                        )

                        .potassium(
                                potassium
                        )

                        .sulphur(
                                sulphur
                        )

                        .zinc(
                                zinc
                        )

                        /*
                         * HOLD has priority over the supplied
                         * status when the weather screening gate
                         * identifies a clearly unsuitable condition.
                         */
                        .status(
                                finalStatus
                        )

                        .weatherCondition(
                                request.getWeatherCondition()
                        )

                        .reassessmentRequired(
                                determineReassessmentRequired(
                                        request,
                                        weatherDecision
                                )
                        )

                        .source(
                                finalSource
                        )

                        .uncertaintyNote(
                                finalUncertainty
                        )

                        .notes(
                                request.getNotes()
                        )

                        .reason(
                                buildReason(
                                        request,
                                        recommendation,
                                        weatherDecision,
                                        nitrogen,
                                        phosphorus,
                                        potassium
                                )
                        )

                        .recommendation(
                                buildRecommendation(
                                        request,
                                        weatherDecision,
                                        recommendation
                                )
                        )

                        .dataStatus(
                                determineDataStatus(
                                        request,
                                        recommendation,
                                        nitrogen,
                                        phosphorus,
                                        potassium
                                )
                        )

                        .build();

        FertilizerSchedule savedSchedule =
                fertilizerScheduleRepository.save(
                        schedule
                );

        return toResponse(
                savedSchedule
        );
    }

    /*
     * =============================================================
     * FIND LINKED RECOMMENDATION
     * =============================================================
     */
    private FertilizerRecommendation findRecommendation(
            FertilizerScheduleRequest request,
            Farm farm,
            Crop crop
    ) {

        if (request.getRecommendationId() == null) {

            return null;
        }

        FertilizerRecommendation recommendation =
                fertilizerRecommendationRepository
                        .findById(
                                request.getRecommendationId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer recommendation not found with ID: "
                                                + request.getRecommendationId()
                                )
                        );

        /*
         * Same-farm protection.
         */
        if (recommendation.getFarm() != null
                && !recommendation.getFarm()
                .getId()
                .equals(
                        farm.getId()
                )) {

            throw new RuntimeException(
                    "Recommendation does not belong to the selected farm"
            );
        }

        /*
         * Same-crop protection.
         */
        if (recommendation.getCrop() != null
                && !recommendation.getCrop()
                .getId()
                .equals(
                        crop.getId()
                )) {

            throw new RuntimeException(
                    "Recommendation does not belong to the selected crop"
            );
        }

        return recommendation;
    }

    /*
     * =============================================================
     * NITROGEN RESOLUTION
     * =============================================================
     *
     * We intentionally use the schedule request here because the
     * existing FertilizerRecommendation entity contract visible in
     * the current service only guarantees farm/crop/dataStatus and
     * does not require us to guess additional getter names.
     *
     * If the caller wants a numeric nutrient quantity in the
     * schedule, it must explicitly provide it.
     * =============================================================
     */
    private Double resolveNitrogen(
            FertilizerScheduleRequest request
    ) {

        return safeNullable(
                request.getNitrogen()
        );
    }

    /*
     * =============================================================
     * PHOSPHORUS RESOLUTION
     * =============================================================
     */
    private Double resolvePhosphorus(
            FertilizerScheduleRequest request
    ) {

        return safeNullable(
                request.getPhosphorus()
        );
    }

    /*
     * =============================================================
     * POTASSIUM RESOLUTION
     * =============================================================
     */
    private Double resolvePotassium(
            FertilizerScheduleRequest request
    ) {

        return safeNullable(
                request.getPotassium()
        );
    }

    /*
     * =============================================================
     * NULL-SAFE NUMERIC VALUE
     * =============================================================
     */
    private Double safeNullable(
            Double value
    ) {

        if (value == null) {

            return null;
        }

        if (value.isNaN()
                || value.isInfinite()) {

            throw new RuntimeException(
                    "Numeric fertilizer value must be finite"
            );
        }

        return value;
    }

    /*
     * =============================================================
     * WEATHER SAFETY GATE
     * =============================================================
     */
    private WeatherDecision evaluateWeatherGate(
            String weatherCondition
    ) {

        /*
         * No weather supplied.
         *
         * We cannot claim that weather is safe.
         * But we also cannot invent a weather hold.
         */
        if (weatherCondition == null
                || weatherCondition.isBlank()) {

            return new WeatherDecision(
                    "WEATHER_UNAVAILABLE",
                    false,
                    false,
                    "Weather condition was not supplied. Timing cannot be fully assessed from weather."
            );
        }

        String normalized =
                weatherCondition
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * ---------------------------------------------------------
         * Clearly unsuitable weather screen.
         * ---------------------------------------------------------
         */
        for (String keyword :
                HOLD_WEATHER_KEYWORDS) {

            if (normalized.contains(
                    keyword
            )) {

                return new WeatherDecision(
                        "HOLD",
                        true,
                        true,
                        "Weather screening identified an unsuitable condition: "
                                + weatherCondition
                                + ". Reassess before fertilizer application."
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * Possible unsuitable conditions that should trigger
         * reassessment rather than an automatic HOLD.
         * ---------------------------------------------------------
         */
        if (normalized.contains(
                "extreme heat"
        )
                || normalized.contains(
                "severe heat"
        )
                || normalized.contains(
                "high wind"
        )
                || normalized.contains(
                "strong wind"
        )) {

            return new WeatherDecision(
                    "REASSESS",
                    false,
                    true,
                    "Weather may affect application timing: "
                            + weatherCondition
                            + ". Reassess field and product-specific guidance before application."
            );
        }

        /*
         * ---------------------------------------------------------
         * No weather gate triggered.
         * ---------------------------------------------------------
         */
        return new WeatherDecision(
                "PASS",
                false,
                false,
                "No configured weather screening hold condition was detected from the supplied weather description."
        );
    }

    /*
     * =============================================================
     * FINAL STATUS
     * =============================================================
     */
    private String determineStatus(
            String requestedStatus,
            WeatherDecision weatherDecision
    ) {

        /*
         * Weather HOLD always takes priority.
         */
        if (weatherDecision.hold()) {

            return "HOLD";
        }

        if (requestedStatus != null
                && !requestedStatus.isBlank()) {

            return requestedStatus;
        }

        return "PLANNED";
    }

    /*
     * =============================================================
     * REASSESSMENT FLAG
     * =============================================================
     */
    private Boolean determineReassessmentRequired(
            FertilizerScheduleRequest request,
            WeatherDecision weatherDecision
    ) {

        if (weatherDecision.reassessmentRequired()) {

            return true;
        }

        if (request.getReassessmentRequired() != null) {

            return request.getReassessmentRequired();
        }

        return true;
    }

    /*
     * =============================================================
     * SOURCE / PROVENANCE
     * =============================================================
     */
    private String determineSource(
            FertilizerScheduleRequest request,
            FertilizerRecommendation recommendation
    ) {

        if (request.getSource() != null
                && !request.getSource().isBlank()) {

            return request.getSource();
        }

        if (recommendation != null) {

            /*
             * The recommendation itself may have provenance,
             * but the current service contract does not rely on
             * a guessed getter.
             */
            return "LINKED_FERTILIZER_RECOMMENDATION";
        }

        return "FERTILIZER_SCHEDULE_INPUT";
    }

    /*
     * =============================================================
     * EXPLAINABLE REASON
     * =============================================================
     */
    private String buildReason(
            FertilizerScheduleRequest request,
            FertilizerRecommendation recommendation,
            WeatherDecision weatherDecision,
            Double nitrogen,
            Double phosphorus,
            Double potassium
    ) {

        String stage =
                request.getStage();

        String date =
                request.getPlannedDate() != null
                        ? request.getPlannedDate()
                        .toString()
                        : "not specified";

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                "Schedule evaluation for stage "
        );

        reason.append(
                stage
        );

        reason.append(
                " on "
        );

        reason.append(
                date
        );

        reason.append(
                ". "
        );

        /*
         * Recommendation linkage.
         */
        if (recommendation != null) {

            reason.append(
                    "A fertilizer recommendation is linked to this schedule. "
            );

        } else {

            reason.append(
                    "No fertilizer recommendation record is linked. "
            );
        }

        /*
         * Nutrient evidence.
         */
        int nutrientValues =
                countNonNull(
                        nitrogen,
                        phosphorus,
                        potassium
                );

        reason.append(
                "Explicit schedule nutrient values available="
        );

        reason.append(
                nutrientValues
        );

        reason.append(
                " of 3. "
        );

        /*
         * Weather.
         */
        reason.append(
                weatherDecision.message()
        );

        return reason.toString();
    }

    /*
     * =============================================================
     * FARMER-FACING RECOMMENDATION
     * =============================================================
     */
    private String buildRecommendation(
            FertilizerScheduleRequest request,
            WeatherDecision weatherDecision,
            FertilizerRecommendation recommendation
    ) {

        /*
         * ---------------------------------------------------------
         * HOLD
         * ---------------------------------------------------------
         */
        if (weatherDecision.hold()) {

            return
                    "Hold the scheduled fertilizer activity until the weather condition is reassessed and the applicable crop/product guidance confirms a suitable application window.";
        }

        /*
         * ---------------------------------------------------------
         * REASSESS
         * ---------------------------------------------------------
         */
        if (weatherDecision.reassessmentRequired()
                && "REASSESS".equals(
                weatherDecision.status()
        )) {

            return
                    "Reassess weather, field conditions and product-specific agronomic guidance before carrying out the scheduled activity.";
        }

        /*
         * ---------------------------------------------------------
         * Missing nutrient quantities.
         * ---------------------------------------------------------
         */
        if (countNonNull(
                request.getNitrogen(),
                request.getPhosphorus(),
                request.getPotassium()
        ) == 0) {

            if (recommendation != null) {

                return
                        "A fertilizer recommendation is linked, but this schedule does not contain explicit nutrient quantities. Do not interpret missing values as zero; verify the approved recommendation and applicable product rate before application.";
            }

            return
                    "No nutrient quantities are available for this schedule. Verify the approved fertilizer recommendation before application.";
        }

        /*
         * ---------------------------------------------------------
         * Reassessment requested by caller.
         * ---------------------------------------------------------
         */
        if (request.getReassessmentRequired() != null
                && request.getReassessmentRequired()) {

            return
                    "Review soil, crop and weather conditions before carrying out the scheduled fertilizer activity.";
        }

        /*
         * ---------------------------------------------------------
         * Normal planned event.
         * ---------------------------------------------------------
         */
        return
                "Follow the scheduled activity at the planned crop stage, subject to the linked recommendation and applicable local agronomic guidance.";
    }

    /*
     * =============================================================
     * DATA STATUS
     * =============================================================
     */
    private String determineDataStatus(
            FertilizerScheduleRequest request,
            FertilizerRecommendation recommendation,
            Double nitrogen,
            Double phosphorus,
            Double potassium
    ) {

        boolean hasExplicitNutrientData =
                countNonNull(
                        nitrogen,
                        phosphorus,
                        potassium
                ) > 0;

        /*
         * Explicit request values plus linked recommendation.
         */
        if (recommendation != null
                && hasExplicitNutrientData) {

            /*
             * The recommendation can already be estimated while
             * the schedule adds calculated/planned timing.
             */
            if (recommendation.getDataStatus() != null
                    && !recommendation.getDataStatus().isBlank()
                    && !"MEASURED".equalsIgnoreCase(
                    recommendation.getDataStatus()
            )) {

                return "ESTIMATED";
            }

            return "MIXED";
        }

        /*
         * Explicit schedule values without recommendation.
         */
        if (hasExplicitNutrientData) {

            return "CALCULATED";
        }

        /*
         * Linked recommendation but no explicit schedule quantities.
         */
        if (recommendation != null) {

            return recommendation.getDataStatus() != null
                    && !recommendation.getDataStatus().isBlank()
                    ? recommendation.getDataStatus()
                    : "ESTIMATED";
        }

        return "INSUFFICIENT_DATA";
    }

    /*
     * =============================================================
     * UNCERTAINTY NOTE
     * =============================================================
     */
    private String buildUncertaintyNote(
            FertilizerScheduleRequest request,
            FertilizerRecommendation recommendation,
            WeatherDecision weatherDecision,
            Double nitrogen,
            Double phosphorus,
            Double potassium
    ) {

        List<String> notes =
                new ArrayList<>();

        notes.add(
                "This fertilizer schedule is decision-support information and not a guaranteed prescription."
        );

        /*
         * Nutrient quantities.
         */
        if (countNonNull(
                nitrogen,
                phosphorus,
                potassium
        ) < 3) {

            notes.add(
                    "One or more major nutrient quantities are missing from the schedule and must not be interpreted as zero."
            );
        }

        /*
         * Recommendation.
         */
        if (recommendation == null) {

            notes.add(
                    "No linked fertilizer recommendation is available."
            );
        } else {

            notes.add(
                    "A fertilizer recommendation record is linked to this schedule."
            );
        }

        /*
         * Weather.
         */
        if ("WEATHER_UNAVAILABLE".equals(
                weatherDecision.status()
        )) {

            notes.add(
                    "Weather information was not supplied, so weather-based timing cannot be fully assessed."
            );
        }

        if ("HOLD".equals(
                weatherDecision.status()
        )) {

            notes.add(
                    "The event was placed on HOLD by the configured conservative weather screening gate."
            );
        }

        if ("REASSESS".equals(
                weatherDecision.status()
        )) {

            notes.add(
                    "The supplied weather may affect application timing and requires reassessment."
            );
        }

        /*
         * Provenance.
         */
        if (request.getSource() == null
                || request.getSource().isBlank()) {

            notes.add(
                    "A schedule-specific source was not supplied; the record is labelled as a linked recommendation or schedule input."
            );
        }

        notes.add(
                "Fertilizer quantities, product selection and application timing should be confirmed against the applicable validated crop/product guidance and latest soil information."
        );

        return String.join(
                " ",
                notes
        );
    }

    /*
     * =============================================================
     * RESPONSE CONVERSION
     * =============================================================
     */
    private FertilizerScheduleResponse toResponse(
            FertilizerSchedule schedule
    ) {

        return FertilizerScheduleResponse.builder()

                .id(
                        schedule.getId()
                )

                .farmId(
                        schedule.getFarm() != null
                                ? schedule.getFarm().getId()
                                : null
                )

                .cropId(
                        schedule.getCrop() != null
                                ? schedule.getCrop().getId()
                                : null
                )

                .cropName(
                        schedule.getCrop() != null
                                ? schedule.getCrop().getName()
                                : null
                )

                /*
                 * Linked recommendation.
                 */
                .recommendationId(
                        schedule.getFertilizerRecommendation() != null
                                ? schedule
                                .getFertilizerRecommendation()
                                .getId()
                                : null
                )

                .stage(
                        schedule.getStage()
                )

                .plannedDate(
                        schedule.getPlannedDate()
                )

                .actionType(
                        schedule.getActionType()
                )

                .productName(
                        schedule.getProductName()
                )

                .nitrogen(
                        schedule.getNitrogen()
                )

                .phosphorus(
                        schedule.getPhosphorus()
                )

                .potassium(
                        schedule.getPotassium()
                )

                .sulphur(
                        schedule.getSulphur()
                )

                .zinc(
                        schedule.getZinc()
                )

                .status(
                        schedule.getStatus()
                )

                .weatherCondition(
                        schedule.getWeatherCondition()
                )

                .reassessmentRequired(
                        schedule.getReassessmentRequired()
                )

                .source(
                        schedule.getSource()
                )

                .dataStatus(
                        schedule.getDataStatus()
                )

                .reason(
                        schedule.getReason()
                )

                .recommendation(
                        schedule.getRecommendation()
                )

                .uncertaintyNote(
                        schedule.getUncertaintyNote()
                )

                .notes(
                        schedule.getNotes()
                )

                .build();
    }

    /*
     * =============================================================
     * GET BY ID
     * =============================================================
     */
    public FertilizerScheduleResponse getScheduleById(
            UUID scheduleId
    ) {

        FertilizerSchedule schedule =
                fertilizerScheduleRepository
                        .findById(
                                scheduleId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer schedule not found with ID: "
                                                + scheduleId
                                )
                        );

        return toResponse(
                schedule
        );
    }

    /*
     * =============================================================
     * GET FARM SCHEDULES
     * =============================================================
     */
    public List<FertilizerScheduleResponse> getFarmSchedules(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        return fertilizerScheduleRepository
                .findByFarmIdOrderByPlannedDateAsc(
                        farmId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * GET CROP SCHEDULES
     * =============================================================
     */
    public List<FertilizerScheduleResponse> getCropSchedules(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );

        return fertilizerScheduleRepository
                .findByFarmIdAndCropIdOrderByPlannedDateAsc(
                        farmId,
                        cropId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * GET BY STATUS
     * =============================================================
     */
    public List<FertilizerScheduleResponse> getSchedulesByStatus(
            UUID farmId,
            String status
    ) {

        verifyFarmExists(
                farmId
        );

        if (status == null
                || status.isBlank()) {

            throw new RuntimeException(
                    "Schedule status is required"
            );
        }

        return fertilizerScheduleRepository
                .findByFarmIdAndStatusIgnoreCaseOrderByPlannedDateAsc(
                        farmId,
                        status
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * GET DATE RANGE
     * =============================================================
     */
    public List<FertilizerScheduleResponse> getSchedulesByDateRange(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        verifyFarmExists(
                farmId
        );

        if (startDate == null
                || endDate == null) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(
                endDate
        )) {

            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        return fertilizerScheduleRepository
                .findByFarmIdAndPlannedDateBetweenOrderByPlannedDateAsc(
                        farmId,
                        startDate,
                        endDate
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * GET BY STAGE
     * =============================================================
     */
    public List<FertilizerScheduleResponse> getSchedulesByStage(
            UUID farmId,
            UUID cropId,
            String stage
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );

        if (stage == null
                || stage.isBlank()) {

            throw new RuntimeException(
                    "Stage is required"
            );
        }

        return fertilizerScheduleRepository
                .findByFarmIdAndCropIdAndStageIgnoreCaseOrderByPlannedDateAsc(
                        farmId,
                        cropId,
                        stage
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * LATEST SCHEDULE
     * =============================================================
     */
    public FertilizerScheduleResponse getLatestSchedule(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        FertilizerSchedule schedule =
                fertilizerScheduleRepository
                        .findFirstByFarmIdOrderByPlannedDateDesc(
                                farmId
                        );

        if (schedule == null) {

            return null;
        }

        return toResponse(
                schedule
        );
    }

    /*
     * =============================================================
     * NEXT SCHEDULE
     * =============================================================
     */
    public FertilizerScheduleResponse getNextSchedule(
            UUID farmId,
            LocalDate date
    ) {

        verifyFarmExists(
                farmId
        );

        LocalDate effectiveDate =
                date != null
                        ? date
                        : LocalDate.now();

        FertilizerSchedule schedule =
                fertilizerScheduleRepository
                        .findFirstByFarmIdAndPlannedDateGreaterThanEqualOrderByPlannedDateAsc(
                                farmId,
                                effectiveDate
                        );

        if (schedule == null) {

            return null;
        }

        return toResponse(
                schedule
        );
    }

    /*
     * =============================================================
     * REASSESSMENT SCHEDULES
     * =============================================================
     */
    public List<FertilizerScheduleResponse>
    getReassessmentSchedules(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        return fertilizerScheduleRepository
                .findByFarmIdAndReassessmentRequiredTrueOrderByPlannedDateAsc(
                        farmId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }

    /*
     * =============================================================
     * UPDATE
     * =============================================================
     */
    @Transactional
    public FertilizerScheduleResponse updateSchedule(
            UUID scheduleId,
            FertilizerScheduleRequest request
    ) {

        validateRequest(
                request
        );

        FertilizerSchedule schedule =
                fertilizerScheduleRepository
                        .findById(
                                scheduleId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer schedule not found with ID: "
                                                + scheduleId
                                )
                        );

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

        Crop crop =
                cropRepository.findById(
                                request.getCropId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop not found with ID: "
                                                + request.getCropId()
                                )
                        );

        FertilizerRecommendation recommendation =
                findRecommendation(
                        request,
                        farm,
                        crop
                );

        Double nitrogen =
                resolveNitrogen(
                        request
                );

        Double phosphorus =
                resolvePhosphorus(
                        request
                );

        Double potassium =
                resolvePotassium(
                        request
                );

        Double sulphur =
                safeNullable(
                        request.getSulphur()
                );

        Double zinc =
                safeNullable(
                        request.getZinc()
                );

        WeatherDecision weatherDecision =
                evaluateWeatherGate(
                        request.getWeatherCondition()
                );

        String finalStatus =
                determineStatus(
                        request.getStatus(),
                        weatherDecision
                );

        String finalSource =
                determineSource(
                        request,
                        recommendation
                );

        String finalUncertainty =
                buildUncertaintyNote(
                        request,
                        recommendation,
                        weatherDecision,
                        nitrogen,
                        phosphorus,
                        potassium
                );

        schedule.setFarm(
                farm
        );

        schedule.setCrop(
                crop
        );

        schedule.setFertilizerRecommendation(
                recommendation
        );

        schedule.setStage(
                request.getStage()
        );

        schedule.setPlannedDate(
                request.getPlannedDate()
        );

        schedule.setActionType(
                request.getActionType()
        );

        schedule.setProductName(
                request.getProductName()
        );

        schedule.setNitrogen(
                nitrogen
        );

        schedule.setPhosphorus(
                phosphorus
        );

        schedule.setPotassium(
                potassium
        );

        schedule.setSulphur(
                sulphur
        );

        schedule.setZinc(
                zinc
        );

        schedule.setStatus(
                finalStatus
        );

        schedule.setWeatherCondition(
                request.getWeatherCondition()
        );

        schedule.setReassessmentRequired(
                determineReassessmentRequired(
                        request,
                        weatherDecision
                )
        );

        schedule.setSource(
                finalSource
        );

        schedule.setUncertaintyNote(
                finalUncertainty
        );

        schedule.setNotes(
                request.getNotes()
        );

        schedule.setReason(
                buildReason(
                        request,
                        recommendation,
                        weatherDecision,
                        nitrogen,
                        phosphorus,
                        potassium
                )
        );

        schedule.setRecommendation(
                buildRecommendation(
                        request,
                        weatherDecision,
                        recommendation
                )
        );

        schedule.setDataStatus(
                determineDataStatus(
                        request,
                        recommendation,
                        nitrogen,
                        phosphorus,
                        potassium
                )
        );

        FertilizerSchedule updated =
                fertilizerScheduleRepository.save(
                        schedule
                );

        return toResponse(
                updated
        );
    }

    /*
     * =============================================================
     * DELETE
     * =============================================================
     */
    public void deleteSchedule(
            UUID scheduleId
    ) {

        if (!fertilizerScheduleRepository.existsById(
                scheduleId
        )) {

            throw new RuntimeException(
                    "Fertilizer schedule not found with ID: "
                            + scheduleId
            );
        }

        fertilizerScheduleRepository.deleteById(
                scheduleId
        );
    }

    /*
     * =============================================================
     * VALIDATION
     * =============================================================
     */
    private void validateRequest(
            FertilizerScheduleRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }

        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getCropId() == null) {

            throw new RuntimeException(
                    "Crop ID is required"
            );
        }

        if (request.getStage() == null
                || request.getStage().isBlank()) {

            throw new RuntimeException(
                    "Stage is required"
            );
        }

        if (request.getPlannedDate() == null) {

            throw new RuntimeException(
                    "Planned date is required"
            );
        }

        if (request.getActionType() == null
                || request.getActionType().isBlank()) {

            throw new RuntimeException(
                    "Action type is required"
            );
        }

        /*
         * Validate numeric values only when explicitly supplied.
         */
        validateNonNegative(
                request.getNitrogen(),
                "Nitrogen"
        );

        validateNonNegative(
                request.getPhosphorus(),
                "Phosphorus"
        );

        validateNonNegative(
                request.getPotassium(),
                "Potassium"
        );

        validateNonNegative(
                request.getSulphur(),
                "Sulphur"
        );

        validateNonNegative(
                request.getZinc(),
                "Zinc"
        );
    }

    /*
     * =============================================================
     * NON-NEGATIVE VALIDATION
     * =============================================================
     */
    private void validateNonNegative(
            Double value,
            String fieldName
    ) {

        if (value == null) {

            return;
        }

        if (value.isNaN()
                || value.isInfinite()) {

            throw new RuntimeException(
                    fieldName
                            + " must be a finite number"
            );
        }

        if (value < 0) {

            throw new RuntimeException(
                    fieldName
                            + " cannot be negative"
            );
        }
    }

    /*
     * =============================================================
     * COUNT NON-NULL
     * =============================================================
     */
    private int countNonNull(
            Double... values
    ) {

        int count =
                0;

        for (Double value : values) {

            if (value != null) {

                count++;
            }
        }

        return count;
    }

    /*
     * =============================================================
     * FARM VALIDATION
     * =============================================================
     */
    private void verifyFarmExists(
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
     * =============================================================
     * CROP VALIDATION
     * =============================================================
     */
    private void verifyCropExists(
            UUID cropId
    ) {

        if (!cropRepository.existsById(
                cropId
        )) {

            throw new RuntimeException(
                    "Crop not found with ID: "
                            + cropId
            );
        }
    }

    /*
     * =============================================================
     * WEATHER DECISION RECORD
     * =============================================================
     */
    private record WeatherDecision(
            String status,
            boolean hold,
            boolean reassessmentRequired,
            String message
    ) {
    }
}