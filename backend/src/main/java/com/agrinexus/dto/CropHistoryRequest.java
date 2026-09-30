package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropHistoryRequest {

    /*
     * Farm where this crop was grown.
     */
    private UUID farmId;

    /*
     * Crop reference from the crops table.
     */
    private UUID cropId;

    /*
     * Agricultural season.
     * Example: Rabi, Kharif, Summer
     */
    private String season;

    /*
     * Crop sowing/planting date.
     */
    private LocalDate sowingDate;

    /*
     * Crop harvest date.
     */
    private LocalDate harvestDate;

    /*
     * Current or recorded crop growth stage.
     */
    private String cropStage;

    /*
     * Whether crop residue remains in the field.
     */
    private Boolean residuePresent;

    /*
     * Type of residue.
     * Example: Rice straw, wheat straw, maize stalk
     */
    private String residueType;

    /*
     * Additional notes for this crop cycle.
     */
    private String notes;
}