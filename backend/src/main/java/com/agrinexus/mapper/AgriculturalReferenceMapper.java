package com.agrinexus.mapper;

import com.agrinexus.dto.AgriculturalReferenceRequest;
import com.agrinexus.dto.AgriculturalReferenceResponse;
import com.agrinexus.entity.AgriculturalReference;
import com.agrinexus.entity.Crop;

public final class AgriculturalReferenceMapper {

    private AgriculturalReferenceMapper() {
    }

    public static AgriculturalReference toEntity(
            Crop crop,
            AgriculturalReferenceRequest request
    ) {
        AgriculturalReference reference = new AgriculturalReference();
        reference.setCrop(crop);
        apply(reference, request);
        return reference;
    }

    public static void apply(
            AgriculturalReference reference,
            AgriculturalReferenceRequest request
    ) {
        reference.setReferenceType(request.getReferenceType());
        reference.setStateName(clean(request.getStateName()));
        reference.setDistrictName(clean(request.getDistrictName()));
        reference.setSubDistrictName(clean(request.getSubDistrictName()));
        reference.setSeason(clean(request.getSeason()));
        reference.setCropStage(clean(request.getCropStage()));

        reference.setNitrogen(request.getNitrogen());
        reference.setPhosphorus(request.getPhosphorus());
        reference.setPotassium(request.getPotassium());
        reference.setSulphur(request.getSulphur());
        reference.setZinc(request.getZinc());

        reference.setKcInitial(request.getKcInitial());
        reference.setKcDevelopment(request.getKcDevelopment());
        reference.setKcMid(request.getKcMid());
        reference.setKcEnd(request.getKcEnd());

        reference.setRootDepthInitialCm(request.getRootDepthInitialCm());
        reference.setRootDepthMidCm(request.getRootDepthMidCm());
        reference.setRootDepthEndCm(request.getRootDepthEndCm());

        reference.setSoilPhMin(request.getSoilPhMin());
        reference.setSoilPhMax(request.getSoilPhMax());
        reference.setTemperatureMin(request.getTemperatureMin());
        reference.setTemperatureMax(request.getTemperatureMax());

        reference.setWaterNeed(clean(request.getWaterNeed()));
        reference.setSowingWindow(clean(request.getSowingWindow()));
        reference.setSeedRate(clean(request.getSeedRate()));
        reference.setRowSpacing(clean(request.getRowSpacing()));
        reference.setSowingDepth(clean(request.getSowingDepth()));
        reference.setCriticalWaterSensitiveStages(clean(request.getCriticalWaterSensitiveStages()));
        reference.setApplicableConditions(clean(request.getApplicableConditions()));

        reference.setSource(clean(request.getSource()));
        reference.setSourceDocument(clean(request.getSourceDocument()));
        reference.setSourceUrl(clean(request.getSourceUrl()));
        reference.setPublicationYear(request.getPublicationYear());
        reference.setDataStatus(clean(request.getDataStatus()));
        reference.setVersion(clean(request.getVersion()));
        reference.setValidFrom(clean(request.getValidFrom()));
        reference.setValidTo(clean(request.getValidTo()));
        reference.setNotes(clean(request.getNotes()));
    }

    public static AgriculturalReferenceResponse toResponse(
            AgriculturalReference reference
    ) {
        return AgriculturalReferenceResponse.builder()
                .id(reference.getId())
                .cropId(reference.getCrop() != null ? reference.getCrop().getId() : null)
                .cropName(reference.getCrop() != null ? reference.getCrop().getName() : null)
                .referenceType(reference.getReferenceType())
                .stateName(reference.getStateName())
                .districtName(reference.getDistrictName())
                .subDistrictName(reference.getSubDistrictName())
                .season(reference.getSeason())
                .cropStage(reference.getCropStage())
                .nitrogen(reference.getNitrogen())
                .phosphorus(reference.getPhosphorus())
                .potassium(reference.getPotassium())
                .sulphur(reference.getSulphur())
                .zinc(reference.getZinc())
                .kcInitial(reference.getKcInitial())
                .kcDevelopment(reference.getKcDevelopment())
                .kcMid(reference.getKcMid())
                .kcEnd(reference.getKcEnd())
                .rootDepthInitialCm(reference.getRootDepthInitialCm())
                .rootDepthMidCm(reference.getRootDepthMidCm())
                .rootDepthEndCm(reference.getRootDepthEndCm())
                .soilPhMin(reference.getSoilPhMin())
                .soilPhMax(reference.getSoilPhMax())
                .temperatureMin(reference.getTemperatureMin())
                .temperatureMax(reference.getTemperatureMax())
                .waterNeed(reference.getWaterNeed())
                .sowingWindow(reference.getSowingWindow())
                .seedRate(reference.getSeedRate())
                .rowSpacing(reference.getRowSpacing())
                .sowingDepth(reference.getSowingDepth())
                .criticalWaterSensitiveStages(reference.getCriticalWaterSensitiveStages())
                .applicableConditions(reference.getApplicableConditions())
                .source(reference.getSource())
                .sourceDocument(reference.getSourceDocument())
                .sourceUrl(reference.getSourceUrl())
                .publicationYear(reference.getPublicationYear())
                .dataStatus(reference.getDataStatus())
                .version(reference.getVersion())
                .validFrom(reference.getValidFrom())
                .validTo(reference.getValidTo())
                .notes(reference.getNotes())
                .build();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
