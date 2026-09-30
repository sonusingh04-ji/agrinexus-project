package com.agrinexus.service;

import com.agrinexus.dto.FertilizerRecommendationRequest;
import com.agrinexus.dto.FertilizerRecommendationResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FertilizerRecommendation;
import com.agrinexus.entity.FertilizerReference;
import com.agrinexus.entity.NutrientBalance;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.FertilizerRecommendationRepository;
import com.agrinexus.repository.NutrientBalanceRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FertilizerRecommendationService {

    /*
     * =============================================================
     * AGRINEXUS FERTILIZER RECOMMENDATION ENGINE
     * =============================================================
     *
     * Methodology:
     * AGRINEXUS-FERT-RECOMMENDATION-V3
     *
     * PRINCIPLES
     *
     * 1. Soil values come only from real SoilSample records.
     * 2. Nutrient balance comes only from stored records.
     * 3. Fertilizer reference values come only from VERIFIED
     *    FertilizerReference records.
     * 4. No hardcoded crop fertilizer rates.
     * 5. No fabricated nutrient quantities.
     * 6. Original reference unit is preserved.
     * 7. No silent kg/ha -> kg/acre conversion.
     * 8. Recommendation is decision-support information.
     * 9. Missing verified evidence returns INSUFFICIENT_DATA.
     * 10. Reference provenance is preserved in the response.
     * =============================================================
     */

    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-FERT-RECOMMENDATION-V3";

    private static final String VERIFIED =
            "VERIFIED";

    private static final String INSUFFICIENT_DATA =
            "INSUFFICIENT_DATA";

    private static final String REVIEW_REQUIRED =
            "REVIEW_REQUIRED";

    private static final String REFERENCE_BASED =
            "REFERENCE_BASED";

    private static final String NOT_AVAILABLE =
            "NOT_AVAILABLE";

    private static final String DEFAULT_UNCERTAINTY =
            "This output is decision-support information based on a "
                    + "verified agronomic reference and the available "
                    + "farm record. It is not an automatic fertilizer "
                    + "prescription. Confirm the crop stage, soil-test "
                    + "results, fertilizer product formulation, "
                    + "application method and applicable local "
                    + "agricultural guidance before field application.";


    private final FertilizerRecommendationRepository
            fertilizerRecommendationRepository;

    private final FarmRepository
            farmRepository;

    private final CropRepository
            cropRepository;

    private final SoilSampleRepository
            soilSampleRepository;

    private final NutrientBalanceRepository
            nutrientBalanceRepository;

    private final FertilizerReferenceService
            fertilizerReferenceService;


    /*
     * =============================================================
     * GENERATE RECOMMENDATION
     * =============================================================
     */
    @Transactional
    public FertilizerRecommendationResponse generateRecommendation(
            FertilizerRecommendationRequest request
    ) {

        validateRequest(request);

        /*
         * ---------------------------------------------------------
         * 1. Load farm
         * ---------------------------------------------------------
         */
        Farm farm =
                farmRepository.findById(
                                request.getFarmId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );


        /*
         * ---------------------------------------------------------
         * 2. Load crop
         * ---------------------------------------------------------
         */
        Crop crop =
                cropRepository.findById(
                                request.getCropId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop not found with ID: "
                                                + request.getCropId()
                                )
                        );


        /*
         * ---------------------------------------------------------
         * 3. Get latest real soil record.
         * ---------------------------------------------------------
         */
        SoilSample latestSoil =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                request.getFarmId()
                        );


        /*
         * ---------------------------------------------------------
         * 4. Get latest nutrient balance.
         * ---------------------------------------------------------
         */
        NutrientBalance latestBalance =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                request.getFarmId()
                        );


        /*
         * ---------------------------------------------------------
         * 5. Soil-test record is mandatory.
         *
         * We do not generate a fertilizer recommendation without
         * an actual stored soil record.
         * ---------------------------------------------------------
         */
        if (latestSoil == null) {

            return buildInsufficientDataResponse(
                    farm,
                    crop,
                    request,
                    "No soil-test record is available for this farm."
            );
        }


        /*
         * ---------------------------------------------------------
         * 6. Validate field area.
         * ---------------------------------------------------------
         */
        if (!isFinitePositive(
                request.getFieldArea()
        )) {

            return buildInsufficientDataResponse(
                    farm,
                    crop,
                    request,
                    "A valid positive field area is required before "
                            + "generating a field-level fertilizer "
                            + "recommendation."
            );
        }


        /*
         * ---------------------------------------------------------
         * 7. Validate field-area unit.
         * ---------------------------------------------------------
         */
        String fieldAreaUnit =
                normalizeAreaUnit(
                        request.getFieldAreaUnit()
                );


        /*
         * ---------------------------------------------------------
         * 8. Resolve VERIFIED fertilizer reference.
         *
         * FertilizerReferenceService resolves:
         *
         * 1. Crop + Region + Stage
         * 2. Crop + Region
         * 3. Crop + Stage
         * 4. Crop
         *
         * Only VERIFIED records are considered.
         *
         * Farm location is passed exactly as stored.
         * We do not invent a region when one is unavailable.
         * ---------------------------------------------------------
         */
        FertilizerReference reference =
                fertilizerReferenceService
                        .findBestVerifiedReference(
                                crop.getId(),
                                normalizeRegion(
                                        farm.getLocation()
                                ),
                                request.getCropStage()
                        );


        /*
         * ---------------------------------------------------------
         * 9. No verified reference -> no fabricated output.
         * ---------------------------------------------------------
         */
        if (reference == null) {

            return buildInsufficientDataResponse(
                    farm,
                    crop,
                    request,
                    "No VERIFIED fertilizer reference is available "
                            + "for the selected crop and available "
                            + "farm/crop context."
            );
        }


        /*
         * ---------------------------------------------------------
         * 10. Validate the selected reference.
         * ---------------------------------------------------------
         */
        String referenceValidation =
                validateReference(
                        reference,
                        crop
                );

        if (referenceValidation != null) {

            return buildInsufficientDataResponse(
                    farm,
                    crop,
                    request,
                    referenceValidation
            );
        }


        /*
         * ---------------------------------------------------------
         * 11. Determine soil provenance.
         * ---------------------------------------------------------
         */
        String soilSource =
                normalizeSource(
                        latestSoil.getSource()
                );

        String soilDataStatus =
                determineSoilDataStatus(
                        latestSoil
                );


        /*
         * ---------------------------------------------------------
         * 12. Build explainable reason.
         * ---------------------------------------------------------
         */
        String reason =
                buildReason(
                        latestSoil,
                        latestBalance,
                        request,
                        soilSource,
                        soilDataStatus,
                        reference
                );


        /*
         * ---------------------------------------------------------
         * 13. Build recommendation text.
         * ---------------------------------------------------------
         */
        String recommendationText =
                buildRecommendation(
                        crop,
                        request,
                        reference
                );


        /*
         * ---------------------------------------------------------
         * 14. Build uncertainty/provenance note.
         * ---------------------------------------------------------
         */
        String uncertaintyNote =
                buildUncertaintyNote(
                        latestSoil,
                        request,
                        reference
                );


        /*
         * ---------------------------------------------------------
         * 15. Persist recommendation.
         *
         * The nutrient values copied below belong to the verified
         * reference. They are NOT soil measurements.
         *
         * No conversion is performed here.
         * ---------------------------------------------------------
         */
        FertilizerRecommendation entity =
                FertilizerRecommendation.builder()

                        .farm(
                                farm
                        )

                        .crop(
                                crop
                        )

                        .cropStage(
                                normalizeStage(
                                        request.getCropStage()
                                )
                        )

                        .fieldArea(
                                request.getFieldArea()
                        )

                        .fieldAreaUnit(
                                fieldAreaUnit
                        )

                        .nitrogen(
                                reference.getNitrogen()
                        )

                        .phosphorus(
                                reference.getPhosphorus()
                        )

                        .potassium(
                                reference.getPotassium()
                        )

                        .sulphur(
                                reference.getSulphur()
                        )

                        .zinc(
                                reference.getZinc()
                        )

                        .status(
                                REVIEW_REQUIRED
                        )

                        .recommendation(
                                recommendationText
                        )

                        .reason(
                                reason
                        )

                        .dataStatus(
                                REFERENCE_BASED
                        )

                        .source(
                                buildReferenceSource(
                                        reference
                                )
                        )

                        .uncertaintyNote(
                                uncertaintyNote
                        )

                        .generatedAt(
                                LocalDateTime.now()
                        )

                        .notes(
                                buildStoredNotes(
                                        request,
                                        reference
                                )
                        )

                        .build();


        FertilizerRecommendation saved =
                fertilizerRecommendationRepository.save(
                        entity
                );

        return toResponse(
                saved
        );
    }


    /*
     * =============================================================
     * BUILD REASON
     * =============================================================
     */
    private String buildReason(
            SoilSample soil,
            NutrientBalance balance,
            FertilizerRecommendationRequest request,
            String soilSource,
            String soilDataStatus,
            FertilizerReference reference
    ) {

        StringBuilder reason =
                new StringBuilder();


        /*
         * Soil provenance.
         */
        reason.append(
                "Latest soil sample source="
        );

        reason.append(
                soilSource
        );

        reason.append(
                ", dataStatus="
        );

        reason.append(
                soilDataStatus
        );

        reason.append(
                ". "
        );


        /*
         * Sample date.
         */
        if (soil.getSampleDate() != null) {

            reason.append(
                    "Sample date="
            );

            reason.append(
                    soil.getSampleDate()
            );

            reason.append(
                    ". "
            );

        } else {

            reason.append(
                    "Sample date is unavailable. "
            );
        }


        /*
         * Actual soil measurements.
         */
        reason.append(
                "Available soil values: "
        );

        reason.append(
                "pH="
        );

        reason.append(
                safeNumber(
                        soil.getPH()
                )
        );

        reason.append(
                ", nitrogen="
        );

        reason.append(
                safeNumber(
                        soil.getNitrogen()
                )
        );

        reason.append(
                ", phosphorus="
        );

        reason.append(
                safeNumber(
                        soil.getPhosphorus()
                )
        );

        reason.append(
                ", potassium="
        );

        reason.append(
                safeNumber(
                        soil.getPotassium()
                )
        );

        reason.append(
                ". "
        );


        /*
         * Nutrient balance context.
         */
        if (balance != null) {

            reason.append(
                    "Latest nutrient-budget balance: N="
            );

            reason.append(
                    safeNumber(
                            balance.getNitrogenBalance()
                    )
            );

            reason.append(
                    ", P="
            );

            reason.append(
                    safeNumber(
                            balance.getPhosphorusBalance()
                    )
            );

            reason.append(
                    ", K="
            );

            reason.append(
                    safeNumber(
                            balance.getPotassiumBalance()
                    )
            );

            reason.append(
                    ". "
            );

        } else {

            reason.append(
                    "No nutrient-budget record is available. "
            );
        }


        /*
         * Request context.
         */
        reason.append(
                "Crop stage="
        );

        reason.append(
                safeString(
                        request.getCropStage()
                )
        );

        reason.append(
                ". Field area="
        );

        reason.append(
                request.getFieldArea()
        );

        reason.append(
                " "
        );

        reason.append(
                normalizeAreaUnit(
                        request.getFieldAreaUnit()
                )
        );

        reason.append(
                ". "
        );


        /*
         * Verified reference context.
         */
        reason.append(
                "Verified fertilizer reference values: "
        );

        reason.append(
                "N="
        );

        reason.append(
                safeNumber(
                        reference.getNitrogen()
                )
        );

        reason.append(
                ", P="
        );

        reason.append(
                safeNumber(
                        reference.getPhosphorus()
                )
        );

        reason.append(
                ", K="
        );

        reason.append(
                safeNumber(
                        reference.getPotassium()
                )
        );

        reason.append(
                ", S="
        );

        reason.append(
                safeNumber(
                        reference.getSulphur()
                )
        );

        reason.append(
                ", Zn="
        );

        reason.append(
                safeNumber(
                        reference.getZinc()
                )
        );

        reason.append(
                ", unit="
        );

        reason.append(
                safeString(
                        reference.getUnit()
                )
        );

        reason.append(
                ". "
        );


        /*
         * Reference region.
         */
        if (reference.getRegion() != null
                && !reference.getRegion().isBlank()) {

            reason.append(
                    "Reference region="
            );

            reason.append(
                    reference.getRegion()
            );

            reason.append(
                    ". "
            );
        }


        /*
         * Reference stage.
         */
        if (reference.getApplicationStage() != null
                && !reference.getApplicationStage().isBlank()) {

            reason.append(
                    "Reference stage="
            );

            reason.append(
                    reference.getApplicationStage()
            );

            reason.append(
                    ". "
            );
        }


        /*
         * Variety / production condition.
         */
        if (reference.getVarietyOrCondition() != null
                && !reference.getVarietyOrCondition().isBlank()) {

            reason.append(
                    "Reference variety/condition="
            );

            reason.append(
                    reference.getVarietyOrCondition()
            );

            reason.append(
                    ". "
            );
        }


        /*
         * Source information.
         */
        reason.append(
                "Reference source="
        );

        reason.append(
                buildReferenceSource(
                        reference
                )
        );

        return reason.toString();
    }


    /*
     * =============================================================
     * BUILD FARMER-FACING RECOMMENDATION
     * =============================================================
     */
    private String buildRecommendation(
            Crop crop,
            FertilizerRecommendationRequest request,
            FertilizerReference reference
    ) {

        return
                "For "
                        + safeString(
                        crop.getName()
                )
                        + " at crop stage "
                        + safeString(
                        request.getCropStage()
                )
                        + ", a VERIFIED fertilizer reference is "
                        + "available. Reference nutrient values are: "
                        + "N="
                        + safeNumber(
                        reference.getNitrogen()
                )
                        + ", P="
                        + safeNumber(
                        reference.getPhosphorus()
                )
                        + ", K="
                        + safeNumber(
                        reference.getPotassium()
                )
                        + ", S="
                        + safeNumber(
                        reference.getSulphur()
                )
                        + ", Zn="
                        + safeNumber(
                        reference.getZinc()
                )
                        + " "
                        + safeString(
                        reference.getUnit()
                )
                        + ". These values are reference guidance, "
                        + "not measured soil values and not an "
                        + "automatic field prescription. No automatic "
                        + "conversion to another nutrient or field "
                        + "area unit has been performed.";
    }


    /*
     * =============================================================
     * BUILD UNCERTAINTY / PROVENANCE
     * =============================================================
     */
    private String buildUncertaintyNote(
            SoilSample soil,
            FertilizerRecommendationRequest request,
            FertilizerReference reference
    ) {

        StringBuilder note =
                new StringBuilder();

        note.append(
                DEFAULT_UNCERTAINTY
        );

        note.append(
                " Reference nutrient unit="
        );

        note.append(
                safeString(
                        reference.getUnit()
                )
        );

        note.append(
                ". Requested field-area unit="
        );

        note.append(
                normalizeAreaUnit(
                        request.getFieldAreaUnit()
                )
        );

        note.append(
                ". No automatic conversion was performed. "
        );


        /*
         * Soil source.
         */
        if (soil.getSource() == null
                || soil.getSource().isBlank()) {

            note.append(
                    "The soil-record source is missing. "
            );
        }


        /*
         * Sample date.
         */
        if (soil.getSampleDate() == null) {

            note.append(
                    "The soil sample date is missing. "
            );
        }


        /*
         * Reference provenance.
         */
        note.append(
                "Reference status="
        );

        note.append(
                safeString(
                        reference.getDataStatus()
                )
        );

        note.append(
                ". Source organization="
        );

        note.append(
                safeString(
                        reference.getSourceOrganization()
                )
        );

        note.append(
                ". Source document="
        );

        note.append(
                safeString(
                        reference.getSourceDocument()
                )
        );


        if (reference.getSourceUrl() != null
                && !reference.getSourceUrl().isBlank()) {

            note.append(
                    ". Source URL="
            );

            note.append(
                    reference.getSourceUrl()
            );
        }


        if (reference.getVerifiedDate() != null) {

            note.append(
                    ". Reference verified on="
            );

            note.append(
                    reference.getVerifiedDate()
            );
        }


        note.append(
                ". Review applicability before field application."
        );

        return note.toString();
    }


    /*
     * =============================================================
     * VALIDATE REFERENCE
     * =============================================================
     */
    private String validateReference(
            FertilizerReference reference,
            Crop crop
    ) {

        if (reference == null) {

            return
                    "Fertilizer reference is unavailable.";
        }


        /*
         * Must be VERIFIED.
         */
        if (!VERIFIED.equalsIgnoreCase(
                safeString(
                        reference.getDataStatus()
                )
        )) {

            return
                    "The selected fertilizer reference is not marked "
                            + "VERIFIED and cannot be used.";
        }


        /*
         * Crop relationship.
         */
        if (reference.getCrop() == null
                || reference.getCrop().getId() == null) {

            return
                    "The fertilizer reference is not linked to a valid crop.";
        }


        if (crop == null
                || crop.getId() == null
                || !crop.getId().equals(
                reference.getCrop().getId()
        )) {

            return
                    "The fertilizer reference crop does not match "
                            + "the selected crop.";
        }


        /*
         * Unit is mandatory.
         */
        if (reference.getUnit() == null
                || reference.getUnit().isBlank()) {

            return
                    "The fertilizer reference unit is missing.";
        }


        /*
         * Source organization is mandatory.
         */
        if (reference.getSourceOrganization() == null
                || reference.getSourceOrganization().isBlank()) {

            return
                    "The fertilizer reference source organization "
                            + "is missing.";
        }


        /*
         * Source document is mandatory.
         */
        if (reference.getSourceDocument() == null
                || reference.getSourceDocument().isBlank()) {

            return
                    "The fertilizer reference source document "
                            + "is missing.";
        }


        /*
         * At least one nutrient must be present.
         */
        boolean hasNutrient =
                hasValidNutrient(
                        reference.getNitrogen()
                )
                        || hasValidNutrient(
                        reference.getPhosphorus()
                )
                        || hasValidNutrient(
                        reference.getPotassium()
                )
                        || hasValidNutrient(
                        reference.getSulphur()
                )
                        || hasValidNutrient(
                        reference.getZinc()
                );

        if (!hasNutrient) {

            return
                    "The verified fertilizer reference does not "
                            + "contain any valid nutrient value.";
        }


        /*
         * Reject invalid numeric values.
         */
        if (isInvalidNumber(
                reference.getNitrogen()
        )
                || isInvalidNumber(
                reference.getPhosphorus()
        )
                || isInvalidNumber(
                reference.getPotassium()
        )
                || isInvalidNumber(
                reference.getSulphur()
        )
                || isInvalidNumber(
                reference.getZinc()
        )) {

            return
                    "The fertilizer reference contains an invalid "
                            + "numeric nutrient value.";
        }


        /*
         * Reject negative nutrient amounts.
         */
        if (isNegative(
                reference.getNitrogen()
        )
                || isNegative(
                reference.getPhosphorus()
        )
                || isNegative(
                reference.getPotassium()
        )
                || isNegative(
                reference.getSulphur()
        )
                || isNegative(
                reference.getZinc()
        )) {

            return
                    "The fertilizer reference contains a negative "
                            + "nutrient value.";
        }

        return null;
    }


    /*
     * =============================================================
     * SOIL DATA STATUS
     * =============================================================
     */
    private String determineSoilDataStatus(
            SoilSample soil
    ) {

        String source =
                normalizeSource(
                        soil.getSource()
                );


        if ("LABORATORY".equals(source)
                || "SOIL_TEST".equals(source)) {

            return "MEASURED";
        }


        if ("FARMER_ENTERED".equals(source)) {

            return "ENTERED";
        }


        if ("ESTIMATED".equals(source)) {

            return "ESTIMATED";
        }


        if ("TEST".equals(source)
                || "SYNTHETIC".equals(source)) {

            return "TEST";
        }


        return "UNKNOWN";
    }


    /*
     * =============================================================
     * REFERENCE SOURCE
     * =============================================================
     */
    private String buildReferenceSource(
            FertilizerReference reference
    ) {

        StringBuilder source =
                new StringBuilder();


        source.append(
                safeString(
                        reference.getSourceOrganization()
                )
        );


        source.append(
                " | "
        );


        source.append(
                safeString(
                        reference.getSourceDocument()
                )
        );


        if (reference.getRegion() != null
                && !reference.getRegion().isBlank()) {

            source.append(
                    " | Region: "
            );

            source.append(
                    reference.getRegion()
            );
        }


        if (reference.getApplicationStage() != null
                && !reference.getApplicationStage().isBlank()) {

            source.append(
                    " | Stage: "
            );

            source.append(
                    reference.getApplicationStage()
            );
        }


        if (reference.getVarietyOrCondition() != null
                && !reference.getVarietyOrCondition().isBlank()) {

            source.append(
                    " | Variety/Condition: "
            );

            source.append(
                    reference.getVarietyOrCondition()
            );
        }


        if (reference.getSourceUrl() != null
                && !reference.getSourceUrl().isBlank()) {

            source.append(
                    " | URL: "
            );

            source.append(
                    reference.getSourceUrl()
            );
        }


        return source.toString();
    }


    /*
     * =============================================================
     * STORED NOTES
     * =============================================================
     */
    private String buildStoredNotes(
            FertilizerRecommendationRequest request,
            FertilizerReference reference
    ) {

        StringBuilder notes =
                new StringBuilder();


        notes.append(
                "Methodology="
        );

        notes.append(
                METHODOLOGY_VERSION
        );

        notes.append(
                ". "
        );


        notes.append(
                "Reference status="
        );

        notes.append(
                safeString(
                        reference.getDataStatus()
                )
        );

        notes.append(
                ". "
        );


        notes.append(
                "Reference unit="
        );

        notes.append(
                safeString(
                        reference.getUnit()
                )
        );

        notes.append(
                ". "
        );


        notes.append(
                "Reference source="
        );

        notes.append(
                safeString(
                        reference.getSourceOrganization()
                )
        );

        notes.append(
                " | "
        );

        notes.append(
                safeString(
                        reference.getSourceDocument()
                )
        );


        if (reference.getSourceUrl() != null
                && !reference.getSourceUrl().isBlank()) {

            notes.append(
                    " | URL="
            );

            notes.append(
                    reference.getSourceUrl()
            );
        }


        if (request.getFarmerNote() != null
                && !request.getFarmerNote().isBlank()) {

            notes.append(
                    ". Farmer note="
            );

            notes.append(
                    request.getFarmerNote().trim()
            );
        }


        return notes.toString();
    }


    /*
     * =============================================================
     * INSUFFICIENT DATA RESPONSE
     * =============================================================
     */
    private FertilizerRecommendationResponse
    buildInsufficientDataResponse(
            Farm farm,
            Crop crop,
            FertilizerRecommendationRequest request,
            String reason
    ) {

        return
                FertilizerRecommendationResponse.builder()

                        .id(
                                null
                        )

                        .farmId(
                                farm != null
                                        ? farm.getId()
                                        : request.getFarmId()
                        )

                        .cropId(
                                crop != null
                                        ? crop.getId()
                                        : request.getCropId()
                        )

                        .cropName(
                                crop != null
                                        ? crop.getName()
                                        : null
                        )

                        .cropStage(
                                request.getCropStage()
                        )

                        .fieldArea(
                                request.getFieldArea()
                        )

                        .fieldAreaUnit(
                                normalizeAreaUnit(
                                        request.getFieldAreaUnit()
                                )
                        )

                        .nitrogen(
                                null
                        )

                        .phosphorus(
                                null
                        )

                        .potassium(
                                null
                        )

                        .sulphur(
                                null
                        )

                        .zinc(
                                null
                        )

                        .status(
                                INSUFFICIENT_DATA
                        )

                        .recommendation(
                                "A fertilizer recommendation cannot "
                                        + "be generated because the "
                                        + "required verified evidence "
                                        + "is not currently available."
                        )

                        .reason(
                                reason
                        )

                        .dataStatus(
                                NOT_AVAILABLE
                        )

                        .source(
                                "BACKEND_VALIDATION"
                        )

                        .uncertaintyNote(
                                "No unsupported fertilizer quantity "
                                        + "has been generated. Add or "
                                        + "verify the required soil and "
                                        + "agronomic reference data."
                        )

                        .generatedAt(
                                LocalDateTime.now()
                        )

                        .notes(
                                request.getFarmerNote()
                        )

                        .build();
    }


    /*
     * =============================================================
     * GET BY ID
     * =============================================================
     */
    public FertilizerRecommendationResponse
    getRecommendationById(
            UUID recommendationId
    ) {

        if (recommendationId == null) {

            throw new RuntimeException(
                    "Recommendation ID is required"
            );
        }


        FertilizerRecommendation recommendation =
                fertilizerRecommendationRepository
                        .findById(
                                recommendationId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer recommendation not "
                                                + "found with ID: "
                                                + recommendationId
                                )
                        );


        return toResponse(
                recommendation
        );
    }


    /*
     * =============================================================
     * GET FARM RECOMMENDATIONS
     * =============================================================
     */
    public List<FertilizerRecommendationResponse>
    getFarmRecommendations(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );


        return fertilizerRecommendationRepository
                .findByFarmIdOrderByGeneratedAtDesc(
                        farmId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    /*
     * =============================================================
     * GET CROP RECOMMENDATIONS
     * =============================================================
     */
    public List<FertilizerRecommendationResponse>
    getCropRecommendations(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );


        return fertilizerRecommendationRepository
                .findByFarmIdAndCropIdOrderByGeneratedAtDesc(
                        farmId,
                        cropId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    /*
     * =============================================================
     * GET LATEST FARM RECOMMENDATION
     * =============================================================
     */
    public FertilizerRecommendationResponse
    getLatestFarmRecommendation(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );


        FertilizerRecommendation recommendation =
                fertilizerRecommendationRepository
                        .findFirstByFarmIdOrderByGeneratedAtDesc(
                                farmId
                        );


        if (recommendation == null) {

            return null;
        }


        return toResponse(
                recommendation
        );
    }


    /*
     * =============================================================
     * ENTITY -> RESPONSE
     * =============================================================
     */
    private FertilizerRecommendationResponse
    toResponse(
            FertilizerRecommendation recommendation
    ) {

        return
                FertilizerRecommendationResponse.builder()

                        .id(
                                recommendation.getId()
                        )

                        .farmId(
                                recommendation.getFarm() != null
                                        ? recommendation
                                        .getFarm()
                                        .getId()
                                        : null
                        )

                        .cropId(
                                recommendation.getCrop() != null
                                        ? recommendation
                                        .getCrop()
                                        .getId()
                                        : null
                        )

                        .cropName(
                                recommendation.getCrop() != null
                                        ? recommendation
                                        .getCrop()
                                        .getName()
                                        : null
                        )

                        .cropStage(
                                recommendation.getCropStage()
                        )

                        .fieldArea(
                                recommendation.getFieldArea()
                        )

                        .fieldAreaUnit(
                                recommendation.getFieldAreaUnit()
                        )

                        .nitrogen(
                                recommendation.getNitrogen()
                        )

                        .phosphorus(
                                recommendation.getPhosphorus()
                        )

                        .potassium(
                                recommendation.getPotassium()
                        )

                        .sulphur(
                                recommendation.getSulphur()
                        )

                        .zinc(
                                recommendation.getZinc()
                        )

                        .status(
                                recommendation.getStatus()
                        )

                        .recommendation(
                                recommendation.getRecommendation()
                        )

                        .reason(
                                recommendation.getReason()
                        )

                        .dataStatus(
                                recommendation.getDataStatus()
                        )

                        .source(
                                recommendation.getSource()
                        )

                        .uncertaintyNote(
                                recommendation.getUncertaintyNote()
                        )

                        .generatedAt(
                                recommendation.getGeneratedAt()
                        )

                        .notes(
                                recommendation.getNotes()
                        )

                        .build();
    }


    /*
     * =============================================================
     * REQUEST VALIDATION
     * =============================================================
     */
    private void validateRequest(
            FertilizerRecommendationRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }


        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }


        if (request.getCropId() == null) {

            throw new RuntimeException(
                    "Crop ID is required"
            );
        }


        if (request.getCropStage() == null
                || request.getCropStage().isBlank()) {

            throw new RuntimeException(
                    "Crop stage is required"
            );
        }


        if (!isFinitePositive(
                request.getFieldArea()
        )) {

            throw new RuntimeException(
                    "Field area must be a finite value greater "
                            + "than zero"
            );
        }


        if (request.getFieldAreaUnit() == null
                || request.getFieldAreaUnit().isBlank()) {

            throw new RuntimeException(
                    "Field area unit is required"
            );
        }


        if (request.getFarmerNote() != null
                && request.getFarmerNote().length() > 2000) {

            throw new RuntimeException(
                    "Farmer note cannot exceed 2000 characters"
            );
        }
    }


    /*
     * =============================================================
     * VERIFY FARM
     * =============================================================
     */
    private void verifyFarmExists(
            UUID farmId
    ) {

        if (farmId == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }


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
     * =============================================================
     * VERIFY CROP
     * =============================================================
     */
    private void verifyCropExists(
            UUID cropId
    ) {

        if (cropId == null) {

            throw new RuntimeException(
                    "Crop ID is required"
            );
        }


        if (!cropRepository.existsById(
                cropId
        )) {

            throw new RuntimeException(
                    "Crop not found with ID: "
                            + cropId
            );
        }
    }


    /*
     * =============================================================
     * VALID NUTRIENT
     * =============================================================
     */
    private boolean hasValidNutrient(
            Double value
    ) {

        return value != null
                && Double.isFinite(value)
                && value >= 0.0;
    }


    /*
     * =============================================================
     * INVALID NUMBER
     * =============================================================
     */
    private boolean isInvalidNumber(
            Double value
    ) {

        return value != null
                && !Double.isFinite(value);
    }


    /*
     * =============================================================
     * NEGATIVE NUMBER
     * =============================================================
     */
    private boolean isNegative(
            Double value
    ) {

        return value != null
                && Double.isFinite(value)
                && value < 0.0;
    }


    /*
     * =============================================================
     * POSITIVE FINITE NUMBER
     * =============================================================
     */
    private boolean isFinitePositive(
            Double value
    ) {

        return value != null
                && Double.isFinite(value)
                && value > 0.0;
    }


    /*
     * =============================================================
     * SOURCE NORMALIZATION
     * =============================================================
     */
    private String normalizeSource(
            String source
    ) {

        if (source == null
                || source.isBlank()) {

            return "UNKNOWN";
        }


        return source
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =============================================================
     * AREA UNIT NORMALIZATION
     * =============================================================
     */
    private String normalizeAreaUnit(
            String unit
    ) {

        if (unit == null
                || unit.isBlank()) {

            return "UNKNOWN";
        }


        return unit
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =============================================================
     * CROP STAGE NORMALIZATION
     * =============================================================
     */
    private String normalizeStage(
            String stage
    ) {

        if (stage == null
                || stage.isBlank()) {

            return "UNKNOWN";
        }


        return stage
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =============================================================
     * REGION NORMALIZATION
     *
     * We use the location already stored on the Farm.
     * No external geography is invented here.
     * =============================================================
     */
    private String normalizeRegion(
            String location
    ) {

        if (location == null
                || location.isBlank()) {

            return null;
        }


        return location.trim();
    }


    /*
     * =============================================================
     * SAFE NUMBER
     * =============================================================
     */
    private String safeNumber(
            Double value
    ) {

        return value == null
                ? "UNKNOWN"
                : value.toString();
    }


    /*
     * =============================================================
     * SAFE STRING
     * =============================================================
     */
    private String safeString(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "UNKNOWN"
                : value.trim();
    }
}