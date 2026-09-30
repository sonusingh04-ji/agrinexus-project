package com.agrinexus.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropRequest {

    private String name;

    private String category;

    private String season;

    private String sowingWindow;

    private Double idealTemperatureMin;

    private Double idealTemperatureMax;

    private String soilType;

    private Double soilPhMin;

    private Double soilPhMax;

    private String seedRate;

    private String rowSpacing;

    private String sowingDepth;

    private String waterNeed;

    private String criticalWaterSensitiveStages;

    private String commonRotation;
}