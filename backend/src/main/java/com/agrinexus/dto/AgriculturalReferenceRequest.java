package com.agrinexus.dto;

import com.agrinexus.enums.AgriculturalReferenceType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgriculturalReferenceRequest {

    @NotNull(message = "Crop ID is required")
    private UUID cropId;

    @NotNull(message = "Reference type is required")
    private AgriculturalReferenceType referenceType;

    private String stateName;
    private String districtName;
    private String subDistrictName;
    private String season;
    private String cropStage;

    private Double nitrogen;
    private Double phosphorus;
    private Double potassium;
    private Double sulphur;
    private Double zinc;

    private Double kcInitial;
    private Double kcDevelopment;
    private Double kcMid;
    private Double kcEnd;

    private Double rootDepthInitialCm;
    private Double rootDepthMidCm;
    private Double rootDepthEndCm;

    private Double soilPhMin;
    private Double soilPhMax;
    private Double temperatureMin;
    private Double temperatureMax;

    private String waterNeed;
    private String sowingWindow;
    private String seedRate;
    private String rowSpacing;
    private String sowingDepth;
    private String criticalWaterSensitiveStages;
    private String applicableConditions;

    private String source;
    private String sourceDocument;
    private String sourceUrl;
    private Integer publicationYear;
    private String dataStatus;
    private String version;
    private String validFrom;
    private String validTo;
    private String notes;
}
