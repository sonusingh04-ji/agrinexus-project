package com.agrinexus.service;

import com.agrinexus.dto.*;
import com.agrinexus.entity.*;
import com.agrinexus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIContextSnapshotService {

    private final AIContextSnapshotRepository contextRepository;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;

    private final FarmRepository farmRepository;
    private final SoilSampleRepository soilSampleRepository;
    private final CropHistoryRepository cropHistoryRepository;
    private final CropRepository cropRepository;
    private final WeatherRecordRepository weatherRecordRepository;
    private final WeatherForecastDataRepository weatherForecastDataRepository;
    private final IrrigationTelemetryRepository irrigationTelemetryRepository;
    private final FertilizerRecommendationRepository fertilizerRecommendationRepository;
    private final NutrientBalanceRepository nutrientBalanceRepository;
    private final WeatherRiskRepository weatherRiskRepository;
    private final DiseasePestRiskRepository diseasePestRiskRepository;
    private final SatelliteObservationRepository satelliteObservationRepository;

    /*
     * Member 6
     */
    private final FieldHealthPassportService fieldHealthPassportService;
    private final SoilTestTrendService soilTestTrendService;
    private final LowestCostSafePlanService lowestCostSafePlanService;
    private final VillageCooperativeService villageCooperativeService;

    private final VillageCooperativeMemberRepository
            villageCooperativeMemberRepository;

    /*
     * Existing Member 2 irrigation decision engine.
     *
     * Member 5 consumes the decision produced by Member 2
     * instead of recreating irrigation logic.
     */
    private final SmartIrrigationService smartIrrigationService;


    /*
     * =============================================================
     * Create AI context snapshot.
     * =============================================================
     */
    public AIContextResponse create(
            AIContextRequest request
    ) {

        validate(request);

        UUID farmId =
                request.getFarmId();

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
         * Optional conversation.
         * ---------------------------------------------------------
         */
        AIConversation conversation = null;

        if (request.getConversationId() != null) {

            conversation =
                    conversationRepository.findById(
                            request.getConversationId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI conversation not found with ID: "
                                            + request.getConversationId()
                            )
                    );

            if (conversation.getFarm() != null
                    && !farmId.equals(
                    conversation.getFarm().getId()
            )) {

                throw new RuntimeException(
                        "Conversation does not belong to the specified farm"
                );
            }
        }


        /*
         * ---------------------------------------------------------
         * Optional message.
         * ---------------------------------------------------------
         */
        AIMessage message = null;

        if (request.getMessageId() != null) {

            if (conversation == null) {

                throw new RuntimeException(
                        "Conversation ID is required when message ID is provided"
                );
            }

            message =
                    messageRepository.findById(
                            request.getMessageId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI message not found with ID: "
                                            + request.getMessageId()
                            )
                    );

            if (message.getConversation() == null
                    || !conversation.getId().equals(
                    message.getConversation().getId()
            )) {

                throw new RuntimeException(
                        "Message does not belong to the specified conversation"
                );
            }
        }


        /*
         * ---------------------------------------------------------
         * Latest soil sample.
         * ---------------------------------------------------------
         */
        SoilSample latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );


        /*
         * ---------------------------------------------------------
         * Latest crop history.
         * ---------------------------------------------------------
         */
        CropHistory latestCropHistory =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );


        /*
         * ---------------------------------------------------------
         * Latest / associated crop.
         * ---------------------------------------------------------
         */
        Crop latestCrop = null;

        if (latestCropHistory != null) {

            latestCrop =
                    latestCropHistory.getCrop();
        }


        /*
         * ---------------------------------------------------------
         * Latest weather.
         * ---------------------------------------------------------
         */
        WeatherRecord latestWeather =
                weatherRecordRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * ---------------------------------------------------------
         * Latest upcoming forecast.
         * ---------------------------------------------------------
         */
        WeatherForecastData latestForecast =
                weatherForecastDataRepository
                        .findFirstByFarmIdAndForecastDateGreaterThanEqualOrderByForecastDateAsc(
                                farmId,
                                LocalDate.now()
                        );


        /*
         * ---------------------------------------------------------
         * Latest irrigation telemetry.
         * ---------------------------------------------------------
         */
        IrrigationTelemetry latestTelemetry =
                irrigationTelemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );


        /*
         * ---------------------------------------------------------
         * Latest fertilizer recommendation.
         * ---------------------------------------------------------
         */
        FertilizerRecommendation latestFertilizer =
                fertilizerRecommendationRepository
                        .findFirstByFarmIdOrderByGeneratedAtDesc(
                                farmId
                        );


        /*
         * ---------------------------------------------------------
         * Latest nutrient balance.
         * ---------------------------------------------------------
         */
        NutrientBalance latestNutrientBalance =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                farmId
                        );


        /*
         * ---------------------------------------------------------
         * Latest weather risk.
         * ---------------------------------------------------------
         */
        WeatherRisk latestWeatherRisk =
                weatherRiskRepository.findAll()
                        .stream()
                        .filter(risk ->
                                risk.getFarm() != null
                                        && farmId.equals(
                                        risk.getFarm().getId()
                                )
                        )
                        .max(
                                Comparator.comparing(
                                        WeatherRisk::getDetectedAt
                                )
                        )
                        .orElse(null);


        /*
         * ---------------------------------------------------------
         * Latest disease/pest risk.
         * ---------------------------------------------------------
         */
        DiseasePestRisk latestDiseasePestRisk =
                diseasePestRiskRepository
                        .findByFarmIdOrderByEvaluatedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * ---------------------------------------------------------
         * Latest satellite observation.
         * ---------------------------------------------------------
         */
        SatelliteObservation latestSatelliteObservation =
                satelliteObservationRepository
                        .findFirstByFarmIdOrderByObservedAtDesc(
                                farmId
                        )
                        .orElse(null);


        /*
         * =========================================================
         * MEMBER 6 — FIELD HEALTH PASSPORT
         * =========================================================
         */
        FieldHealthPassportResponse fieldHealthPassport =
                fieldHealthPassportService.getPassport(
                        farmId
                );


        /*
         * =========================================================
         * MEMBER 6 — SOIL TEST TREND
         * =========================================================
         */
        SoilTestTrendResponse soilTestTrend =
                soilTestTrendService.getSoilTestTrend(
                        farmId
                );


        /*
         * =========================================================
         * MEMBER 6 — LOWEST-COST SAFE PLAN
         * =========================================================
         *
         * Evaluate only when the caller provides a target season.
         */
        LowestCostSafePlanResponse lowestCostPlan = null;

        if (
                request.getTargetSeason() != null
                        && !request.getTargetSeason().isBlank()
        ) {

            LowestCostSafePlanRequest lowestCostRequest =
                    LowestCostSafePlanRequest.builder()

                            .farmId(
                                    farmId
                            )

                            .targetSeason(
                                    request.getTargetSeason()
                            )

                            .farmerConstraint(
                                    request.getFarmerConstraint()
                            )

                            .requireSoilCompatibility(
                                    request.getRequireSoilCompatibility()
                            )

                            .availableWaterMm(
                                    request.getAvailableWaterMm()
                            )

                            .build();


            lowestCostPlan =
                    lowestCostSafePlanService.evaluate(
                            lowestCostRequest
                    );
        }


        /*
         * =========================================================
         * MEMBER 6 — VILLAGE COOPERATIVE
         * =========================================================
         *
         * Farm -> farmerId -> active cooperative membership.
         *
         * We do not invent a cooperative relationship.
         */
        CooperativeSummaryResponse cooperativeSummary =
                null;

        if (farm.getFarmerId() != null) {

            VillageCooperativeMember membership =
                    villageCooperativeMemberRepository
                            .findByFarmerId(
                                    farm.getFarmerId()
                            )
                            .stream()
                            .filter(member ->
                                    "ACTIVE".equalsIgnoreCase(
                                            member.getStatus()
                                    )
                            )
                            .findFirst()
                            .orElse(null);


            if (
                    membership != null
                            && membership.getCooperative() != null
            ) {

                cooperativeSummary =
                        villageCooperativeService.getSummary(
                                membership
                                        .getCooperative()
                                        .getId()
                        );
            }
        }


        /*
         * =========================================================
         * MEMBER 2 — REAL SMART IRRIGATION DECISION
         * =========================================================
         */
        SmartIrrigationResponse irrigationResponse =
                buildSmartIrrigationResponse(
                        farmId,
                        latestCropHistory,
                        latestTelemetry
                );


        /*
         * =========================================================
         * Existing Member 1–5 decision summaries
         * =========================================================
         */
        String irrigationDecision =
                buildIrrigationDecision(
                        irrigationResponse
                );


        String cropDecision =
                buildCropDecision(
                        latestCropHistory
                );


        String nutrientDecision =
                buildNutrientDecision(
                        latestNutrientBalance
                );


        String weatherRiskDecision =
                buildWeatherRiskDecision(
                        latestWeatherRisk
                );


        String diseasePestRiskDecision =
                buildDiseasePestDecision(
                        latestDiseasePestRisk
                );


        String satelliteDecision =
                buildSatelliteDecision(
                        latestSatelliteObservation
                );


        String fieldOperationDecision =
                buildFieldOperationDecision(
                        latestWeather,
                        latestTelemetry
                );


        /*
         * =========================================================
         * Member 6 decision summaries
         * =========================================================
         */
        String fieldHealthPassportDecision =
                buildFieldHealthPassportDecision(
                        fieldHealthPassport
                );


        String soilTestTrendDecision =
                buildSoilTestTrendDecision(
                        soilTestTrend
                );


        String lowestCostPlanDecision =
                buildLowestCostPlanDecision(
                        lowestCostPlan
                );


        String cooperativeDecision =
                buildCooperativeDecision(
                        cooperativeSummary
                );


        /*
         * ---------------------------------------------------------
         * Overall decision summary.
         * ---------------------------------------------------------
         */
        String decisionSummary =
                buildDecisionSummary(
                        irrigationDecision,
                        cropDecision,
                        nutrientDecision,
                        weatherRiskDecision,
                        diseasePestRiskDecision,
                        satelliteDecision,
                        fieldOperationDecision,
                        fieldHealthPassportDecision,
                        soilTestTrendDecision,
                        lowestCostPlanDecision,
                        cooperativeDecision
                );


        /*
         * ---------------------------------------------------------
         * Complete farm context.
         * ---------------------------------------------------------
         */
        String farmContext =
                buildFarmContext(
                        farm,
                        latestSoil,
                        latestCropHistory,
                        latestCrop,
                        latestWeather,
                        latestForecast,
                        latestTelemetry,
                        latestFertilizer,
                        latestNutrientBalance,
                        latestWeatherRisk,
                        latestDiseasePestRisk,
                        latestSatelliteObservation,
                        irrigationResponse,
                        fieldHealthPassport,
                        soilTestTrend,
                        lowestCostPlan,
                        cooperativeSummary
                );


        /*
         * ---------------------------------------------------------
         * Uncertainty.
         * ---------------------------------------------------------
         */
        String uncertainty =
                buildUncertaintyContext(
                        latestSoil,
                        latestWeather,
                        latestForecast,
                        latestTelemetry,
                        latestFertilizer,
                        latestNutrientBalance,
                        latestWeatherRisk,
                        latestDiseasePestRisk,
                        latestSatelliteObservation,
                        irrigationResponse,
                        fieldHealthPassport,
                        soilTestTrend,
                        lowestCostPlan,
                        cooperativeSummary
                );


        /*
         * ---------------------------------------------------------
         * Data provenance.
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        latestSoil,
                        latestWeather,
                        latestForecast,
                        latestTelemetry,
                        latestFertilizer,
                        latestNutrientBalance,
                        latestWeatherRisk,
                        latestDiseasePestRisk,
                        latestSatelliteObservation,
                        irrigationResponse,
                        fieldHealthPassport,
                        soilTestTrend,
                        lowestCostPlan,
                        cooperativeSummary
                );


        /*
         * ---------------------------------------------------------
         * Create immutable context snapshot.
         * ---------------------------------------------------------
         */
        AIContextSnapshot snapshot =
                AIContextSnapshot.builder()

                        .conversation(
                                conversation
                        )

                        .message(
                                message
                        )

                        .farm(
                                farm
                        )

                        .soilSampleId(
                                latestSoil != null
                                        ? latestSoil.getId()
                                        : null
                        )

                        .cropHistoryId(
                                latestCropHistory != null
                                        ? latestCropHistory.getId()
                                        : null
                        )

                        .cropId(
                                latestCrop != null
                                        ? latestCrop.getId()
                                        : null
                        )

                        .weatherRecordId(
                                latestWeather != null
                                        ? latestWeather.getId()
                                        : null
                        )

                        .weatherForecastId(
                                latestForecast != null
                                        ? latestForecast.getId()
                                        : null
                        )

                        .irrigationTelemetryId(
                                latestTelemetry != null
                                        ? latestTelemetry.getId()
                                        : null
                        )

                        .fertilizerRecommendationId(
                                latestFertilizer != null
                                        ? latestFertilizer.getId()
                                        : null
                        )

                        .nutrientBalanceId(
                                latestNutrientBalance != null
                                        ? latestNutrientBalance.getId()
                                        : null
                        )

                        .weatherRiskId(
                                latestWeatherRisk != null
                                        ? latestWeatherRisk.getId()
                                        : null
                        )

                        .diseasePestRiskId(
                                latestDiseasePestRisk != null
                                        ? latestDiseasePestRisk.getId()
                                        : null
                        )

                        .satelliteObservationId(
                                latestSatelliteObservation != null
                                        ? latestSatelliteObservation.getId()
                                        : null
                        )

                        .decisionSummary(
                                decisionSummary
                        )

                        .irrigationDecision(
                                irrigationDecision
                        )

                        .cropDecision(
                                cropDecision
                        )

                        .nutrientDecision(
                                nutrientDecision
                        )

                        .weatherRiskDecision(
                                weatherRiskDecision
                        )

                        .diseasePestRiskDecision(
                                diseasePestRiskDecision
                        )

                        .satelliteDecision(
                                satelliteDecision
                        )

                        .fieldOperationDecision(
                                fieldOperationDecision
                        )

                        /*
                         * Member 6
                         */
                        .fieldHealthPassportDecision(
                                fieldHealthPassportDecision
                        )

                        .soilTestTrendDecision(
                                soilTestTrendDecision
                        )

                        .lowestCostPlanDecision(
                                lowestCostPlanDecision
                        )

                        .cooperativeDecision(
                                cooperativeDecision
                        )

                        .farmContext(
                                farmContext
                        )

                        .uncertaintyContext(
                                uncertainty
                        )

                        .dataStatus(
                                dataStatus
                        )

                        .capturedAt(
                                LocalDateTime.now()
                        )

                        .build();


        return toResponse(
                contextRepository.save(
                        snapshot
                )
        );
    }


    /*
     * =============================================================
     * Smart Irrigation
     * =============================================================
     */
    private SmartIrrigationResponse buildSmartIrrigationResponse(
            UUID farmId,
            CropHistory cropHistory,
            IrrigationTelemetry telemetry
    ) {

        SmartIrrigationRequest request =
                SmartIrrigationRequest.builder()

                        .farmId(
                                farmId
                        )

                        .cropId(
                                cropHistory != null
                                        && cropHistory.getCrop() != null
                                        ? cropHistory
                                        .getCrop()
                                        .getId()
                                        : null
                        )

                        .cropStage(
                                telemetry != null
                                        ? telemetry.getCropStage()
                                        : null
                        )

                        .build();


        return smartIrrigationService.evaluate(
                request
        );
    }


    /*
     * =============================================================
     * Get snapshot by ID.
     * =============================================================
     */
    public AIContextResponse getById(
            UUID id
    ) {

        AIContextSnapshot snapshot =
                contextRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "AI context snapshot not found with ID: "
                                                + id
                                )
                        );


        return toResponse(
                snapshot
        );
    }


    /*
     * =============================================================
     * Get latest context for a farm.
     * =============================================================
     */
    public AIContextResponse getLatestByFarm(
            UUID farmId
    ) {

        verifyFarm(
                farmId
        );


        AIContextSnapshot snapshot =
                contextRepository.findAll()
                        .stream()
                        .filter(context ->
                                context.getFarm() != null
                                        && farmId.equals(
                                        context.getFarm().getId()
                                )
                        )
                        .max(
                                Comparator.comparing(
                                        AIContextSnapshot::getCapturedAt
                                )
                        )
                        .orElse(null);


        if (snapshot == null) {

            return null;
        }


        return toResponse(
                snapshot
        );
    }


    /*
     * =============================================================
     * Irrigation decision
     * =============================================================
     */
    private String buildIrrigationDecision(
            SmartIrrigationResponse response
    ) {

        if (response == null) {

            return "No smart irrigation decision is available.";
        }


        StringBuilder result =
                new StringBuilder();


        result.append(
                "Decision="
        );


        result.append(
                safe(
                        response.getFieldStatus()
                )
        );


        result.append(
                "; "
        );


        if (response.getActionItem() != null) {

            result.append(
                    "Action="
            );


            result.append(
                    response.getActionItem()
            );


            result.append(
                    "; "
            );
        }


        if (response.getCurrentVwc() != null) {

            result.append(
                    "VWC="
            );


            result.append(
                    response.getCurrentVwc()
            );


            result.append(
                    "%; "
            );
        }


        if (response.getFieldCapacity() != null) {

            result.append(
                    "FC="
            );


            result.append(
                    response.getFieldCapacity()
            );


            result.append(
                    "%; "
            );
        }


        if (response.getWiltingPoint() != null) {

            result.append(
                    "PWP="
            );


            result.append(
                    response.getWiltingPoint()
            );


            result.append(
                    "%; "
            );
        }


        if (response.getDepletionLevel() != null) {

            result.append(
                    "Depletion="
            );


            result.append(
                    response.getDepletionLevel()
                            * 100.0
            );


            result.append(
                    "%; "
            );
        }


        if (response.getEt0() != null) {

            result.append(
                    "ET0="
            );


            result.append(
                    response.getEt0()
            );


            result.append(
                    " mm/day; "
            );
        }


        if (response.getKc() != null) {

            result.append(
                    "Kc="
            );


            result.append(
                    response.getKc()
            );


            result.append(
                    "; "
            );
        }


        if (response.getEtc() != null) {

            result.append(
                    "ETc="
            );


            result.append(
                    response.getEtc()
            );


            result.append(
                    " mm/day; "
            );
        }


        if (response.getForecastRain7d() != null) {

            result.append(
                    "ForecastRain7d="
            );


            result.append(
                    response.getForecastRain7d()
            );


            result.append(
                    " mm; "
            );
        }


        if (response.getForecastConfidence() != null) {

            result.append(
                    "ForecastConfidence="
            );


            result.append(
                    response.getForecastConfidence()
                            * 100.0
            );


            result.append(
                    "%; "
            );
        }


        if (response.getDataStatus() != null) {

            result.append(
                    "DataStatus="
            );


            result.append(
                    response.getDataStatus()
            );


            result.append(
                    "."
            );
        }


        return result.toString();
    }


    /*
     * =============================================================
     * Crop summary
     * =============================================================
     */
    private String buildCropDecision(
            CropHistory cropHistory
    ) {

        if (cropHistory == null) {

            return "No recent crop-history record is available.";
        }


        String cropName =
                cropHistory.getCrop() != null
                        ? cropHistory.getCrop().getName()
                        : "Unknown crop";


        return
                "Latest recorded crop: "
                        + cropName
                        + ".";
    }


    /*
     * =============================================================
     * Nutrient summary
     * =============================================================
     */
    private String buildNutrientDecision(
            NutrientBalance nutrientBalance
    ) {

        if (nutrientBalance == null) {

            return "No nutrient-balance record is available.";
        }


        return
                "Latest nutrient balance: N="
                        + nutrientBalance.getNitrogenBalance()
                        + ", P="
                        + nutrientBalance.getPhosphorusBalance()
                        + ", K="
                        + nutrientBalance.getPotassiumBalance()
                        + ".";
    }


    /*
     * =============================================================
     * Weather-risk summary
     * =============================================================
     */
    private String buildWeatherRiskDecision(
            WeatherRisk weatherRisk
    ) {

        if (weatherRisk == null) {

            return "No recent weather-risk record is available.";
        }


        return
                "Latest weather risk: "
                        + weatherRisk.getRiskType()
                        + ", level="
                        + weatherRisk.getRiskLevel()
                        + ", score="
                        + weatherRisk.getRiskScore()
                        + ".";
    }


    /*
     * =============================================================
     * Disease/Pest summary
     * =============================================================
     */
    private String buildDiseasePestDecision(
            DiseasePestRisk risk
    ) {

        if (risk == null) {

            return
                    "No recent disease/pest risk record is available.";
        }


        StringBuilder result =
                new StringBuilder();


        result.append(
                "Latest disease/pest risk: "
        );


        result.append(
                safe(
                        risk.getRiskName()
                )
        );


        result.append(
                ", category="
        );


        result.append(
                safe(
                        risk.getRiskCategory()
                )
        );


        result.append(
                ", level="
        );


        result.append(
                safe(
                        risk.getRiskLevel()
                )
        );


        result.append(
                ", score="
        );


        result.append(
                risk.getRiskScore()
        );


        if (
                risk.getRecommendation() != null
                        && !risk.getRecommendation().isBlank()
        ) {

            result.append(
                    ", recommendation="
            );


            result.append(
                    risk.getRecommendation()
            );
        }


        result.append(
                "."
        );


        return result.toString();
    }


    /*
     * =============================================================
     * Satellite summary
     * =============================================================
     */
    private String buildSatelliteDecision(
            SatelliteObservation observation
    ) {

        if (observation == null) {

            return
                    "No recent satellite observation is available.";
        }


        StringBuilder result =
                new StringBuilder();


        result.append(
                "Latest satellite observation: "
        );


        result.append(
                safe(
                        observation.getCropCondition()
                )
        );


        result.append(
                ", change="
        );


        result.append(
                safe(
                        observation.getChangeStatus()
                )
        );


        if (observation.getNdvi() != null) {

            result.append(
                    ", NDVI="
            );


            result.append(
                    observation.getNdvi()
            );
        }


        if (observation.getNdwi() != null) {

            result.append(
                    ", NDWI="
            );


            result.append(
                    observation.getNdwi()
            );
        }


        if (observation.getChangePercent() != null) {

            result.append(
                    ", changePercent="
            );


            result.append(
                    observation.getChangePercent()
            );


            result.append(
                    "%"
            );
        }


        result.append(
                ", dataStatus="
        );


        result.append(
                safe(
                        observation.getDataStatus()
                )
        );


        result.append(
                "."
        );


        return result.toString();
    }


    /*
     * =============================================================
     * Field operation summary
     * =============================================================
     */
    private String buildFieldOperationDecision(
            WeatherRecord weather,
            IrrigationTelemetry telemetry
    ) {

        StringBuilder result =
                new StringBuilder();


        if (weather != null) {

            result.append(
                    "Current weather: "
            );


            result.append(
                    safe(
                            weather.getWeatherCondition()
                    )
            );


            if (weather.getTemperature() != null) {

                result.append(
                        ", temperature="
                );


                result.append(
                        weather.getTemperature()
                );


                result.append(
                        "°C"
                );
            }


            if (weather.getHumidity() != null) {

                result.append(
                        ", humidity="
                );


                result.append(
                        weather.getHumidity()
                );
            }


            result.append(
                    ". "
            );
        }


        if (telemetry != null
                && telemetry.getCropStage() != null) {

            result.append(
                    "Current crop stage: "
            );


            result.append(
                    telemetry.getCropStage()
            );


            result.append(
                    "."
            );
        }


        if (result.isEmpty()) {

            return
                    "No current field-operation context is available.";
        }


        return result.toString();
    }


    /*
     * =============================================================
     * MEMBER 6 — Field Health Passport
     * =============================================================
     */
    private String buildFieldHealthPassportDecision(
            FieldHealthPassportResponse passport
    ) {

        if (passport == null) {

            return
                    "No Field Health Passport is available.";
        }


        return
                "Field Health Passport: status="
                        + safe(
                        passport.getOverallHealthStatus()
                )
                        + ", healthScore="
                        + passport.getHealthScore()
                        + ", crop="
                        + safe(
                        passport.getCurrentCropName()
                )
                        + ", soilTrend="
                        + safe(
                        passport.getSoilTrend()
                )
                        + ", recoveryFocus="
                        + safe(
                        passport.getRecoveryFocusArea()
                )
                        + ", recoveryAction="
                        + safe(
                        passport.getRecoveryAction()
                )
                        + ", recommendation="
                        + safe(
                        passport.getRecoveryRecommendation()
                )
                        + ".";
    }


    /*
     * =============================================================
     * MEMBER 6 — Soil Test Trend
     * =============================================================
     */
    private String buildSoilTestTrendDecision(
            SoilTestTrendResponse trend
    ) {

        if (trend == null) {

            return
                    "No soil-test trend information is available.";
        }


        return
                "Soil Test Trend: overallTrend="
                        + safe(
                        trend.getOverallTrend()
                )
                        + ", latestTestDate="
                        + trend.getLatestTestDate()
                        + ", nextSuggestedTestDate="
                        + trend.getNextSuggestedTestDate()
                        + ", reminderStatus="
                        + safe(
                        trend.getTestReminderStatus()
                )
                        + ", totalSamples="
                        + trend.getTotalSoilSamples()
                        + ", measuredSamples="
                        + trend.getMeasuredSoilSamples()
                        + ", recommendedAction="
                        + safe(
                        trend.getRecommendedAction()
                )
                        + ".";
    }


    /*
     * =============================================================
     * MEMBER 6 — Lowest-Cost Safe Plan
     * =============================================================
     */
    private String buildLowestCostPlanDecision(
            LowestCostSafePlanResponse plan
    ) {

        if (plan == null) {

            return
                    "Lowest-Cost Safe Plan was not evaluated "
                            + "because target-season planning inputs "
                            + "were not supplied.";
        }


        return
                "Lowest-Cost Safe Plan: crop="
                        + safe(
                        plan.getRecommendedCrop()
                )
                        + ", score="
                        + plan.getRecommendationScore()
                        + ", estimatedCost="
                        + plan.getEstimatedTotalCostInrPerHa()
                        + " INR/ha"
                        + ", waterDemand="
                        + safe(
                        plan.getWaterDemandClass()
                )
                        + ", reason="
                        + safe(
                        plan.getReason()
                )
                        + ".";
    }


    /*
     * =============================================================
     * MEMBER 6 — Village Cooperative
     * =============================================================
     */
    private String buildCooperativeDecision(
            CooperativeSummaryResponse summary
    ) {

        if (summary == null) {

            return
                    "No active village cooperative is linked "
                            + "to this farm's farmer account.";
        }


        return
                "Village Cooperative: "
                        + safe(
                        summary.getCooperativeName()
                )
                        + ", status="
                        + safe(
                        summary.getStatus()
                )
                        + ", activeMembers="
                        + summary.getActiveMemberCount()
                        + ", resources="
                        + summary.getResourceCount()
                        + ", availableResources="
                        + summary.getAvailableResourceCount()
                        + ", usageRecords="
                        + summary.getTotalUsageRecords()
                        + ", completedUsage="
                        + summary.getCompletedUsageRecords()
                        + ", recordedUsageCost="
                        + summary.getTotalUsageCostInr()
                        + " INR"
                        + ", memberShare="
                        + summary.getTotalMemberShareInr()
                        + " INR.";
    }


    /*
     * =============================================================
     * Build overall decision summary.
     * =============================================================
     */
    private String buildDecisionSummary(
            String irrigation,
            String crop,
            String nutrient,
            String weatherRisk,
            String diseasePestRisk,
            String satellite,
            String fieldOperation,
            String fieldHealthPassport,
            String soilTestTrend,
            String lowestCostPlan,
            String cooperative
    ) {

        return
                "Irrigation: "
                        + irrigation
                        + " Crop: "
                        + crop
                        + " Nutrients: "
                        + nutrient
                        + " Weather risk: "
                        + weatherRisk
                        + " Disease/Pest risk: "
                        + diseasePestRisk
                        + " Satellite monitoring: "
                        + satellite
                        + " Field operations: "
                        + fieldOperation
                        + " Field Health Passport: "
                        + fieldHealthPassport
                        + " Soil Test Trend: "
                        + soilTestTrend
                        + " Lowest-Cost Safe Plan: "
                        + lowestCostPlan
                        + " Village Cooperative: "
                        + cooperative;
    }


    /*
     * =============================================================
     * Build complete farm context.
     * =============================================================
     */
    private String buildFarmContext(
            Farm farm,
            SoilSample soil,
            CropHistory cropHistory,
            Crop crop,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            WeatherRisk weatherRisk,
            DiseasePestRisk diseasePestRisk,
            SatelliteObservation satelliteObservation,
            SmartIrrigationResponse irrigationResponse,
            FieldHealthPassportResponse fieldHealthPassport,
            SoilTestTrendResponse soilTestTrend,
            LowestCostSafePlanResponse lowestCostPlan,
            CooperativeSummaryResponse cooperativeSummary
    ) {

        StringBuilder context =
                new StringBuilder();


        context.append(
                "Farm: "
        );


        context.append(
                safe(
                        farm.getName()
                )
        );


        context.append(
                ". Location: "
        );


        context.append(
                safe(
                        farm.getLocation()
                )
        );


        if (farm.getLatitude() != null) {

            context.append(
                    ". Latitude: "
            );


            context.append(
                    farm.getLatitude()
            );
        }


        if (farm.getLongitude() != null) {

            context.append(
                    ". Longitude: "
            );


            context.append(
                    farm.getLongitude()
            );
        }


        /*
         * Soil
         */
        if (soil != null) {

            context.append(
                    ". Latest soil sample: pH="
            );


            context.append(
                    soil.getPH()
            );


            context.append(
                    ", soilType="
            );


            context.append(
                    safe(
                            soil.getSoilType()
                    )
            );


            if (soil.getNitrogen() != null) {

                context.append(
                        ", N="
                );


                context.append(
                        soil.getNitrogen()
                );
            }


            if (soil.getPhosphorus() != null) {

                context.append(
                        ", P="
                );


                context.append(
                        soil.getPhosphorus()
                );
            }


            if (soil.getPotassium() != null) {

                context.append(
                        ", K="
                );


                context.append(
                        soil.getPotassium()
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Crop
         */
        if (cropHistory != null
                && cropHistory.getCrop() != null) {

            context.append(
                    " Previous/latest recorded crop: "
            );


            context.append(
                    cropHistory.getCrop().getName()
            );


            context.append(
                    "."
            );
        }


        if (crop != null) {

            context.append(
                    " Associated crop: "
            );


            context.append(
                    crop.getName()
            );


            context.append(
                    "."
            );
        }


        /*
         * Weather
         */
        if (weather != null) {

            context.append(
                    " Current weather: "
            );


            context.append(
                    safe(
                            weather.getWeatherCondition()
                    )
            );


            if (weather.getTemperature() != null) {

                context.append(
                        ", temperature="
                );


                context.append(
                        weather.getTemperature()
                );


                context.append(
                        "°C"
                );
            }


            if (weather.getHumidity() != null) {

                context.append(
                        ", humidity="
                );


                context.append(
                        weather.getHumidity()
                );
            }


            if (weather.getRainfall() != null) {

                context.append(
                        ", rainfall="
                );


                context.append(
                        weather.getRainfall()
                );


                context.append(
                        " mm"
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Forecast
         */
        if (forecast != null) {

            context.append(
                    " Upcoming forecast: date="
            );


            context.append(
                    forecast.getForecastDate()
            );


            if (forecast.getRainfallMm() != null) {

                context.append(
                        ", rainfall="
                );


                context.append(
                        forecast.getRainfallMm()
                );


                context.append(
                        " mm"
                );
            }


            if (forecast.getForecastConfidence() != null) {

                context.append(
                        ", confidence="
                );


                context.append(
                        forecast.getForecastConfidence()
                                * 100.0
                );


                context.append(
                        "%"
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Telemetry
         */
        if (telemetry != null) {

            context.append(
                    " Latest irrigation telemetry: "
            );


            if (telemetry.getVwc30cm() != null) {

                context.append(
                        "VWC30cm="
                );


                context.append(
                        telemetry.getVwc30cm()
                );


                context.append(
                        "%"
                );
            }


            if (telemetry.getPumpState() != null) {

                context.append(
                        ", pumpState="
                );


                context.append(
                        telemetry.getPumpState()
                );
            }


            if (telemetry.getSolarIrradianceWm2() != null) {

                context.append(
                        ", solar="
                );


                context.append(
                        telemetry.getSolarIrradianceWm2()
                );


                context.append(
                        " W/m²"
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Smart irrigation decision
         */
        if (irrigationResponse != null) {

            context.append(
                    " Smart irrigation decision: "
            );


            context.append(
                    safe(
                            irrigationResponse.getFieldStatus()
                    )
            );


            if (irrigationResponse.getActionItem() != null) {

                context.append(
                        ". Action: "
                );


                context.append(
                        irrigationResponse.getActionItem()
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Fertilizer
         */
        if (fertilizer != null) {

            context.append(
                    " Latest fertilizer recommendation: "
            );


            context.append(
                    safe(
                            fertilizer.getRecommendation()
                    )
            );


            context.append(
                    "."
            );
        }


        /*
         * Nutrient balance
         */
        if (nutrient != null) {

            context.append(
                    " Nutrient balance: N="
            );


            context.append(
                    nutrient.getNitrogenBalance()
            );


            context.append(
                    ", P="
            );


            context.append(
                    nutrient.getPhosphorusBalance()
            );


            context.append(
                    ", K="
            );


            context.append(
                    nutrient.getPotassiumBalance()
            );


            context.append(
                    "."
            );
        }


        /*
         * Weather risk
         */
        if (weatherRisk != null) {

            context.append(
                    " Weather risk: "
            );


            context.append(
                    weatherRisk.getRiskType()
            );


            context.append(
                    ", level="
            );


            context.append(
                    weatherRisk.getRiskLevel()
            );


            context.append(
                    ", score="
            );


            context.append(
                    weatherRisk.getRiskScore()
            );


            context.append(
                    "."
            );
        }


        /*
         * Disease/Pest risk
         */
        if (diseasePestRisk != null) {

            context.append(
                    " Disease/pest risk: "
            );


            context.append(
                    safe(
                            diseasePestRisk.getRiskName()
                    )
            );


            context.append(
                    ", category="
            );


            context.append(
                    safe(
                            diseasePestRisk.getRiskCategory()
                    )
            );


            context.append(
                    ", level="
            );


            context.append(
                    safe(
                            diseasePestRisk.getRiskLevel()
                    )
            );


            context.append(
                    ", score="
            );


            context.append(
                    diseasePestRisk.getRiskScore()
            );


            if (
                    diseasePestRisk.getRecommendation() != null
                            && !diseasePestRisk
                            .getRecommendation()
                            .isBlank()
            ) {

                context.append(
                        ", recommendation="
                );


                context.append(
                        diseasePestRisk.getRecommendation()
                );
            }


            context.append(
                    "."
            );
        }


        /*
         * Satellite monitoring
         */
        if (satelliteObservation != null) {

            context.append(
                    " Satellite monitoring: "
            );


            context.append(
                    "cropCondition="
            );


            context.append(
                    safe(
                            satelliteObservation.getCropCondition()
                    )
            );


            context.append(
                    ", changeStatus="
            );


            context.append(
                    safe(
                            satelliteObservation.getChangeStatus()
                    )
            );


            if (satelliteObservation.getNdvi() != null) {

                context.append(
                        ", NDVI="
                );


                context.append(
                        satelliteObservation.getNdvi()
                );
            }


            if (satelliteObservation.getNdwi() != null) {

                context.append(
                        ", NDWI="
                );


                context.append(
                        satelliteObservation.getNdwi()
                );
            }


            if (satelliteObservation.getChangePercent() != null) {

                context.append(
                        ", changePercent="
                );


                context.append(
                        satelliteObservation.getChangePercent()
                );


                context.append(
                        "%"
                );
            }


            context.append(
                    ", source="
            );


            context.append(
                    safe(
                            satelliteObservation.getDataSource()
                    )
            );


            context.append(
                    ", dataStatus="
            );


            context.append(
                    safe(
                            satelliteObservation.getDataStatus()
                    )
            );


            context.append(
                    "."
            );
        }


        /*
         * =========================================================
         * MEMBER 6 — FIELD HEALTH PASSPORT
         * =========================================================
         */
        if (fieldHealthPassport != null) {

            context.append(
                    " Field Health Passport: "
                            + "status="
                            + safe(
                            fieldHealthPassport
                                    .getOverallHealthStatus()
                    )
                            + ", score="
                            + fieldHealthPassport
                            .getHealthScore()
                            + ", crop="
                            + safe(
                            fieldHealthPassport
                                    .getCurrentCropName()
                    )
                            + ", soilTrend="
                            + safe(
                            fieldHealthPassport
                                    .getSoilTrend()
                    )
                            + ", recoveryFocus="
                            + safe(
                            fieldHealthPassport
                                    .getRecoveryFocusArea()
                    )
                            + ", recoveryAction="
                            + safe(
                            fieldHealthPassport
                                    .getRecoveryAction()
                    )
                            + ", recommendation="
                            + safe(
                            fieldHealthPassport
                                    .getRecoveryRecommendation()
                    )
                            + "."
            );
        }


        /*
         * =========================================================
         * MEMBER 6 — SOIL TEST TREND
         * =========================================================
         */
        if (soilTestTrend != null) {

            context.append(
                    " Soil Test Trend: "
                            + "overallTrend="
                            + safe(
                            soilTestTrend
                                    .getOverallTrend()
                    )
                            + ", latestTestDate="
                            + soilTestTrend
                            .getLatestTestDate()
                            + ", nextSuggestedTestDate="
                            + soilTestTrend
                            .getNextSuggestedTestDate()
                            + ", reminderStatus="
                            + safe(
                            soilTestTrend
                                    .getTestReminderStatus()
                    )
                            + ", totalSamples="
                            + soilTestTrend
                            .getTotalSoilSamples()
                            + ", measuredSamples="
                            + soilTestTrend
                            .getMeasuredSoilSamples()
                            + ", recommendedAction="
                            + safe(
                            soilTestTrend
                                    .getRecommendedAction()
                    )
                            + "."
            );
        }


        /*
         * =========================================================
         * MEMBER 6 — LOWEST-COST SAFE PLAN
         * =========================================================
         */
        if (lowestCostPlan != null) {

            context.append(
                    " Lowest-Cost Safe Plan: "
                            + "crop="
                            + safe(
                            lowestCostPlan
                                    .getRecommendedCrop()
                    )
                            + ", targetSeason="
                            + safe(
                            lowestCostPlan
                                    .getTargetSeason()
                    )
                            + ", constraint="
                            + safe(
                            lowestCostPlan
                                    .getFarmerConstraint()
                    )
                            + ", estimatedCost="
                            + lowestCostPlan
                            .getEstimatedTotalCostInrPerHa()
                            + " INR/ha"
                            + ", waterDemand="
                            + safe(
                            lowestCostPlan
                                    .getWaterDemandClass()
                    )
                            + ", score="
                            + lowestCostPlan
                            .getRecommendationScore()
                            + "."
            );
        }


        /*
         * =========================================================
         * MEMBER 6 — VILLAGE COOPERATIVE
         * =========================================================
         */
        if (cooperativeSummary != null) {

            context.append(
                    " Village Cooperative: "
                            + safe(
                            cooperativeSummary
                                    .getCooperativeName()
                    )
                            + ", status="
                            + safe(
                            cooperativeSummary
                                    .getStatus()
                    )
                            + ", activeMembers="
                            + cooperativeSummary
                            .getActiveMemberCount()
                            + ", resources="
                            + cooperativeSummary
                            .getResourceCount()
                            + ", availableResources="
                            + cooperativeSummary
                            .getAvailableResourceCount()
                            + ", usageRecords="
                            + cooperativeSummary
                            .getTotalUsageRecords()
                            + ", completedUsage="
                            + cooperativeSummary
                            .getCompletedUsageRecords()
                            + ", usageCost="
                            + cooperativeSummary
                            .getTotalUsageCostInr()
                            + " INR"
                            + ", memberShare="
                            + cooperativeSummary
                            .getTotalMemberShareInr()
                            + " INR."
            );
        }


        return context.toString();
    }


    /*
     * =============================================================
     * Build uncertainty context.
     * =============================================================
     */
    private String buildUncertaintyContext(
            SoilSample soil,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            WeatherRisk weatherRisk,
            DiseasePestRisk diseasePestRisk,
            SatelliteObservation satelliteObservation,
            SmartIrrigationResponse irrigationResponse,
            FieldHealthPassportResponse fieldHealthPassport,
            SoilTestTrendResponse soilTestTrend,
            LowestCostSafePlanResponse lowestCostPlan,
            CooperativeSummaryResponse cooperativeSummary
    ) {

        StringBuilder uncertainty =
                new StringBuilder();


        uncertainty.append(
                "AI should treat this context as decision-support information only."
        );


        /*
         * Forecast test data
         */
        if (
                forecast != null
                        && (
                        "TEST".equalsIgnoreCase(
                                forecast.getDataStatus()
                        )
                                || "TEST_FORECAST".equalsIgnoreCase(
                                forecast.getSource()
                        )
                )
        ) {

            uncertainty.append(
                    " Weather forecast is synthetic test data."
            );
        }


        /*
         * Telemetry test data
         */
        if (
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                )
        ) {

            uncertainty.append(
                    " Irrigation telemetry is synthetic test data."
            );
        }


        /*
         * Fertilizer uncertainty
         */
        if (
                fertilizer != null
                        && fertilizer.getUncertaintyNote() != null
        ) {

            uncertainty.append(
                    " Fertilizer recommendation note: "
            );


            uncertainty.append(
                    fertilizer.getUncertaintyNote()
            );
        }


        /*
         * Smart irrigation uncertainty
         */
        if (
                irrigationResponse != null
                        && irrigationResponse
                        .getUncertaintyNote() != null
        ) {

            uncertainty.append(
                    " Smart irrigation note: "
            );


            uncertainty.append(
                    irrigationResponse.getUncertaintyNote()
            );
        }


        /*
         * Disease/Pest uncertainty
         */
        if (
                diseasePestRisk != null
                        && diseasePestRisk
                        .getUncertaintyNote() != null
        ) {

            uncertainty.append(
                    " Disease/pest risk note: "
            );


            uncertainty.append(
                    diseasePestRisk.getUncertaintyNote()
            );
        }


        /*
         * Satellite uncertainty
         */
        if (
                satelliteObservation != null
                        && satelliteObservation
                        .getUncertaintyNote() != null
        ) {

            uncertainty.append(
                    " Satellite monitoring note: "
            );


            uncertainty.append(
                    satelliteObservation.getUncertaintyNote()
            );
        }


        /*
         * =========================================================
         * MEMBER 6 uncertainty
         * =========================================================
         */

        if (
                fieldHealthPassport != null
        ) {

            uncertainty.append(
                    " Field Health Passport is a long-term "
                            + "decision-support summary; its health "
                            + "score is not a yield prediction."
            );
        }


        if (
                soilTestTrend != null
        ) {

            uncertainty.append(
                    " Soil Test Trend may report insufficient "
                            + "historical data when multiple dated "
                            + "soil tests are unavailable."
            );
        }


        if (
                lowestCostPlan != null
        ) {

            uncertainty.append(
                    " Lowest-Cost Safe Plan uses planning "
                            + "estimates from the crop-comparison "
                            + "dataset and does not predict yield, "
                            + "income or guaranteed savings."
            );
        }


        if (
                cooperativeSummary != null
        ) {

            uncertainty.append(
                    " Cooperative cost figures are based on "
                            + "recorded usage and supplied planning "
                            + "rates and are not guaranteed savings."
            );
        }


        /*
         * Missing soil
         */
        if (soil == null) {

            uncertainty.append(
                    " No latest soil sample is available."
            );
        }


        /*
         * Missing weather
         */
        if (weather == null) {

            uncertainty.append(
                    " No current weather record is available."
            );
        }


        /*
         * Missing forecast
         */
        if (forecast == null) {

            uncertainty.append(
                    " No upcoming forecast is available."
            );
        }


        /*
         * Missing nutrient balance
         */
        if (nutrient == null) {

            uncertainty.append(
                    " No latest nutrient-balance record is available."
            );
        }


        /*
         * Missing weather risk
         */
        if (weatherRisk == null) {

            uncertainty.append(
                    " No latest weather-risk record is available."
            );
        }


        /*
         * Missing disease/pest risk
         */
        if (diseasePestRisk == null) {

            uncertainty.append(
                    " No recent disease/pest risk assessment is available."
            );
        }


        /*
         * Missing satellite
         */
        if (satelliteObservation == null) {

            uncertainty.append(
                    " No recent satellite observation is available."
            );
        }


        uncertainty.append(
                " Critical missing information should be surfaced instead of being guessed."
        );


        return uncertainty.toString();
    }


    /*
     * =============================================================
     * Determine data provenance.
     * =============================================================
     */
    private String determineDataStatus(
            SoilSample soil,
            WeatherRecord weather,
            WeatherForecastData forecast,
            IrrigationTelemetry telemetry,
            FertilizerRecommendation fertilizer,
            NutrientBalance nutrient,
            WeatherRisk weatherRisk,
            DiseasePestRisk diseasePestRisk,
            SatelliteObservation satelliteObservation,
            SmartIrrigationResponse irrigationResponse,
            FieldHealthPassportResponse fieldHealthPassport,
            SoilTestTrendResponse soilTestTrend,
            LowestCostSafePlanResponse lowestCostPlan,
            CooperativeSummaryResponse cooperativeSummary
    ) {

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


        boolean testFertilizer =
                fertilizer != null
                        && (
                        "TEST".equalsIgnoreCase(
                                fertilizer.getDataStatus()
                        )
                                || "ESTIMATED".equalsIgnoreCase(
                                fertilizer.getDataStatus()
                        )
                                || "DEMO_INPUT".equalsIgnoreCase(
                                fertilizer.getSource()
                        )
                );


        boolean testIrrigation =
                irrigationResponse != null
                        && "TEST".equalsIgnoreCase(
                        irrigationResponse.getDataStatus()
                );


        boolean testDiseasePest =
                diseasePestRisk != null
                        && "TEST".equalsIgnoreCase(
                        diseasePestRisk.getDataStatus()
                );


        boolean testSatellite =
                satelliteObservation != null
                        && "TEST".equalsIgnoreCase(
                        satelliteObservation.getDataStatus()
                );


        boolean estimatedLowestCostPlan =
                lowestCostPlan != null
                        && "ESTIMATED".equalsIgnoreCase(
                        lowestCostPlan.getDataStatus()
                );


        boolean testFieldHealthPassport =
                fieldHealthPassport != null
                        && "TEST".equalsIgnoreCase(
                        fieldHealthPassport.getDataStatus()
                );


        boolean testCooperative =
                cooperativeSummary != null
                        && "TEST".equalsIgnoreCase(
                        cooperativeSummary.getDataStatus()
                );


        boolean hasData =
                soil != null
                        || weather != null
                        || forecast != null
                        || telemetry != null
                        || fertilizer != null
                        || nutrient != null
                        || weatherRisk != null
                        || diseasePestRisk != null
                        || satelliteObservation != null
                        || irrigationResponse != null
                        || fieldHealthPassport != null
                        || soilTestTrend != null
                        || lowestCostPlan != null
                        || cooperativeSummary != null;


        /*
         * Any known synthetic input makes the snapshot
         * clearly identifiable as TEST.
         */
        if (
                testForecast
                        || testTelemetry
                        || testFertilizer
                        || testIrrigation
                        || testDiseasePest
                        || testSatellite
                        || testFieldHealthPassport
                        || testCooperative
        ) {

            return "TEST";
        }


        /*
         * Lowest-Cost Safe Plan is an estimated planning layer.
         */
        if (estimatedLowestCostPlan) {

            return "MIXED";
        }


        if (hasData) {

            return "MIXED";
        }


        return "UNKNOWN";
    }


    /*
     * =============================================================
     * Validation.
     * =============================================================
     */
    private void validate(
            AIContextRequest request
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
    }


    /*
     * =============================================================
     * Verify farm.
     * =============================================================
     */
    private void verifyFarm(
            UUID farmId
    ) {

        if (
                !farmRepository.existsById(
                        farmId
                )
        ) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }


    /*
     * =============================================================
     * Null-safe string helper.
     * =============================================================
     */
    private String safe(
            String value
    ) {

        return value == null
                ? "UNKNOWN"
                : value;
    }


    /*
     * =============================================================
     * Entity -> Response.
     * =============================================================
     */
    private AIContextResponse toResponse(
            AIContextSnapshot snapshot
    ) {

        return AIContextResponse.builder()

                .id(
                        snapshot.getId()
                )

                .farmId(
                        snapshot.getFarm() != null
                                ? snapshot.getFarm().getId()
                                : null
                )

                .conversationId(
                        snapshot.getConversation() != null
                                ? snapshot.getConversation().getId()
                                : null
                )

                .messageId(
                        snapshot.getMessage() != null
                                ? snapshot.getMessage().getId()
                                : null
                )

                .soilSampleId(
                        snapshot.getSoilSampleId()
                )

                .cropHistoryId(
                        snapshot.getCropHistoryId()
                )

                .cropId(
                        snapshot.getCropId()
                )

                .weatherRecordId(
                        snapshot.getWeatherRecordId()
                )

                .weatherForecastId(
                        snapshot.getWeatherForecastId()
                )

                .irrigationTelemetryId(
                        snapshot.getIrrigationTelemetryId()
                )

                .fertilizerRecommendationId(
                        snapshot.getFertilizerRecommendationId()
                )

                .nutrientBalanceId(
                        snapshot.getNutrientBalanceId()
                )

                .weatherRiskId(
                        snapshot.getWeatherRiskId()
                )

                .diseasePestRiskId(
                        snapshot.getDiseasePestRiskId()
                )

                .satelliteObservationId(
                        snapshot.getSatelliteObservationId()
                )

                .decisionSummary(
                        snapshot.getDecisionSummary()
                )

                .irrigationDecision(
                        snapshot.getIrrigationDecision()
                )

                .cropDecision(
                        snapshot.getCropDecision()
                )

                .nutrientDecision(
                        snapshot.getNutrientDecision()
                )

                .weatherRiskDecision(
                        snapshot.getWeatherRiskDecision()
                )

                .diseasePestRiskDecision(
                        snapshot.getDiseasePestRiskDecision()
                )

                .satelliteDecision(
                        snapshot.getSatelliteDecision()
                )

                .fieldOperationDecision(
                        snapshot.getFieldOperationDecision()
                )

                /*
                 * Member 6
                 */
                .fieldHealthPassportDecision(
                        snapshot.getFieldHealthPassportDecision()
                )

                .soilTestTrendDecision(
                        snapshot.getSoilTestTrendDecision()
                )

                .lowestCostPlanDecision(
                        snapshot.getLowestCostPlanDecision()
                )

                .cooperativeDecision(
                        snapshot.getCooperativeDecision()
                )

                .farmContext(
                        snapshot.getFarmContext()
                )

                .uncertaintyContext(
                        snapshot.getUncertaintyContext()
                )

                .dataStatus(
                        snapshot.getDataStatus()
                )

                .capturedAt(
                        snapshot.getCapturedAt()
                )

                .build();
    }
}