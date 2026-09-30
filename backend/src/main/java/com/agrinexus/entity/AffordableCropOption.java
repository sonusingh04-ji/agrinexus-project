package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "affordable_crop_options",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_affordable_crop_option_crop",
                        columnNames = "crop_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffordableCropOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Existing Crop master record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /*
     * =========================================================
     * Water
     * =========================================================
     */

    private Double waterRequirementMmMin;

    private Double waterRequirementMmMax;

    @Column(length = 30)
    private String waterDemandClass;

    /*
     * =========================================================
     * Crop duration
     * =========================================================
     */

    private Integer cropDurationDaysMin;

    private Integer cropDurationDaysMax;

    /*
     * =========================================================
     * Costs
     * =========================================================
     */

    private Double estimatedInputCostInrPerHa;

    private Double estimatedEnergyFuelCostInrPerHa;

    private Double estimatedFertilizerPesticideCostInrPerHa;

    /*
     * =========================================================
     * Safety / decision guidance
     * =========================================================
     */

    @Column(length = 1500)
    private String agronomicSafetyTradeoff;

    @Column(length = 2500)
    private String recommendationRule;

    /*
     * Example:
     * Planning estimate; calibrate with local KVK/state-agriculture data
     */
    @Column(length = 500)
    private String dataStatus;

    @Column(length = 300)
    private String source;

    @Column(length = 2000)
    private String notes;
}