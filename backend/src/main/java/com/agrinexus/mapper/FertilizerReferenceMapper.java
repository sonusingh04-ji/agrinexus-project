package com.agrinexus.mapper;

import com.agrinexus.dto.FertilizerReferenceResponse;
import com.agrinexus.entity.FertilizerReference;

public final class FertilizerReferenceMapper {

    private FertilizerReferenceMapper() {
    }

    public static FertilizerReferenceResponse toResponse(
            FertilizerReference reference
    ) {

        if (reference == null) {
            return null;
        }

        return FertilizerReferenceResponse.builder()
                .id(reference.getId())

                .cropId(
                        reference.getCrop() != null
                                ? reference.getCrop().getId()
                                : null
                )

                .cropName(
                        reference.getCrop() != null
                                ? reference.getCrop().getName()
                                : null
                )

                .region(reference.getRegion())
                .varietyOrCondition(reference.getVarietyOrCondition())
                .applicationStage(reference.getApplicationStage())

                .nitrogen(reference.getNitrogen())
                .phosphorus(reference.getPhosphorus())
                .potassium(reference.getPotassium())
                .sulphur(reference.getSulphur())
                .zinc(reference.getZinc())

                .unit(reference.getUnit())

                .sourceOrganization(
                        reference.getSourceOrganization()
                )

                .sourceDocument(
                        reference.getSourceDocument()
                )

                .sourceUrl(
                        reference.getSourceUrl()
                )

                .verifiedDate(
                        reference.getVerifiedDate()
                )

                .dataStatus(
                        reference.getDataStatus()
                )

                .notes(
                        reference.getNotes()
                )

                .build();
    }
}