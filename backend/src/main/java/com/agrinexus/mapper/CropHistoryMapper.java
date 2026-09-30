package com.agrinexus.mapper;

import com.agrinexus.dto.CropHistoryRequest;
import com.agrinexus.dto.CropHistoryResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;

public class CropHistoryMapper {

    private CropHistoryMapper() {
        // Utility class
    }

    public static CropHistory toEntity(
            CropHistoryRequest request,
            Farm farm,
            Crop crop
    ) {

        return CropHistory.builder()
                .farm(farm)
                .crop(crop)
                .season(request.getSeason())
                .sowingDate(request.getSowingDate())
                .harvestDate(request.getHarvestDate())
                .cropStage(request.getCropStage())
                .residuePresent(
                        request.getResiduePresent() != null
                                ? request.getResiduePresent()
                                : false
                )
                .residueType(request.getResidueType())
                .notes(request.getNotes())
                .build();
    }

    public static CropHistoryResponse toResponse(
            CropHistory history
    ) {

        return CropHistoryResponse.builder()
                .id(history.getId())
                .farmId(
                        history.getFarm() != null
                                ? history.getFarm().getId()
                                : null
                )
                .cropId(
                        history.getCrop() != null
                                ? history.getCrop().getId()
                                : null
                )
                .cropName(
                        history.getCrop() != null
                                ? history.getCrop().getName()
                                : null
                )
                .season(history.getSeason())
                .sowingDate(history.getSowingDate())
                .harvestDate(history.getHarvestDate())
                .cropStage(history.getCropStage())
                .residuePresent(history.getResiduePresent())
                .residueType(history.getResidueType())
                .notes(history.getNotes())
                .build();
    }
}