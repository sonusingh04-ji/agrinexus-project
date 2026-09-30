package com.agrinexus.mapper;

import com.agrinexus.dto.FarmRequest;
import com.agrinexus.dto.FarmResponse;
import com.agrinexus.entity.Farm;

public class FarmMapper {

    private FarmMapper() {
    }

    public static Farm toEntity(
            FarmRequest request
    ) {

        return Farm.builder()
                .farmerId(request.getFarmerId())
                .name(request.getName())
                .areaAcres(request.getAreaAcres())
                .location(request.getLocation())
                .stateName(request.getStateName())
                .districtName(request.getDistrictName())
                .subDistrictName(request.getSubDistrictName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
    }


    public static FarmResponse toResponse(
            Farm farm
    ) {

        return FarmResponse.builder()
                .id(farm.getId())
                .farmerId(farm.getFarmerId())
                .name(farm.getName())
                .areaAcres(farm.getAreaAcres())
                .location(farm.getLocation())
                .stateName(farm.getStateName())
                .districtName(farm.getDistrictName())
                .subDistrictName(farm.getSubDistrictName())
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .createdAt(farm.getCreatedAt())
                .build();
    }
}