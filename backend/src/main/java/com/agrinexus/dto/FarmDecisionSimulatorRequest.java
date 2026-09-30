package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmDecisionSimulatorRequest {

    /*
     * Farm for which the scenario comparison
     * is required.
     */
    private UUID farmId;

    /*
     * Type of decision being compared.
     *
     * Example:
     * IRRIGATION
     * SOWING
     * TILLAGE
     */
    private String decisionType;

    /*
     * Current/action-now scenario.
     *
     * Example:
     * "Irrigate today"
     * "Sow today"
     */
    private String optionA;

    /*
     * Alternative scenario.
     *
     * Example:
     * "Wait for rainfall"
     * "Wait 3 days"
     */
    private String optionB;

    /*
     * Estimated water requirement for Option A,
     * when applicable.
     */
    private Double optionAWater;

    /*
     * Estimated water requirement for Option B,
     * when applicable.
     */
    private Double optionBWater;

    /*
     * Estimated energy requirement for Option A,
     * when applicable.
     */
    private Double optionAEnergy;

    /*
     * Estimated energy requirement for Option B,
     * when applicable.
     */
    private Double optionBEnergy;

    /*
     * Estimated cost for Option A.
     */
    private Double optionACost;

    /*
     * Estimated cost for Option B.
     */
    private Double optionBCost;

    /*
     * Expected timing/delay for Option A.
     */
    private String optionATiming;

    /*
     * Expected timing/delay for Option B.
     */
    private String optionBTiming;

    /*
     * Optional assumptions used in the comparison.
     */
    private String assumptions;
}