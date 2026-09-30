package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutrientBalanceResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /*
     * Nutrient inputs.
     */
    private Double nitrogenInput;

    private Double phosphorusInput;

    private Double potassiumInput;

    private Double sulphurInput;

    private Double zincInput;

    /*
     * Crop uptake / removal.
     */
    private Double nitrogenUptake;

    private Double phosphorusUptake;

    private Double potassiumUptake;

    private Double sulphurUptake;

    private Double zincUptake;

    /*
     * Residue return.
     */
    private Double nitrogenResidueReturn;

    private Double phosphorusResidueReturn;

    private Double potassiumResidueReturn;

    private Double sulphurResidueReturn;

    private Double zincResidueReturn;

    /*
     * Biological nitrogen fixation.
     */
    private Double nitrogenBiologicalFixation;

    /*
     * Calculated net balances.
     */
    private Double nitrogenBalance;

    private Double phosphorusBalance;

    private Double potassiumBalance;

    private Double sulphurBalance;

    private Double zincBalance;

    /*
     * Overall interpretation of the budget.
     */
    private String nitrogenStatus;

    private String phosphorusStatus;

    private String potassiumStatus;

    private String sulphurStatus;

    private String zincStatus;

    /*
     * Data provenance.
     */
    private String dataStatus;

    private String source;

    private String notes;
}