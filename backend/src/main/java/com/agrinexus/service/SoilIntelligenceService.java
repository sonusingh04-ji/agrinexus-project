package com.agrinexus.service;

import com.agrinexus.dto.SoilIntelligenceResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoilIntelligenceService {

    private final FarmRepository farmRepository;

    private final SoilSampleRepository soilSampleRepository;

    private final CropRepository cropRepository;

    /*
     * -------------------------------------------------------------
     * Main soil-intelligence analysis.
     *
     * cropId is optional.
     *
     * When supplied, the service can perform a crop-specific
     * comparison using the Crop entity's configured pH range.
     * -------------------------------------------------------------
     */
    @Transactional(readOnly = true)
    public SoilIntelligenceResponse analyze(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(farmId);

        /*
         * Latest soil record.
         */
        SoilSample sample =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );

        if (sample == null) {

            return emptyResponse(
                    farmId
            );
        }

        /*
         * Optional crop.
         */
        Crop crop = null;

        if (cropId != null) {

            crop =
                    cropRepository.findById(
                            cropId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Crop not found with ID: "
                                            + cropId
                            )
                    );
        }

        /*
         * Supported parameters in the current SoilSample
         * entity.
         */
        List<String> available =
                new ArrayList<>();

        List<String> missing =
                new ArrayList<>();

        /*
         * ---------------------------------------------------------
         * pH
         * ---------------------------------------------------------
         */
        addParameterStatus(
                "pH",
                sample.getPH(),
                available,
                missing
        );

        /*
         * ---------------------------------------------------------
         * EC
         * ---------------------------------------------------------
         */
        addParameterStatus(
                "Electrical Conductivity",
                sample.getElectricalConductivity(),
                available,
                missing
        );

        /*
         * ---------------------------------------------------------
         * Organic carbon
         * ---------------------------------------------------------
         */
        addParameterStatus(
                "Organic Carbon",
                sample.getOrganicCarbon(),
                available,
                missing
        );

        /*
         * ---------------------------------------------------------
         * Macronutrients.
         * ---------------------------------------------------------
         */
        addParameterStatus(
                "Nitrogen",
                sample.getNitrogen(),
                available,
                missing
        );

        addParameterStatus(
                "Phosphorus",
                sample.getPhosphorus(),
                available,
                missing
        );

        addParameterStatus(
                "Potassium",
                sample.getPotassium(),
                available,
                missing
        );

        /*
         * ---------------------------------------------------------
         * Micronutrients.
         * ---------------------------------------------------------
         */
        addParameterStatus(
                "Zinc",
                sample.getZinc(),
                available,
                missing
        );

        addParameterStatus(
                "Sulphur",
                sample.getSulphur(),
                available,
                missing
        );

        addParameterStatus(
                "Iron",
                sample.getIron(),
                available,
                missing
        );

        addParameterStatus(
                "Manganese",
                sample.getManganese(),
                available,
                missing
        );

        addParameterStatus(
                "Copper",
                sample.getCopper(),
                available,
                missing
        );

        /*
         * ---------------------------------------------------------
         * Completeness.
         *
         * 11 supported numeric parameters:
         * pH, EC, OC, N, P, K, Zn, S, Fe, Mn, Cu
         * ---------------------------------------------------------
         */
        int totalParameters =
                available.size() + missing.size();

        int measuredParameterCount =
                available.size();

        int missingParameterCount =
                missing.size();

        double completenessScore;

        if (totalParameters == 0) {
            completenessScore = 0.0;
        } else {
            completenessScore =
                    round2(
                            ((double) measuredParameterCount
                                    / totalParameters)
                                    * 100.0
                    );
        }

        /*
         * ---------------------------------------------------------
         * Data status.
         * ---------------------------------------------------------
         */
        String dataStatus =
                determineDataStatus(
                        sample
                );

        /*
         * ---------------------------------------------------------
         * General soil-data status.
         *
         * This deliberately describes data coverage instead of
         * inventing a universal "good/bad soil" threshold.
         * ---------------------------------------------------------
         */
        String soilDataStatus =
                determineSoilDataStatus(
                        completenessScore
                );

        /*
         * ---------------------------------------------------------
         * Crop-specific pH interpretation.
         * ---------------------------------------------------------
         */
        String cropPhInterpretation =
                null;

        UUID responseCropId =
                null;

        String responseCropName =
                null;

        Double cropSoilPhMin =
                null;

        Double cropSoilPhMax =
                null;

        if (crop != null) {

            responseCropId =
                    crop.getId();

            responseCropName =
                    crop.getName();

            cropSoilPhMin =
                    crop.getSoilPhMin();

            cropSoilPhMax =
                    crop.getSoilPhMax();

            cropPhInterpretation =
                    interpretCropPH(
                            sample.getPH(),
                            crop
                    );
        }

        /*
         * ---------------------------------------------------------
         * Findings.
         * ---------------------------------------------------------
         */
        List<String> findings =
                buildFindings(
                        sample,
                        crop
                );

        /*
         * ---------------------------------------------------------
         * Explanation.
         * ---------------------------------------------------------
         */
        String explanation =
                buildExplanation(
                        sample,
                        crop,
                        measuredParameterCount,
                        missingParameterCount,
                        completenessScore,
                        dataStatus,
                        soilDataStatus,
                        cropPhInterpretation
                );

        /*
         * ---------------------------------------------------------
         * Uncertainty.
         * ---------------------------------------------------------
         */
        String uncertaintyNote =
                buildUncertaintyNote(
                        sample,
                        crop
                );

        return SoilIntelligenceResponse.builder()

                .farmId(
                        farmId
                )

                .soilSampleId(
                        sample.getId()
                )

                .sampleDate(
                        sample.getSampleDate()
                )

                .source(
                        sample.getSource()
                )

                .dataStatus(
                        dataStatus
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

                .measuredParameterCount(
                        measuredParameterCount
                )

                .missingParameterCount(
                        missingParameterCount
                )

                .completenessScore(
                        completenessScore
                )

                .soilDataStatus(
                        soilDataStatus
                )

                .cropId(
                        responseCropId
                )

                .cropName(
                        responseCropName
                )

                .cropSoilPhMin(
                        cropSoilPhMin
                )

                .cropSoilPhMax(
                        cropSoilPhMax
                )

                .cropPhInterpretation(
                        cropPhInterpretation
                )

                .availableParameters(
                        available
                )

                .missingParameters(
                        missing
                )

                .findings(
                        findings
                )

                .explanation(
                        explanation
                )

                .uncertaintyNote(
                        uncertaintyNote
                )

                .build();
    }

    /*
     * -------------------------------------------------------------
     * Add parameter to available/missing lists.
     * -------------------------------------------------------------
     */
    private void addParameterStatus(
            String parameterName,
            Double value,
            List<String> available,
            List<String> missing
    ) {

        if (value != null) {

            available.add(
                    parameterName
            );

        } else {

            missing.add(
                    parameterName
            );
        }
    }

    /*
     * -------------------------------------------------------------
     * Determine provenance status.
     * -------------------------------------------------------------
     */
    private String determineDataStatus(
            SoilSample sample
    ) {

        String source =
                sample.getSource();

        if (source == null
                || source.isBlank()) {

            return "UNKNOWN";
        }

        String normalized =
                source.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        /*
         * These are explicitly observational/test sources.
         */
        if ("LABORATORY".equals(normalized)
                || "SOIL_TEST".equals(normalized)) {

            return "MEASURED";
        }

        /*
         * Farmer-entered information is not silently labeled
         * as laboratory-measured.
         */
        if ("FARMER_ENTERED".equals(normalized)) {

            return "ENTERED";
        }

        if ("ESTIMATED".equals(normalized)) {

            return "ESTIMATED";
        }

        if ("TEST".equals(normalized)
                || "SYNTHETIC".equals(normalized)) {

            return "TEST";
        }

        return "UNKNOWN";
    }

    /*
     * -------------------------------------------------------------
     * Data coverage status.
     * -------------------------------------------------------------
     */
    private String determineSoilDataStatus(
            double completenessScore
    ) {

        if (completenessScore >= 90.0) {

            return "HIGH_COVERAGE";
        }

        if (completenessScore >= 60.0) {

            return "PARTIAL_COVERAGE";
        }

        return "LIMITED_COVERAGE";
    }

    /*
     * -------------------------------------------------------------
     * Crop-specific pH interpretation.
     *
     * Uses only the range explicitly stored on Crop.
     * -------------------------------------------------------------
     */
    private String interpretCropPH(
            Double soilPH,
            Crop crop
    ) {

        if (soilPH == null) {

            return
                    "Soil pH is missing; crop-specific pH comparison cannot be performed.";
        }

        if (crop.getSoilPhMin() == null
                || crop.getSoilPhMax() == null) {

            return
                    "The selected crop does not have a configured pH range, so crop-specific pH comparison cannot be performed.";
        }

        double min =
                crop.getSoilPhMin();

        double max =
                crop.getSoilPhMax();

        if (soilPH >= min
                && soilPH <= max) {

            return
                    "Measured soil pH is within the configured pH range for "
                            + safe(
                            crop.getName()
                    )
                            + ".";
        }

        if (soilPH < min) {

            return
                    "Measured soil pH is below the configured pH range for "
                            + safe(
                            crop.getName()
                    )
                            + ".";
        }

        return
                "Measured soil pH is above the configured pH range for "
                        + safe(
                        crop.getName()
                )
                        + ".";
    }

    /*
     * -------------------------------------------------------------
     * Findings.
     *
     * Important:
     * We only make claims supported by the supplied fields.
     * We do NOT classify N/P/K as deficient or excessive without
     * a validated soil-test threshold/rating method.
     * -------------------------------------------------------------
     */
    private List<String> buildFindings(
            SoilSample sample,
            Crop crop
    ) {

        List<String> findings =
                new ArrayList<>();

        if (sample.getPH() != null) {

            findings.add(
                    "Measured soil pH is available."
            );

        } else {

            findings.add(
                    "Soil pH is missing."
            );
        }

        if (sample.getElectricalConductivity() != null) {

            findings.add(
                    "Electrical conductivity measurement is available."
            );

        } else {

            findings.add(
                    "Electrical conductivity is missing."
            );
        }

        if (sample.getOrganicCarbon() != null) {

            findings.add(
                    "Organic carbon measurement is available."
            );

        } else {

            findings.add(
                    "Organic carbon is missing."
            );
        }

        /*
         * N/P/K availability.
         */
        if (sample.getNitrogen() != null
                && sample.getPhosphorus() != null
                && sample.getPotassium() != null) {

            findings.add(
                    "Nitrogen, phosphorus and potassium measurements are available."
            );

        } else {

            findings.add(
                    "One or more N/P/K measurements are missing."
            );
        }

        /*
         * Micronutrient coverage.
         */
        int micronutrientCount =
                countNonNull(
                        sample.getZinc(),
                        sample.getSulphur(),
                        sample.getIron(),
                        sample.getManganese(),
                        sample.getCopper()
                );

        if (micronutrientCount == 5) {

            findings.add(
                    "All currently supported micronutrient measurements are available."
            );

        } else if (micronutrientCount > 0) {

            findings.add(
                    micronutrientCount
                            + " of 5 supported micronutrient measurements are available."
            );

        } else {

            findings.add(
                    "No supported micronutrient measurements are available."
            );
        }

        /*
         * Crop-specific pH finding.
         */
        if (crop != null) {

            findings.add(
                    interpretCropPH(
                            sample.getPH(),
                            crop
                    )
            );
        }

        return findings;
    }

    /*
     * -------------------------------------------------------------
     * Explanation.
     * -------------------------------------------------------------
     */
    private String buildExplanation(
            SoilSample sample,
            Crop crop,
            int measuredParameterCount,
            int missingParameterCount,
            double completenessScore,
            String dataStatus,
            String soilDataStatus,
            String cropPhInterpretation
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "The latest available soil record contains "
        );

        text.append(
                measuredParameterCount
        );

        text.append(
                " of 11 supported soil measurements, giving a data completeness score of "
        );

        text.append(
                completenessScore
        );

        text.append(
                "%. "
        );

        text.append(
                "Data status="
        );

        text.append(
                dataStatus
        );

        text.append(
                ". "
        );

        text.append(
                "Coverage classification="
        );

        text.append(
                soilDataStatus
        );

        text.append(
                ". "
        );

        if (missingParameterCount > 0) {

            text.append(
                    missingParameterCount
            );

            text.append(
                    " supported measurements are missing. "
            );
        }

        if (cropPhInterpretation != null) {

            text.append(
                    cropPhInterpretation
            );

            text.append(
                    " "
            );
        }

        text.append(
                "Nutrient deficiency or excess should only be classified when an appropriate soil-test rating method or validated local threshold is available."
        );

        return text.toString();
    }

    /*
     * -------------------------------------------------------------
     * Uncertainty note.
     * -------------------------------------------------------------
     */
    private String buildUncertaintyNote(
            SoilSample sample,
            Crop crop
    ) {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "This Soil Intelligence result is decision-support information and not a replacement for a laboratory soil test or qualified agronomic interpretation."
        );

        text.append(
                " The system reports the measurements actually stored in the selected soil record and does not invent missing values."
        );

        text.append(
                " N/P/K and micronutrient deficiency or excess labels are not generated without an appropriate validated rating method or threshold."
        );

        if (sample.getSource() == null
                || sample.getSource().isBlank()) {

            text.append(
                    " Soil-record source/provenance is missing."
            );
        }

        if (sample.getSampleDate() == null) {

            text.append(
                    " Soil sample date is missing."
            );
        }

        if (crop == null) {

            text.append(
                    " No crop was supplied, so crop-specific pH interpretation was not performed."
            );
        }

        return text.toString();
    }

    /*
     * -------------------------------------------------------------
     * Empty response when no soil sample exists.
     * -------------------------------------------------------------
     */
    private SoilIntelligenceResponse emptyResponse(
            UUID farmId
    ) {

        return SoilIntelligenceResponse.builder()

                .farmId(
                        farmId
                )

                .dataStatus(
                        "NO_DATA"
                )

                .measuredParameterCount(
                        0
                )

                .missingParameterCount(
                        11
                )

                .completenessScore(
                        0.0
                )

                .soilDataStatus(
                        "NO_DATA"
                )

                .availableParameters(
                        List.of()
                )

                .missingParameters(
                        List.of(
                                "pH",
                                "Electrical Conductivity",
                                "Organic Carbon",
                                "Nitrogen",
                                "Phosphorus",
                                "Potassium",
                                "Zinc",
                                "Sulphur",
                                "Iron",
                                "Manganese",
                                "Copper"
                        )
                )

                .findings(
                        List.of(
                                "No soil sample is available for this farm."
                        )
                )

                .explanation(
                        "Soil Intelligence cannot perform an interpretation because no soil sample is currently available."
                )

                .uncertaintyNote(
                        "Critical soil information is missing. A soil sample should be provided before making soil-based fertilizer or amendment decisions."
                )

                .build();
    }

    /*
     * -------------------------------------------------------------
     * Count non-null values.
     * -------------------------------------------------------------
     */
    private int countNonNull(
            Double... values
    ) {

        int count =
                0;

        for (Double value : values) {

            if (value != null) {

                count++;
            }
        }

        return count;
    }

    /*
     * -------------------------------------------------------------
     * Farm verification.
     * -------------------------------------------------------------
     */
    private void verifyFarmExists(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    /*
     * -------------------------------------------------------------
     * Safe string.
     * -------------------------------------------------------------
     */
    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    /*
     * -------------------------------------------------------------
     * Round.
     * -------------------------------------------------------------
     */
    private double round2(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}