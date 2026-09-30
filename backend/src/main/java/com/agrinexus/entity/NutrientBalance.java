package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nutrient_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutrientBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm for which this nutrient budget is maintained.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop/season associated with this budget.
     *
     * Optional because a balance can also represent
     * a broader farm-level period.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /*
     * Budget period.
     */
    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    /*
     * ---------------------------------------------------------
     * Nutrient inputs
     *
     * Values are nutrient amounts contributed during
     * the selected budget period.
     * ---------------------------------------------------------
     */

    private Double nitrogenInput;

    private Double phosphorusInput;

    private Double potassiumInput;

    private Double sulphurInput;

    private Double zincInput;

    /*
     * ---------------------------------------------------------
     * Crop uptake / removal
     *
     * These are recorded or estimated nutrient removals.
     * ---------------------------------------------------------
     */

    private Double nitrogenUptake;

    private Double phosphorusUptake;

    private Double potassiumUptake;

    private Double sulphurUptake;

    private Double zincUptake;

    /*
     * ---------------------------------------------------------
     * Other nutrient-flow components
     *
     * Kept separate so the calculation remains explainable.
     * ---------------------------------------------------------
     */

    private Double nitrogenResidueReturn;

    private Double phosphorusResidueReturn;

    private Double potassiumResidueReturn;

    private Double sulphurResidueReturn;

    private Double zincResidueReturn;

    private Double nitrogenBiologicalFixation;

    /*
     * ---------------------------------------------------------
     * Calculated net balance
     *
     * Net balance is derived from the stored components.
     * ---------------------------------------------------------
     */

    private Double nitrogenBalance;

    private Double phosphorusBalance;

    private Double potassiumBalance;

    private Double sulphurBalance;

    private Double zincBalance;

    /*
     * ---------------------------------------------------------
     * Provenance / calculation information
     * ---------------------------------------------------------
     */

    /*
     * Example:
     * MEASURED
     * ESTIMATED
     * MIXED
     */
    @Column(length = 30)
    private String dataStatus;

    /*
     * Source of the balance calculation.
     */
    @Column(length = 100)
    private String source;

    /*
     * Notes explaining assumptions,
     * missing values, or calculation context.
     */
    @Column(length = 2000)
    private String notes;
}