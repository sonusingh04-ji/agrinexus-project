package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropWaterProfileResponse {

    private UUID id;
    private UUID cropId;
    private String cropName;

    private String stateName;
    private String districtName;
    private String season;

    private Double kcInitial;
    private Double kcDevelopment;
    private Double kcMid;
    private Double kcEnd;

    private Double rootDepthInitialCm;
    private Double rootDepthMidCm;
    private Double rootDepthEndCm;

    private Integer initialDays;
    private Integer developmentDays;
    private Integer midDays;
    private Integer lateDays;

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
