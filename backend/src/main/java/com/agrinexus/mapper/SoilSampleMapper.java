package com.agrinexus.mapper;

import com.agrinexus.dto.SoilSampleRequest;
import com.agrinexus.dto.SoilSampleResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SoilSample;

import java.util.Locale;

public class SoilSampleMapper {

    private SoilSampleMapper() {
        // Utility class
    }

    /*
     * =========================================================
     * REQUEST DTO -> ENTITY
     * =========================================================
     */
    public static SoilSample toEntity(
            SoilSampleRequest request,
            Farm farm
    ) {

        return SoilSample.builder()
                .farm(farm)

                .sampleDate(
                        request.getSampleDate()
                )

                .soilType(
                        cleanString(
                                request.getSoilType()
                        )
                )

                .pH(
                        request.getPH()
                )

                .electricalConductivity(
                        request.getElectricalConductivity()
                )

                .organicCarbon(
                        request.getOrganicCarbon()
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

                .zinc(
                        request.getZinc()
                )

                .sulphur(
                        request.getSulphur()
                )

                .iron(
                        request.getIron()
                )

                .manganese(
                        request.getManganese()
                )

                .copper(
                        request.getCopper()
                )

                .source(
                        normalizeSource(
                                request.getSource()
                        )
                )

                .notes(
                        cleanString(
                                request.getNotes()
                        )
                )

                .build();
    }

    /*
     * =========================================================
     * ENTITY -> RESPONSE DTO
     * =========================================================
     */
    public static SoilSampleResponse toResponse(
            SoilSample sample
    ) {

        if (sample == null) {
            return null;
        }

        return SoilSampleResponse.builder()

                .id(
                        sample.getId()
                )

                .farmId(
                        sample.getFarm() != null
                                ? sample.getFarm().getId()
                                : null
                )

                .sampleDate(
                        sample.getSampleDate()
                )

                .soilType(
                        sample.getSoilType()
                )

                .pH(
                        sample.getPH()
                )

                .electricalConductivity(
                        sample.getElectricalConductivity()
                )

                .organicCarbon(
                        sample.getOrganicCarbon()
                )

                .nitrogen(
                        sample.getNitrogen()
                )

                .phosphorus(
                        sample.getPhosphorus()
                )

                .potassium(
                        sample.getPotassium()
                )

                .zinc(
                        sample.getZinc()
                )

                .sulphur(
                        sample.getSulphur()
                )

                .iron(
                        sample.getIron()
                )

                .manganese(
                        sample.getManganese()
                )

                .copper(
                        sample.getCopper()
                )

                .source(
                        sample.getSource()
                )

                .notes(
                        sample.getNotes()
                )

                .build();
    }

    /*
     * =========================================================
     * CLEAN TEXT
     * =========================================================
     */
    private static String cleanString(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    /*
     * =========================================================
     * NORMALIZE SOURCE
     * =========================================================
     */
    private static String normalizeSource(
            String source
    ) {

        if (source == null
                || source.isBlank()) {

            return null;
        }

        return source
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}