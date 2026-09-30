package com.agrinexus.mapper;

import com.agrinexus.dto.CropRequest;
import com.agrinexus.dto.CropResponse;
import com.agrinexus.entity.Crop;

public class CropMapper {

    private CropMapper() {
        // Utility class
    }

    public static Crop toEntity(
            CropRequest request
    ) {

        return Crop.builder()
                .name(request.getName())
                .category(request.getCategory())
                .season(request.getSeason())
                .sowingWindow(request.getSowingWindow())
                .idealTemperatureMin(
                        request.getIdealTemperatureMin()
                )
                .idealTemperatureMax(
                        request.getIdealTemperatureMax()
                )
                .soilType(request.getSoilType())
                .soilPhMin(request.getSoilPhMin())
                .soilPhMax(request.getSoilPhMax())
                .seedRate(request.getSeedRate())
                .rowSpacing(request.getRowSpacing())
                .sowingDepth(request.getSowingDepth())
                .waterNeed(request.getWaterNeed())
                .criticalWaterSensitiveStages(
                        request.getCriticalWaterSensitiveStages()
                )
                .commonRotation(
                        request.getCommonRotation()
                )
                .build();
    }

    public static CropResponse toResponse(
            Crop crop
    ) {

        return CropResponse.builder()
                .id(crop.getId())
                .name(crop.getName())
                .category(crop.getCategory())
                .season(crop.getSeason())
                .sowingWindow(crop.getSowingWindow())
                .idealTemperatureMin(
                        crop.getIdealTemperatureMin()
                )
                .idealTemperatureMax(
                        crop.getIdealTemperatureMax()
                )
                .soilType(crop.getSoilType())
                .soilPhMin(crop.getSoilPhMin())
                .soilPhMax(crop.getSoilPhMax())
                .seedRate(crop.getSeedRate())
                .rowSpacing(crop.getRowSpacing())
                .sowingDepth(crop.getSowingDepth())
                .waterNeed(crop.getWaterNeed())
                .criticalWaterSensitiveStages(
                        crop.getCriticalWaterSensitiveStages()
                )
                .commonRotation(
                        crop.getCommonRotation()
                )
                .build();
    }
}