package com.agrinexus.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowestCostSafePlanResponse {

    private UUID farmId;

    private String farmName;

    private String targetSeason;

    private String farmerConstraint;

    private Double soilPh;

    private String soilType;

    private String recommendedCrop;

    private Double recommendationScore;

    private Double estimatedTotalCostInrPerHa;

    private Double estimatedInputCostInrPerHa;

    private Double estimatedEnergyFuelCostInrPerHa;

    private Double estimatedFertilizerPesticideCostInrPerHa;

    private Double waterRequirementMmMin;

    private Double waterRequirementMmMax;

    private String waterDemandClass;

    private Integer cropDurationDaysMin;

    private Integer cropDurationDaysMax;

    private String safetyTradeoff;

    private String reason;

    private String recommendation;

    private String dataStatus;

    private String source;

    private String uncertaintyNote;

    private List<CropOption> rankedOptions;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CropOption {

        private UUID cropId;

        private String cropName;

        private String category;

        private String season;

        private Double recommendationScore;

        private Double totalCostInrPerHa;

        private Double inputCostInrPerHa;

        private Double energyFuelCostInrPerHa;

        private Double fertilizerPesticideCostInrPerHa;

        private Double waterRequirementMmMin;

        private Double waterRequirementMmMax;

        private Integer durationDaysMin;

        private Integer durationDaysMax;

        private String waterDemandClass;

        private Boolean soilCompatible;

        private Boolean waterCompatible;

        private Boolean seasonCompatible;

        private String safetyTradeoff;

        private String dataStatus;
    }
}