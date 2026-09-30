package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FertilizerReferenceResponse {

    private UUID id;

    private UUID cropId;

    private String cropName;

    private String region;

    private String varietyOrCondition;

    private String applicationStage;

    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double sulphur;

    private Double zinc;

    private String unit;

    private String sourceOrganization;

    private String sourceDocument;

    private String sourceUrl;

    private LocalDate verifiedDate;

    private String dataStatus;

    private String notes;
}