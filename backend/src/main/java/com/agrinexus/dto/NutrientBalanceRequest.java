package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutrientBalanceRequest {

    /*
     * Farm for which the nutrient balance
     * should be calculated/stored.
     */
    private UUID farmId;

    /*
     * Optional crop associated with this
     * nutrient budget.
     */
    private UUID cropId;

    /*
     * Budget period.
     */
    private LocalDate periodStart;

    private LocalDate periodEnd;

    /*
     * Nutrient inputs during the budget period.
     */
    private Double nitrogenInput;

    private Double phosphorusInput;

    private Double potassiumInput;

    private Double sulphurInput;

    private Double zincInput;

    /*
     * Crop nutrient uptake / removal.
     *
     * These values must be measured, sourced,
     * or explicitly treated as estimates.
     */
    private Double nitrogenUptake;

    private Double phosphorusUptake;

    private Double potassiumUptake;

    private Double sulphurUptake;

    private Double zincUptake;

    /*
     * Nutrient returned through retained/returned residue.
     */
    private Double nitrogenResidueReturn;

    private Double phosphorusResidueReturn;

    private Double potassiumResidueReturn;

    private Double sulphurResidueReturn;

    private Double zincResidueReturn;

    /*
     * Biological nitrogen fixation, where applicable.
     */
    private Double nitrogenBiologicalFixation;

    /*
     * Provenance/status of the data.
     *
     * Examples:
     * MEASURED
     * ESTIMATED
     * MIXED
     */
    private String dataStatus;

    /*
     * Source/reference of the calculation or data.
     */
    private String source;

    /*
     * Explanation, assumptions, or notes.
     */
    private String notes;
}