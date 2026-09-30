package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmDecisionSimulationRequest {

    /*
     * Farm being simulated.
     */
    private UUID farmId;

    /*
     * Crop associated with the scenario.
     *
     * Optional.
     */
    private UUID cropId;

    /*
     * First scenario.
     *
     * Example:
     * IRRIGATE_NOW
     */
    private String scenarioA;

    /*
     * Comparison scenario.
     *
     * Example:
     * WAIT_FOR_RAIN
     */
    private String scenarioB;
}