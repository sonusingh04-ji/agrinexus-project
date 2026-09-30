package com.agrinexus.mapper;

import com.agrinexus.dto.FertilizerInputRequest;
import com.agrinexus.dto.FertilizerInputResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FertilizerInput;

public class FertilizerInputMapper {

    private FertilizerInputMapper() {
        // Utility class
    }

    /*
     * Convert request DTO to entity.
     */
    public static FertilizerInput toEntity(
            FertilizerInputRequest request,
            Farm farm,
            Crop crop
    ) {

        return FertilizerInput.builder()
                .farm(farm)
                .crop(crop)
                .applicationDate(
                        request.getApplicationDate()
                )
                .productName(
                        request.getProductName()
                )
                .inputType(
                        request.getInputType()
                )
                .quantity(
                        request.getQuantity()
                )
                .quantityUnit(
                        request.getQuantityUnit()
                )
                .nitrogen(
                        request.getNitrogen()
                )
                .phosphorus(
                        request.getPhosphorus()
                )
                .potassium(
                        request.getPotassium()
                )
                .sulphur(
                        request.getSulphur()
                )
                .zinc(
                        request.getZinc()
                )
                .source(
                        request.getSource()
                )
                .notes(
                        request.getNotes()
                )
                .build();
    }

    /*
     * Convert entity to response DTO.
     */
    public static FertilizerInputResponse toResponse(
            FertilizerInput input
    ) {

        return FertilizerInputResponse.builder()
                .id(
                        input.getId()
                )
                .farmId(
                        input.getFarm() != null
                                ? input.getFarm().getId()
                                : null
                )
                .cropId(
                        input.getCrop() != null
                                ? input.getCrop().getId()
                                : null
                )
                .cropName(
                        input.getCrop() != null
                                ? input.getCrop().getName()
                                : null
                )
                .applicationDate(
                        input.getApplicationDate()
                )
                .productName(
                        input.getProductName()
                )
                .inputType(
                        input.getInputType()
                )
                .quantity(
                        input.getQuantity()
                )
                .quantityUnit(
                        input.getQuantityUnit()
                )
                .nitrogen(
                        input.getNitrogen()
                )
                .phosphorus(
                        input.getPhosphorus()
                )
                .potassium(
                        input.getPotassium()
                )
                .sulphur(
                        input.getSulphur()
                )
                .zinc(
                        input.getZinc()
                )
                .source(
                        input.getSource()
                )
                .notes(
                        input.getNotes()
                )
                .build();
    }
}