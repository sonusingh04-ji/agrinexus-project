package com.agrinexus.service;

import com.agrinexus.dto.FarmDecisionSimulatorRequest;
import com.agrinexus.dto.FarmDecisionSimulatorResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FarmDecisionSimulatorService {

    private final FarmRepository farmRepository;

    /*
     * Compare two farmer-provided scenarios.
     *
     * This is an explainable MVP.
     *
     * The service does NOT invent water, energy, cost,
     * rainfall, or savings values. Those values must be
     * supplied by the caller or produced by another
     * validated module.
     */
    public FarmDecisionSimulatorResponse simulateDecision(
            FarmDecisionSimulatorRequest request
    ) {

        /*
         * ---------------------------------------------------------
         * 1. Validate required data
         * ---------------------------------------------------------
         */
        if (request.getFarmId() == null) {
            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getDecisionType() == null
                || request.getDecisionType().isBlank()) {
            throw new RuntimeException(
                    "Decision type is required"
            );
        }

        if (request.getOptionA() == null
                || request.getOptionA().isBlank()) {
            throw new RuntimeException(
                    "Option A is required"
            );
        }

        if (request.getOptionB() == null
                || request.getOptionB().isBlank()) {
            throw new RuntimeException(
                    "Option B is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * 2. Verify farm
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
         * 3. Calculate resource differences
         *
         * Difference = Option A - Option B
         * ---------------------------------------------------------
         */
        Double waterDifference =
                calculateDifference(
                        request.getOptionAWater(),
                        request.getOptionBWater()
                );

        Double energyDifference =
                calculateDifference(
                        request.getOptionAEnergy(),
                        request.getOptionBEnergy()
                );

        Double costDifference =
                calculateDifference(
                        request.getOptionACost(),
                        request.getOptionBCost()
                );

        /*
         * ---------------------------------------------------------
         * 4. Determine water-efficient option
         * ---------------------------------------------------------
         */
        String waterEfficientOption =
                determineLowerValueOption(
                        request.getOptionA(),
                        request.getOptionB(),
                        request.getOptionAWater(),
                        request.getOptionBWater()
                );

        /*
         * ---------------------------------------------------------
         * 5. Determine energy-efficient option
         * ---------------------------------------------------------
         */
        String energyEfficientOption =
                determineLowerValueOption(
                        request.getOptionA(),
                        request.getOptionB(),
                        request.getOptionAEnergy(),
                        request.getOptionBEnergy()
                );

        /*
         * ---------------------------------------------------------
         * 6. Determine cheaper option
         * ---------------------------------------------------------
         */
        String costEfficientOption =
                determineLowerValueOption(
                        request.getOptionA(),
                        request.getOptionB(),
                        request.getOptionACost(),
                        request.getOptionBCost()
                );

        /*
         * ---------------------------------------------------------
         * 7. Timing comparison
         * ---------------------------------------------------------
         */
        String timingComparison =
                buildTimingComparison(
                        request.getOptionA(),
                        request.getOptionB(),
                        request.getOptionATiming(),
                        request.getOptionBTiming()
                );

        /*
         * ---------------------------------------------------------
         * 8. Build overall recommendation
         * ---------------------------------------------------------
         */
        String recommendation =
                buildRecommendation(
                        request,
                        waterEfficientOption,
                        energyEfficientOption,
                        costEfficientOption
                );

        /*
         * ---------------------------------------------------------
         * 9. Assumptions
         *
         * If caller doesn't provide assumptions,
         * clearly state that values are caller supplied.
         * ---------------------------------------------------------
         */
        String assumptions =
                request.getAssumptions();

        if (assumptions == null
                || assumptions.isBlank()) {

            assumptions =
                    "Water, energy and cost values are "
                            + "user-provided estimates. "
                            + "Results depend on the assumptions "
                            + "used for each scenario.";
        }

        /*
         * ---------------------------------------------------------
         * 10. Build response
         * ---------------------------------------------------------
         */
        return FarmDecisionSimulatorResponse.builder()
                .farmId(farm.getId())
                .decisionType(
                        request.getDecisionType()
                )
                .optionA(
                        request.getOptionA()
                )
                .optionB(
                        request.getOptionB()
                )
                .waterDifference(
                        waterDifference
                )
                .energyDifference(
                        energyDifference
                )
                .costDifference(
                        costDifference
                )
                .waterEfficientOption(
                        waterEfficientOption
                )
                .energyEfficientOption(
                        energyEfficientOption
                )
                .costEfficientOption(
                        costEfficientOption
                )
                .timingComparison(
                        timingComparison
                )
                .recommendation(
                        recommendation
                )
                .assumptions(
                        assumptions
                )
                .build();
    }


    /*
     * Calculate Option A - Option B.
     *
     * Returns null when either input is missing.
     */
    private Double calculateDifference(
            Double optionA,
            Double optionB
    ) {

        if (optionA == null
                || optionB == null) {

            return null;
        }

        return optionA - optionB;
    }


    /*
     * Determine which option requires less of a resource.
     */
    private String determineLowerValueOption(
            String optionAName,
            String optionBName,
            Double optionAValue,
            Double optionBValue
    ) {

        if (optionAValue == null
                || optionBValue == null) {

            return "NOT_AVAILABLE";
        }

        if (optionAValue < optionBValue) {
            return optionAName;
        }

        if (optionBValue < optionAValue) {
            return optionBName;
        }

        return "EQUAL";
    }


    /*
     * Build a clear timing comparison.
     */
    private String buildTimingComparison(
            String optionA,
            String optionB,
            String optionATiming,
            String optionBTiming
    ) {

        String timingA =
                optionATiming != null
                        && !optionATiming.isBlank()
                        ? optionATiming
                        : "Not specified";

        String timingB =
                optionBTiming != null
                        && !optionBTiming.isBlank()
                        ? optionBTiming
                        : "Not specified";

        return optionA
                + " → "
                + timingA
                + "; "
                + optionB
                + " → "
                + timingB
                + ".";
    }


    /*
     * Build an overall comparison message.
     */
    private String buildRecommendation(
            FarmDecisionSimulatorRequest request,
            String waterEfficientOption,
            String energyEfficientOption,
            String costEfficientOption
    ) {

        StringBuilder recommendation =
                new StringBuilder();

        recommendation.append(
                "Scenario comparison completed for "
        );

        recommendation.append(
                request.getDecisionType()
        );

        recommendation.append(". ");

        boolean hasComparison = false;

        if (!"NOT_AVAILABLE"
                .equals(waterEfficientOption)
                && !"EQUAL".equals(waterEfficientOption)) {

            recommendation.append(
                    "Water-efficient option: "
            );

            recommendation.append(
                    waterEfficientOption
            );

            recommendation.append(". ");

            hasComparison = true;
        }

        if (!"NOT_AVAILABLE"
                .equals(energyEfficientOption)
                && !"EQUAL".equals(energyEfficientOption)) {

            recommendation.append(
                    "Energy-efficient option: "
            );

            recommendation.append(
                    energyEfficientOption
            );

            recommendation.append(". ");

            hasComparison = true;
        }

        if (!"NOT_AVAILABLE"
                .equals(costEfficientOption)
                && !"EQUAL".equals(costEfficientOption)) {

            recommendation.append(
                    "Lower-cost option: "
            );

            recommendation.append(
                    costEfficientOption
            );

            recommendation.append(". ");

            hasComparison = true;
        }

        if (!hasComparison) {

            recommendation.append(
                    "There is not enough supplied data "
                            + "to identify a single better option."
            );
        }

        recommendation.append(
                " Review the stated assumptions before "
                        + "making the final farm decision."
        );

        return recommendation.toString();
    }
}