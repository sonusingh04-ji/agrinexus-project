package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIContextRequest {

    /*
     * Farm for which AI context is required.
     */
    private UUID farmId;

    /*
     * Optional conversation.
     */
    private UUID conversationId;

    /*
     * Optional AI message.
     */
    private UUID messageId;

    /*
     * Whether to include the latest available
     * decision-engine data.
     */
    private Boolean includeLatestData;

    /*
     * Optional human-readable purpose.
     *
     * Examples:
     * COPILOT
     * EXPLANATION
     * VOICE
     * IVR
     */
    private String purpose;

    /*
     * =========================================================
     * Member 6 — Lowest-Cost Safe Plan inputs
     * =========================================================
     */

    /*
     * Target crop season.
     *
     * Examples:
     * RABI
     * KHARIF
     * SUMMER
     */
    private String targetSeason;

    /*
     * Farmer planning constraint.
     *
     * Examples:
     * LOW_COST
     * LOW_WATER
     * LOW_ENERGY
     * SHORT_DURATION
     * NONE
     */
    private String farmerConstraint;

    /*
     * Whether soil compatibility must be enforced
     * as a safety gate.
     */
    private Boolean requireSoilCompatibility;

    /*
     * Optional farm-specific available water budget.
     *
     * Unit: mm
     */
    private Double availableWaterMm;
}