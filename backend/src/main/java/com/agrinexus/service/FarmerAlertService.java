package com.agrinexus.service;

import com.agrinexus.dto.FarmerAlertResponse;
import com.agrinexus.entity.CombinedRiskAnalysis;
import com.agrinexus.entity.FarmerAlert;
import com.agrinexus.entity.WeatherRisk;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.mapper.FarmerAlertMapper;
import com.agrinexus.repository.CombinedRiskAnalysisRepository;
import com.agrinexus.repository.FarmerAlertRepository;
import com.agrinexus.repository.WeatherRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FarmerAlertService {

    private final FarmerAlertRepository farmerAlertRepository;
    private final WeatherRiskRepository weatherRiskRepository;
    private final CombinedRiskAnalysisRepository combinedRiskAnalysisRepository;

    /*
     * Create a manual farmer alert.
     */
    public FarmerAlertResponse createAlert(
            FarmerAlert alert
    ) {

        FarmerAlert savedAlert =
                farmerAlertRepository.save(alert);

        return FarmerAlertMapper.toResponse(savedAlert);
    }

    /*
     * Get all alerts for a farm.
     */
    public List<FarmerAlertResponse> getFarmAlerts(
            UUID farmId
    ) {

        return farmerAlertRepository
                .findByFarmIdOrderByCreatedAtDesc(farmId)
                .stream()
                .map(FarmerAlertMapper::toResponse)
                .toList();
    }

    /*
     * Get unread alerts for a farm.
     */
    public List<FarmerAlertResponse> getUnreadAlerts(
            UUID farmId
    ) {

        return farmerAlertRepository
                .findByFarmIdAndIsReadFalseOrderByCreatedAtDesc(farmId)
                .stream()
                .map(FarmerAlertMapper::toResponse)
                .toList();
    }

    /*
     * Get alerts by risk level.
     */
    public List<FarmerAlertResponse> getAlertsByRiskLevel(
            UUID farmId,
            RiskLevel riskLevel
    ) {

        return farmerAlertRepository
                .findByFarmIdAndRiskLevelOrderByCreatedAtDesc(
                        farmId,
                        riskLevel
                )
                .stream()
                .map(FarmerAlertMapper::toResponse)
                .toList();
    }

    /*
     * Mark an alert as read.
     */
    public FarmerAlertResponse markAlertAsRead(
            UUID alertId
    ) {

        FarmerAlert alert =
                farmerAlertRepository
                        .findById(alertId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Alert not found"
                                )
                        );

        alert.setIsRead(true);

        FarmerAlert updatedAlert =
                farmerAlertRepository.save(alert);

        return FarmerAlertMapper.toResponse(updatedAlert);
    }

    /*
     * Generate alerts from active Weather Risks.
     *
     * Only HIGH and CRITICAL risks create alerts.
     * Duplicate active alerts are prevented.
     */
    public void generateWeatherRiskAlerts(
            UUID farmId
    ) {

        LocalDateTime currentTime =
                LocalDateTime.now();

        List<WeatherRisk> risks =
                weatherRiskRepository
                        .findByFarmIdAndValidUntilAfterOrderByDetectedAtDesc(
                                farmId,
                                currentTime
                        );

        for (WeatherRisk risk : risks) {

            if (risk.getRiskLevel() != RiskLevel.CRITICAL
                    && risk.getRiskLevel() != RiskLevel.HIGH) {

                continue;
            }

            boolean alreadyExists =
                    farmerAlertRepository
                            .existsActiveDuplicateAlert(
                                    farmId,
                                    risk.getTitle(),
                                    risk.getRiskLevel(),
                                    currentTime
                            );

            /*
             * If an active alert with the same
             * farm + title + risk level already exists,
             * do not create it again.
             */
            if (alreadyExists) {
                continue;
            }

            FarmerAlert alert =
                    FarmerAlert.builder()
                            .farm(risk.getFarm())
                            .riskLevel(
                                    risk.getRiskLevel()
                            )
                            .title(
                                    risk.getTitle()
                            )
                            .message(
                                    risk.getDescription()
                            )
                            .recommendation(
                                    risk.getRecommendation()
                            )
                            .isRead(false)
                            .validUntil(
                                    risk.getValidUntil()
                            )
                            .build();

            farmerAlertRepository.save(alert);
        }
    }

    /*
     * Generate an alert from the latest
     * Combined Risk Analysis.
     *
     * Only HIGH and CRITICAL levels create alerts.
     * Duplicate active alerts are prevented.
     */
    public void generateCombinedRiskAlert(
            UUID farmId
    ) {

        CombinedRiskAnalysis combinedRisk =
                combinedRiskAnalysisRepository
                        .findByFarmIdOrderByAnalyzedAtDesc(farmId)
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (combinedRisk == null) {
            return;
        }

        if (combinedRisk.getRiskLevel() != RiskLevel.CRITICAL
                && combinedRisk.getRiskLevel() != RiskLevel.HIGH) {

            return;
        }

        LocalDateTime currentTime =
                LocalDateTime.now();

        boolean alreadyExists =
                farmerAlertRepository
                        .existsActiveDuplicateAlert(
                                farmId,
                                combinedRisk.getTitle(),
                                combinedRisk.getRiskLevel(),
                                currentTime
                        );

        /*
         * If the same active combined-risk alert
         * already exists, do not create another one.
         */
        if (alreadyExists) {
            return;
        }

        FarmerAlert alert =
                FarmerAlert.builder()
                        .farm(
                                combinedRisk.getFarm()
                        )
                        .riskLevel(
                                combinedRisk.getRiskLevel()
                        )
                        .title(
                                combinedRisk.getTitle()
                        )
                        .message(
                                combinedRisk.getAnalysis()
                        )
                        .recommendation(
                                combinedRisk.getRecommendation()
                        )
                        .isRead(false)
                        .validUntil(
                                currentTime.plusHours(24)
                        )
                        .build();

        farmerAlertRepository.save(alert);
    }

    /*
     * Generate all important alerts for a farm.
     */
    public void generateAllAlerts(
            UUID farmId
    ) {

        generateWeatherRiskAlerts(farmId);

        generateCombinedRiskAlert(farmId);
    }
    public long deleteAllFarmAlerts(
            UUID farmId
    ) {

        return farmerAlertRepository
                .deleteByFarmId(farmId);
    }
}