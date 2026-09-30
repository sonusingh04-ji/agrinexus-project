package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmDecisionSimulatorResponse {

    private UUID farmId;

    private String decisionType;

    private String optionA;

    private String optionB;

    /*
     * Difference in water usage:
     * Option A - Option B
     */
    private Double waterDifference;

    /*
     * Difference in energy usage:
     * Option A - Option B
     */
    private Double energyDifference;

    /*
     * Difference in cost:
     * Option A - Option B
     */
    private Double costDifference;

    /*
     * Which option uses less water.
     */
    private String waterEfficientOption;

    /*
     * Which option uses less energy.
     */
    private String energyEfficientOption;

    /*
     * Which option is cheaper.
     */
    private String costEfficientOption;

    /*
     * Timing comparison.
     */
    private String timingComparison;

    /*
     * Overall comparison.
     */
    private String recommendation;

    /*
     * Assumptions used by the simulator.
     */
    private String assumptions;
}