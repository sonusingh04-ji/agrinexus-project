package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropHistoryResponse {

    private UUID id;

    private UUID farmId;

    private UUID cropId;

    private String cropName;

    private String season;

    private LocalDate sowingDate;

    private LocalDate harvestDate;

    private String cropStage;

    private Boolean residuePresent;

    private String residueType;

    private String notes;
}