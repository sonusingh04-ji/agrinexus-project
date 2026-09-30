package com.agrinexus.service;

import com.agrinexus.dto.DigitalMemoryResponse;
import com.agrinexus.entity.*;
import com.agrinexus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FarmDigitalMemoryService {

    private final FarmRepository farmRepository;

    private final CropHistoryRepository cropHistoryRepository;

    private final SoilSampleRepository soilSampleRepository;

    private final DigitalMemoryIrrigationEventRepository
            irrigationEventRepository;

    private final DigitalMemoryIrrigationTelemetryRepository
            irrigationTelemetryRepository;

    private final DigitalMemoryFertilizerRecommendationRepository
            fertilizerRecommendationRepository;

    private final DigitalMemoryFertilizerScheduleRepository
            fertilizerScheduleRepository;

    private final DigitalMemoryWeatherRepository
            weatherRepository;

    private final DigitalMemoryNutrientBalanceRepository
            nutrientBalanceRepository;

    private final DigitalMemoryWeatherRiskRepository
            weatherRiskRepository;


    public DigitalMemoryResponse getFarmMemory(
            UUID farmId
    ) {

        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );

        var crops =
                cropHistoryRepository
                        .findByFarmIdOrderBySowingDateDesc(
                                farmId
                        )
                        .stream()
                        .map(history ->
                                DigitalMemoryResponse.CropMemoryItem
                                        .builder()
                                        .id(history.getId())
                                        .cropId(
                                                history.getCrop() != null
                                                        ? history.getCrop().getId()
                                                        : null
                                        )
                                        .cropName(
                                                history.getCrop() != null
                                                        ? history.getCrop().getName()
                                                        : null
                                        )
                                        .season(history.getSeason())
                                        .sowingDate(history.getSowingDate())
                                        .harvestDate(history.getHarvestDate())
                                        .cropStage(history.getCropStage())
                                        .residuePresent(history.getResiduePresent())
                                        .residueType(history.getResidueType())
                                        .notes(history.getNotes())
                                        .build()
                        )
                        .toList();


        var soil =
                soilSampleRepository
                        .findByFarmIdOrderBySampleDateDesc(
                                farmId
                        )
                        .stream()
                        .map(sample ->
                                DigitalMemoryResponse.SoilMemoryItem
                                        .builder()
                                        .id(sample.getId())
                                        .sampleDate(sample.getSampleDate())
                                        .soilType(sample.getSoilType())
                                        .pH(sample.getPH())
                                        .electricalConductivity(
                                                sample.getElectricalConductivity()
                                        )
                                        .organicCarbon(
                                                sample.getOrganicCarbon()
                                        )
                                        .nitrogen(sample.getNitrogen())
                                        .phosphorus(sample.getPhosphorus())
                                        .potassium(sample.getPotassium())
                                        .zinc(sample.getZinc())
                                        .sulphur(sample.getSulphur())
                                        .iron(sample.getIron())
                                        .manganese(sample.getManganese())
                                        .copper(sample.getCopper())
                                        .source(sample.getSource())
                                        .notes(sample.getNotes())
                                        .build()
                        )
                        .toList();


        var irrigationEvents =
                irrigationEventRepository
                        .findByFarmIdOrderByStartTimeDesc(
                                farmId
                        )
                        .stream()
                        .map(event ->
                                DigitalMemoryResponse.IrrigationMemoryItem
                                        .builder()
                                        .id(event.getId())
                                        .deviceId(event.getDeviceId())
                                        .startTime(event.getStartTime())
                                        .endTime(event.getEndTime())
                                        .durationMinutes(
                                                event.getDurationMinutes()
                                        )
                                        .volumeLiters(
                                                event.getVolumeLiters()
                                        )
                                        .triggerReason(
                                                event.getTriggerReason()
                                        )
                                        .completionStatus(
                                                event.getCompletionStatus()
                                        )
                                        .moistureBefore(
                                                event.getMoistureBefore()
                                        )
                                        .moistureAfter(
                                                event.getMoistureAfter()
                                        )
                                        .targetMoisture(
                                                event.getTargetMoisture()
                                        )
                                        .pumpWasOn(
                                                event.getPumpWasOn()
                                        )
                                        .notes(event.getNotes())
                                        .build()
                        )
                        .toList();


        var irrigationTelemetry =
                irrigationTelemetryRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .map(telemetry ->
                                DigitalMemoryResponse
                                        .IrrigationTelemetryMemoryItem
                                        .builder()
                                        .id(telemetry.getId())
                                        .deviceId(telemetry.getDeviceId())
                                        .recordedAt(telemetry.getRecordedAt())
                                        .vwc15cm(telemetry.getVwc15cm())
                                        .vwc30cm(telemetry.getVwc30cm())
                                        .vwc60cm(telemetry.getVwc60cm())
                                        .pumpState(telemetry.getPumpState())
                                        .flowRateLpm(
                                                telemetry.getFlowRateLpm()
                                        )
                                        .linePressureBar(
                                                telemetry.getLinePressureBar()
                                        )
                                        .waterTableDepthM(
                                                telemetry.getWaterTableDepthM()
                                        )
                                        .pumpRuntimeMinutes(
                                                telemetry.getPumpRuntimeMinutes()
                                        )
                                        .rainfallMm(
                                                telemetry.getRainfallMm()
                                        )
                                        .et0Mm(
                                                telemetry.getEt0Mm()
                                        )
                                        .etcMm(
                                                telemetry.getEtcMm()
                                        )
                                        .solarIrradianceWm2(
                                                telemetry.getSolarIrradianceWm2()
                                        )
                                        .solarPowerKw(
                                                telemetry.getSolarPowerKw()
                                        )
                                        .cloudCoverPercent(
                                                telemetry.getCloudCoverPercent()
                                        )
                                        .storageLevelPercent(
                                                telemetry.getStorageLevelPercent()
                                        )
                                        .batterySocPercent(
                                                telemetry.getBatterySocPercent()
                                        )
                                        .cropStage(
                                                telemetry.getCropStage()
                                        )
                                        .kc(telemetry.getKc())
                                        .source(telemetry.getSource())
                                        .sensorStatus(
                                                telemetry.getSensorStatus()
                                        )
                                        .payloadReference(
                                                telemetry.getPayloadReference()
                                        )
                                        .notes(telemetry.getNotes())
                                        .build()
                        )
                        .toList();


        var fertilizerRecommendations =
                fertilizerRecommendationRepository
                        .findByFarmIdOrderByGeneratedAtDesc(
                                farmId
                        )
                        .stream()
                        .map(rec ->
                                DigitalMemoryResponse.FertilizerMemoryItem
                                        .builder()
                                        .id(rec.getId())
                                        .cropId(
                                                rec.getCrop() != null
                                                        ? rec.getCrop().getId()
                                                        : null
                                        )
                                        .cropName(
                                                rec.getCrop() != null
                                                        ? rec.getCrop().getName()
                                                        : null
                                        )
                                        .cropStage(rec.getCropStage())
                                        .fieldArea(rec.getFieldArea())
                                        .fieldAreaUnit(rec.getFieldAreaUnit())
                                        .nitrogen(rec.getNitrogen())
                                        .phosphorus(rec.getPhosphorus())
                                        .potassium(rec.getPotassium())
                                        .sulphur(rec.getSulphur())
                                        .zinc(rec.getZinc())
                                        .status(rec.getStatus())
                                        .recommendation(
                                                rec.getRecommendation()
                                        )
                                        .reason(rec.getReason())
                                        .dataStatus(rec.getDataStatus())
                                        .source(rec.getSource())
                                        .uncertaintyNote(
                                                rec.getUncertaintyNote()
                                        )
                                        .generatedAt(rec.getGeneratedAt())
                                        .notes(rec.getNotes())
                                        .build()
                        )
                        .toList();


        var fertilizerSchedules =
                fertilizerScheduleRepository
                        .findByFarmIdOrderByPlannedDateDesc(
                                farmId
                        )
                        .stream()
                        .map(schedule ->
                                DigitalMemoryResponse
                                        .FertilizerScheduleMemoryItem
                                        .builder()
                                        .id(schedule.getId())
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
                                        .recommendationId(
                                                schedule
                                                        .getFertilizerRecommendation() != null
                                                        ? schedule
                                                        .getFertilizerRecommendation()
                                                        .getId()
                                                        : null
                                        )
                                        .stage(schedule.getStage())
                                        .plannedDate(
                                                schedule.getPlannedDate()
                                        )
                                        .actionType(
                                                schedule.getActionType()
                                        )
                                        .productName(
                                                schedule.getProductName()
                                        )
                                        .nitrogen(schedule.getNitrogen())
                                        .phosphorus(
                                                schedule.getPhosphorus()
                                        )
                                        .potassium(
                                                schedule.getPotassium()
                                        )
                                        .sulphur(schedule.getSulphur())
                                        .zinc(schedule.getZinc())
                                        .dataStatus(
                                                schedule.getDataStatus()
                                        )
                                        .status(schedule.getStatus())
                                        .weatherCondition(
                                                schedule.getWeatherCondition()
                                        )
                                        .reason(schedule.getReason())
                                        .recommendation(
                                                schedule.getRecommendation()
                                        )
                                        .reassessmentRequired(
                                                schedule
                                                        .getReassessmentRequired()
                                        )
                                        .source(schedule.getSource())
                                        .uncertaintyNote(
                                                schedule
                                                        .getUncertaintyNote()
                                        )
                                        .notes(schedule.getNotes())
                                        .build()
                        )
                        .toList();


        var weather =
                weatherRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .map(record ->
                                DigitalMemoryResponse.WeatherMemoryItem
                                        .builder()
                                        .id(record.getId())
                                        .recordedAt(
                                                record.getRecordedAt()
                                        )
                                        .temperature(
                                                record.getTemperature()
                                        )
                                        .feelsLike(
                                                record.getFeelsLike()
                                        )
                                        .humidity(
                                                record.getHumidity()
                                        )
                                        .rainfall(
                                                record.getRainfall()
                                        )
                                        .windSpeed(
                                                record.getWindSpeed()
                                        )
                                        .windDirection(
                                                record.getWindDirection()
                                        )
                                        .pressure(
                                                record.getPressure()
                                        )
                                        .weatherCondition(
                                                record.getWeatherCondition()
                                        )
                                        .weatherDescription(
                                                record.getWeatherDescription()
                                        )
                                        .build()
                        )
                        .toList();


        var nutrientBalances =
                nutrientBalanceRepository
                        .findByFarmIdOrderByPeriodStartDesc(
                                farmId
                        )
                        .stream()
                        .map(balance ->
                                DigitalMemoryResponse
                                        .NutrientBalanceMemoryItem
                                        .builder()
                                        .id(balance.getId())
                                        .cropId(
                                                balance.getCrop() != null
                                                        ? balance.getCrop().getId()
                                                        : null
                                        )
                                        .cropName(
                                                balance.getCrop() != null
                                                        ? balance.getCrop().getName()
                                                        : null
                                        )
                                        .periodStart(
                                                balance.getPeriodStart()
                                        )
                                        .periodEnd(
                                                balance.getPeriodEnd()
                                        )
                                        .nitrogenInput(
                                                balance.getNitrogenInput()
                                        )
                                        .phosphorusInput(
                                                balance.getPhosphorusInput()
                                        )
                                        .potassiumInput(
                                                balance.getPotassiumInput()
                                        )
                                        .sulphurInput(
                                                balance.getSulphurInput()
                                        )
                                        .zincInput(
                                                balance.getZincInput()
                                        )
                                        .nitrogenUptake(
                                                balance.getNitrogenUptake()
                                        )
                                        .phosphorusUptake(
                                                balance.getPhosphorusUptake()
                                        )
                                        .potassiumUptake(
                                                balance.getPotassiumUptake()
                                        )
                                        .sulphurUptake(
                                                balance.getSulphurUptake()
                                        )
                                        .zincUptake(
                                                balance.getZincUptake()
                                        )
                                        .nitrogenResidueReturn(
                                                balance.getNitrogenResidueReturn()
                                        )
                                        .phosphorusResidueReturn(
                                                balance.getPhosphorusResidueReturn()
                                        )
                                        .potassiumResidueReturn(
                                                balance.getPotassiumResidueReturn()
                                        )
                                        .sulphurResidueReturn(
                                                balance.getSulphurResidueReturn()
                                        )
                                        .zincResidueReturn(
                                                balance.getZincResidueReturn()
                                        )
                                        .nitrogenBiologicalFixation(
                                                balance
                                                        .getNitrogenBiologicalFixation()
                                        )
                                        .nitrogenBalance(
                                                balance.getNitrogenBalance()
                                        )
                                        .phosphorusBalance(
                                                balance.getPhosphorusBalance()
                                        )
                                        .potassiumBalance(
                                                balance.getPotassiumBalance()
                                        )
                                        .sulphurBalance(
                                                balance.getSulphurBalance()
                                        )
                                        .zincBalance(
                                                balance.getZincBalance()
                                        )
                                        .dataStatus(
                                                balance.getDataStatus()
                                        )
                                        .source(
                                                balance.getSource()
                                        )
                                        .notes(
                                                balance.getNotes()
                                        )
                                        .build()
                        )
                        .toList();


        var weatherRisks =
                weatherRiskRepository
                        .findByFarmIdOrderByDetectedAtDesc(
                                farmId
                        )
                        .stream()
                        .map(risk ->
                                DigitalMemoryResponse.WeatherRiskMemoryItem
                                        .builder()
                                        .id(risk.getId())
                                        .riskType(
                                                risk.getRiskType() != null
                                                        ? risk.getRiskType().name()
                                                        : null
                                        )
                                        .riskLevel(
                                                risk.getRiskLevel() != null
                                                        ? risk.getRiskLevel().name()
                                                        : null
                                        )
                                        .riskScore(
                                                risk.getRiskScore()
                                        )
                                        .title(risk.getTitle())
                                        .description(
                                                risk.getDescription()
                                        )
                                        .recommendation(
                                                risk.getRecommendation()
                                        )
                                        .detectedAt(
                                                risk.getDetectedAt()
                                        )
                                        .validUntil(
                                                risk.getValidUntil()
                                        )
                                        .build()
                        )
                        .toList();


        return DigitalMemoryResponse.builder()

                .farmId(farm.getId())
                .farmName(farm.getName())
                .location(farm.getLocation())
                .generatedAt(
                        LocalDateTime.now()
                )

                .cropHistoryCount(
                        crops.size()
                )

                .soilSampleCount(
                        soil.size()
                )

                .irrigationEventCount(
                        irrigationEvents.size()
                )

                .irrigationTelemetryCount(
                        irrigationTelemetry.size()
                )

                .fertilizerRecommendationCount(
                        fertilizerRecommendations.size()
                )

                .fertilizerScheduleCount(
                        fertilizerSchedules.size()
                )

                .weatherRecordCount(
                        weather.size()
                )

                .nutrientBalanceCount(
                        nutrientBalances.size()
                )

                .weatherRiskCount(
                        weatherRisks.size()
                )

                .crops(crops)
                .soil(soil)
                .irrigationEvents(irrigationEvents)
                .irrigationTelemetry(irrigationTelemetry)
                .fertilizerRecommendations(
                        fertilizerRecommendations
                )
                .fertilizerSchedules(
                        fertilizerSchedules
                )
                .weather(weather)
                .nutrientBalances(
                        nutrientBalances
                )
                .weatherRisks(weatherRisks)

                /*
                 * Current operational data is a mixture of measured,
                 * calculated, estimated and test records.
                 */
                .dataStatus("MIXED")

                .source(
                        "AGRINEXUS_MEMBER_1_TO_5_HISTORY"
                )

                .uncertaintyNote(
                        "Digital Memory aggregates historical records "
                                + "stored by the AgriNexus backend. "
                                + "Records may contain measured, calculated, "
                                + "estimated or test data. Historical "
                                + "information is not itself a prediction."
                )

                .build();
    }
}