package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "crop_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm to which this crop history belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Crop that was grown during this cycle.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /*
     * Agricultural season.
     * Example: Rabi, Kharif, Summer
     */
    @Column(length = 50)
    private String season;

    /*
     * Date on which the crop was sown/planted.
     */
    private LocalDate sowingDate;

    /*
     * Date on which the crop was harvested.
     */
    private LocalDate harvestDate;

    /*
     * Crop growth stage.
     * Example: Germination, Vegetative, Flowering, Grain Filling
     */
    @Column(length = 100)
    private String cropStage;

    /*
     * Whether residue from this crop remains in the field.
     */
    @Column(nullable = false)
    private Boolean residuePresent;

    /*
     * Optional description of residue.
     * Example: Rice straw, wheat straw, maize stalk
     */
    @Column(length = 300)
    private String residueType;

    /*
     * Optional notes about this crop cycle.
     */
    @Column(length = 1000)
    private String notes;
}