package com.agrinexus.service;

import com.agrinexus.dto.RiskAnalyticsResponse;
import com.agrinexus.entity.CombinedRiskAnalysis;
import com.agrinexus.entity.Farm;
import com.agrinexus.enums.RiskLevel;
import com.agrinexus.repository.CombinedRiskAnalysisRepository;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskAnalyticsService {

    private final FarmRepository farmRepository;
    private final CombinedRiskAnalysisRepository
            combinedRiskAnalysisRepository;


    public RiskAnalyticsResponse getFarmRiskAnalytics(
            UUID farmId
    ) {

        /*
         * 1. Get Farm
         */
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() ->
                        new RuntimeException("Farm not found")
                );


        /*
         * 2. Get complete risk history
         */
        List<CombinedRiskAnalysis> analyses =
                combinedRiskAnalysisRepository
                        .findByFarmIdOrderByAnalyzedAtDesc(farmId);


        /*
         * If no analysis exists
         */
        if (analyses.isEmpty()) {

            return RiskAnalyticsResponse.builder()
                    .farmId(farm.getId())
                    .farmName(farm.getName())
                    .currentRiskScore(0.0)
                    .currentRiskLevel(RiskLevel.LOW)
                    .averageRiskScore(0.0)
                    .highestRiskScore(0.0)
                    .lowestRiskScore(0.0)
                    .totalAnalyses(0)
                    .criticalRiskCount(0L)
                    .highRiskCount(0L)
                    .mediumRiskCount(0L)
                    .lowRiskCount(0L)
                    .riskHistory(List.of())
                    .lastAnalyzedAt(null)
                    .build();
        }


        /*
         * 3. Latest analysis
         *
         * Repository returns newest first.
         */
        CombinedRiskAnalysis latestAnalysis =
                analyses.getFirst();


        /*
         * 4. Calculate statistics
         */
        double averageRiskScore =
                analyses.stream()
                        .mapToDouble(
                                analysis -> analysis
                                        .getCombinedRiskScore()
                        )
                        .average()
                        .orElse(0.0);


        double highestRiskScore =
                analyses.stream()
                        .mapToDouble(
                                analysis -> analysis
                                        .getCombinedRiskScore()
                        )
                        .max()
                        .orElse(0.0);


        double lowestRiskScore =
                analyses.stream()
                        .mapToDouble(
                                analysis -> analysis
                                        .getCombinedRiskScore()
                        )
                        .min()
                        .orElse(0.0);


        /*
         * 5. Count risk levels
         */
        long criticalRiskCount =
                analyses.stream()
                        .filter(analysis ->
                                analysis.getRiskLevel()
                                        == RiskLevel.CRITICAL
                        )
                        .count();


        long highRiskCount =
                analyses.stream()
                        .filter(analysis ->
                                analysis.getRiskLevel()
                                        == RiskLevel.HIGH
                        )
                        .count();


        long mediumRiskCount =
                analyses.stream()
                        .filter(analysis ->
                                analysis.getRiskLevel()
                                        == RiskLevel.MEDIUM
                        )
                        .count();


        long lowRiskCount =
                analyses.stream()
                        .filter(analysis ->
                                analysis.getRiskLevel()
                                        == RiskLevel.LOW
                        )
                        .count();


        /*
         * 6. Build historical chart data
         *
         * Sorted from oldest → newest,
         * which is better for frontend graphs.
         */
        List<RiskAnalyticsResponse.RiskHistoryPoint>
                riskHistory =
                analyses.stream()
                        .sorted(
                                Comparator.comparing(
                                        CombinedRiskAnalysis
                                                ::getAnalyzedAt
                                )
                        )
                        .map(analysis ->
                                RiskAnalyticsResponse
                                        .RiskHistoryPoint
                                        .builder()
                                        .analysisId(
                                                analysis.getId()
                                        )
                                        .riskScore(
                                                analysis
                                                        .getCombinedRiskScore()
                                        )
                                        .riskLevel(
                                                analysis.getRiskLevel()
                                        )
                                        .analyzedAt(
                                                analysis.getAnalyzedAt()
                                        )
                                        .build()
                        )
                        .toList();


        /*
         * 7. Build final response
         */
        return RiskAnalyticsResponse.builder()

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .currentRiskScore(
                        latestAnalysis.getCombinedRiskScore()
                )

                .currentRiskLevel(
                        latestAnalysis.getRiskLevel()
                )

                .averageRiskScore(
                        averageRiskScore
                )

                .highestRiskScore(
                        highestRiskScore
                )

                .lowestRiskScore(
                        lowestRiskScore
                )

                .totalAnalyses(
                        analyses.size()
                )

                .criticalRiskCount(
                        criticalRiskCount
                )

                .highRiskCount(
                        highRiskCount
                )

                .mediumRiskCount(
                        mediumRiskCount
                )

                .lowRiskCount(
                        lowRiskCount
                )

                .riskHistory(
                        riskHistory
                )

                .lastAnalyzedAt(
                        latestAnalysis.getAnalyzedAt()
                )

                .build();
    }
}