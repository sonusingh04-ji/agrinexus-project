package com.agrinexus.service;

import com.agrinexus.dto.FertilizerReferenceRequest;
import com.agrinexus.dto.FertilizerReferenceResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.FertilizerReference;
import com.agrinexus.mapper.FertilizerReferenceMapper;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FertilizerReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FertilizerReferenceService {

    private static final String VERIFIED = "VERIFIED";
    private static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";
    private static final String RETIRED = "RETIRED";

    private final FertilizerReferenceRepository
            fertilizerReferenceRepository;

    private final CropRepository
            cropRepository;


    /*
     * =============================================================
     * GET ALL VERIFIED REFERENCES FOR A CROP
     * =============================================================
     */
    @Transactional(readOnly = true)
    public List<FertilizerReferenceResponse> getVerifiedReferences(
            UUID cropId
    ) {

        verifyCropExists(cropId);

        return fertilizerReferenceRepository
                .findByCropIdAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                        cropId,
                        VERIFIED
                )
                .stream()
                .map(FertilizerReferenceMapper::toResponse)
                .toList();
    }


    /*
     * =============================================================
     * GET VERIFIED REFERENCES FOR CROP + REGION
     * =============================================================
     */
    @Transactional(readOnly = true)
    public List<FertilizerReferenceResponse> getVerifiedReferences(
            UUID cropId,
            String region
    ) {

        verifyCropExists(cropId);

        if (region == null || region.isBlank()) {
            return getVerifiedReferences(cropId);
        }

        return fertilizerReferenceRepository
                .findByCropIdAndRegionIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                        cropId,
                        region.trim(),
                        VERIFIED
                )
                .stream()
                .map(FertilizerReferenceMapper::toResponse)
                .toList();
    }


    /*
     * =============================================================
     * FIND BEST VERIFIED REFERENCE
     *
     * Priority:
     *
     * 1. Crop + Region + Stage
     * 2. Crop + Region
     * 3. Crop + Stage
     * 4. Crop
     *
     * IMPORTANT:
     * Only VERIFIED references can reach this method's result.
     * =============================================================
     */
    @Transactional(readOnly = true)
    public FertilizerReference findBestVerifiedReference(
            UUID cropId,
            String region,
            String applicationStage
    ) {

        verifyCropExists(cropId);

        /*
         * ---------------------------------------------------------
         * 1. Exact crop + region + stage
         * ---------------------------------------------------------
         */
        if (hasText(region)
                && hasText(applicationStage)) {

            List<FertilizerReference> exact =
                    fertilizerReferenceRepository
                            .findByCropIdAndRegionIgnoreCaseAndApplicationStageIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                                    cropId,
                                    region.trim(),
                                    applicationStage.trim(),
                                    VERIFIED
                            );

            if (!exact.isEmpty()) {
                return exact.get(0);
            }
        }


        /*
         * ---------------------------------------------------------
         * 2. Crop + region
         * ---------------------------------------------------------
         */
        if (hasText(region)) {

            List<FertilizerReference> regional =
                    fertilizerReferenceRepository
                            .findByCropIdAndRegionIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                                    cropId,
                                    region.trim(),
                                    VERIFIED
                            );

            if (!regional.isEmpty()) {
                return regional.get(0);
            }
        }


        /*
         * ---------------------------------------------------------
         * 3. Crop + application stage
         * ---------------------------------------------------------
         */
        if (hasText(applicationStage)) {

            List<FertilizerReference> stageSpecific =
                    fertilizerReferenceRepository
                            .findByCropIdAndApplicationStageIgnoreCaseAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                                    cropId,
                                    applicationStage.trim(),
                                    VERIFIED
                            );

            if (!stageSpecific.isEmpty()) {
                return stageSpecific.get(0);
            }
        }


        /*
         * ---------------------------------------------------------
         * 4. General crop reference
         * ---------------------------------------------------------
         */
        List<FertilizerReference> general =
                fertilizerReferenceRepository
                        .findByCropIdAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                                cropId,
                                VERIFIED
                        );

        return general.isEmpty()
                ? null
                : general.get(0);
    }


    /*
     * =============================================================
     * CREATE REFERENCE
     *
     * New references always enter REVIEW_REQUIRED.
     *
     * A caller cannot create a VERIFIED reference directly.
     * =============================================================
     */
    public FertilizerReferenceResponse createReference(
            FertilizerReferenceRequest request
    ) {

        validateRequest(request);

        Crop crop =
                cropRepository.findById(
                                request.getCropId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Crop not found with ID: "
                                                + request.getCropId()
                                )
                        );


        FertilizerReference reference =
                FertilizerReference.builder()

                        .crop(
                                crop
                        )

                        .region(
                                normalizeNullable(
                                        request.getRegion()
                                )
                        )

                        .varietyOrCondition(
                                normalizeNullable(
                                        request.getVarietyOrCondition()
                                )
                        )

                        .applicationStage(
                                normalizeNullable(
                                        request.getApplicationStage()
                                )
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

                        .unit(
                                request.getUnit().trim()
                        )

                        .sourceOrganization(
                                request.getSourceOrganization().trim()
                        )

                        .sourceDocument(
                                request.getSourceDocument().trim()
                        )

                        .sourceUrl(
                                normalizeNullable(
                                        request.getSourceUrl()
                                )
                        )

                        /*
                         * Do not trust a caller-provided verified date
                         * for a new record.
                         */
                        .verifiedDate(
                                null
                        )

                        /*
                         * New evidence must be reviewed before becoming
                         * usable by recommendation generation.
                         */
                        .dataStatus(
                                REVIEW_REQUIRED
                        )

                        .notes(
                                normalizeNullable(
                                        request.getNotes()
                                )
                        )

                        .build();


        FertilizerReference saved =
                fertilizerReferenceRepository.save(
                        reference
                );

        return FertilizerReferenceMapper.toResponse(
                saved
        );
    }


    /*
     * =============================================================
     * UPDATE REFERENCE
     *
     * General editing is allowed only while a reference is not
     * VERIFIED.
     *
     * VERIFIED records must be changed through a controlled
     * verification/review process.
     * =============================================================
     */
    public FertilizerReferenceResponse updateReference(
            UUID referenceId,
            FertilizerReferenceRequest request
    ) {

        if (referenceId == null) {
            throw new IllegalArgumentException(
                    "Reference ID is required"
            );
        }

        validateRequest(request);

        FertilizerReference reference =
                fertilizerReferenceRepository
                        .findById(referenceId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Fertilizer reference not found "
                                                + "with ID: "
                                                + referenceId
                                )
                        );


        /*
         * Prevent direct editing of an already verified record.
         *
         * This helps preserve the integrity of evidence that the
         * recommendation engine may already depend on.
         */
        if (VERIFIED.equalsIgnoreCase(
                safeString(
                        reference.getDataStatus()
                )
        )) {

            throw new IllegalStateException(
                    "VERIFIED references cannot be directly edited. "
                            + "Retire the current reference and create "
                            + "a new reviewable reference instead."
            );
        }


        Crop crop =
                cropRepository.findById(
                                request.getCropId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Crop not found with ID: "
                                                + request.getCropId()
                                )
                        );


        reference.setCrop(
                crop
        );

        reference.setRegion(
                normalizeNullable(
                        request.getRegion()
                )
        );

        reference.setVarietyOrCondition(
                normalizeNullable(
                        request.getVarietyOrCondition()
                )
        );

        reference.setApplicationStage(
                normalizeNullable(
                        request.getApplicationStage()
                )
        );

        reference.setNitrogen(
                request.getNitrogen()
        );

        reference.setPhosphorus(
                request.getPhosphorus()
        );

        reference.setPotassium(
                request.getPotassium()
        );

        reference.setSulphur(
                request.getSulphur()
        );

        reference.setZinc(
                request.getZinc()
        );

        reference.setUnit(
                request.getUnit().trim()
        );

        reference.setSourceOrganization(
                request.getSourceOrganization().trim()
        );

        reference.setSourceDocument(
                request.getSourceDocument().trim()
        );

        reference.setSourceUrl(
                normalizeNullable(
                        request.getSourceUrl()
                )
        );

        /*
         * Keep existing verification date untouched unless the
         * record is explicitly verified.
         */
        reference.setDataStatus(
                REVIEW_REQUIRED
        );

        reference.setNotes(
                normalizeNullable(
                        request.getNotes()
                )
        );


        FertilizerReference saved =
                fertilizerReferenceRepository.save(
                        reference
                );

        return FertilizerReferenceMapper.toResponse(
                saved
        );
    }


    /*
     * =============================================================
     * VERIFY REFERENCE
     *
     * This is the controlled transition:
     *
     * REVIEW_REQUIRED -> VERIFIED
     *
     * The verification date is assigned by the backend.
     * =============================================================
     */
    public FertilizerReferenceResponse verifyReference(
            UUID referenceId
    ) {

        FertilizerReference reference =
                getReferenceEntity(
                        referenceId
                );


        if (RETIRED.equalsIgnoreCase(
                safeString(
                        reference.getDataStatus()
                )
        )) {

            throw new IllegalStateException(
                    "A RETIRED fertilizer reference cannot be verified."
            );
        }


        /*
         * Revalidate provenance before allowing VERIFIED state.
         */
        validateStoredReferenceForVerification(
                reference
        );


        reference.setDataStatus(
                VERIFIED
        );

        reference.setVerifiedDate(
                LocalDate.now()
        );


        FertilizerReference saved =
                fertilizerReferenceRepository.save(
                        reference
                );

        return FertilizerReferenceMapper.toResponse(
                saved
        );
    }


    /*
     * =============================================================
     * RETIRE REFERENCE
     *
     * We do not physically delete the evidence record.
     * It remains available for audit/provenance.
     * =============================================================
     */
    public FertilizerReferenceResponse retireReference(
            UUID referenceId
    ) {

        FertilizerReference reference =
                getReferenceEntity(
                        referenceId
                );


        reference.setDataStatus(
                RETIRED
        );


        FertilizerReference saved =
                fertilizerReferenceRepository.save(
                        reference
                );

        return FertilizerReferenceMapper.toResponse(
                saved
        );
    }


    /*
     * =============================================================
     * GET REFERENCE BY ID
     * =============================================================
     */
    @Transactional(readOnly = true)
    public FertilizerReferenceResponse getReferenceById(
            UUID referenceId
    ) {

        return FertilizerReferenceMapper.toResponse(
                getReferenceEntity(
                        referenceId
                )
        );
    }


    /*
     * =============================================================
     * GET ALL REFERENCES FOR A CROP
     *
     * Includes REVIEW_REQUIRED / VERIFIED / RETIRED.
     *
     * Useful for administrative review.
     * =============================================================
     */
    @Transactional(readOnly = true)
    public List<FertilizerReferenceResponse>
    getAllReferencesForCrop(
            UUID cropId
    ) {

        verifyCropExists(
                cropId
        );

        return fertilizerReferenceRepository
                .findByCropIdAndDataStatusIgnoreCaseOrderByVerifiedDateDesc(
                        cropId,
                        VERIFIED
                )
                .stream()
                .map(FertilizerReferenceMapper::toResponse)
                .toList();
    }


    /*
     * =============================================================
     * INTERNAL ENTITY LOOKUP
     * =============================================================
     */
    @Transactional(readOnly = true)
    protected FertilizerReference getReferenceEntity(
            UUID referenceId
    ) {

        if (referenceId == null) {

            throw new IllegalArgumentException(
                    "Reference ID is required"
            );
        }


        return fertilizerReferenceRepository
                .findById(
                        referenceId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Fertilizer reference not found "
                                        + "with ID: "
                                        + referenceId
                        )
                );
    }


    /*
     * =============================================================
     * REQUEST VALIDATION
     * =============================================================
     */
    private void validateRequest(
            FertilizerReferenceRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Fertilizer reference request cannot be null"
            );
        }


        if (request.getCropId() == null) {

            throw new IllegalArgumentException(
                    "Crop ID is required"
            );
        }


        if (!hasText(
                request.getUnit()
        )) {

            throw new IllegalArgumentException(
                    "Reference unit is required"
            );
        }


        if (!hasText(
                request.getSourceOrganization()
        )) {

            throw new IllegalArgumentException(
                    "Source organization is required"
            );
        }


        if (!hasText(
                request.getSourceDocument()
        )) {

            throw new IllegalArgumentException(
                    "Source document is required"
            );
        }


        validateNutrient(
                request.getNitrogen(),
                "Nitrogen"
        );

        validateNutrient(
                request.getPhosphorus(),
                "Phosphorus"
        );

        validateNutrient(
                request.getPotassium(),
                "Potassium"
        );

        validateNutrient(
                request.getSulphur(),
                "Sulphur"
        );

        validateNutrient(
                request.getZinc(),
                "Zinc"
        );


        /*
         * At least one nutrient value must be supplied.
         */
        boolean hasNutrient =
                hasValidNutrient(
                        request.getNitrogen()
                )
                        || hasValidNutrient(
                        request.getPhosphorus()
                )
                        || hasValidNutrient(
                        request.getPotassium()
                )
                        || hasValidNutrient(
                        request.getSulphur()
                )
                        || hasValidNutrient(
                        request.getZinc()
                );


        if (!hasNutrient) {

            throw new IllegalArgumentException(
                    "At least one fertilizer nutrient value "
                            + "(N, P, K, S or Zn) is required"
            );
        }


        validateLength(
                request.getRegion(),
                150,
                "Region"
        );

        validateLength(
                request.getVarietyOrCondition(),
                200,
                "Variety or condition"
        );

        validateLength(
                request.getApplicationStage(),
                100,
                "Application stage"
        );

        validateLength(
                request.getUnit(),
                50,
                "Unit"
        );

        validateLength(
                request.getSourceOrganization(),
                200,
                "Source organization"
        );

        validateLength(
                request.getSourceDocument(),
                500,
                "Source document"
        );

        validateLength(
                request.getSourceUrl(),
                1000,
                "Source URL"
        );

        validateLength(
                request.getNotes(),
                2000,
                "Notes"
        );
    }


    /*
     * =============================================================
     * VALIDATE STORED REFERENCE BEFORE VERIFICATION
     * =============================================================
     */
    private void validateStoredReferenceForVerification(
            FertilizerReference reference
    ) {

        if (reference.getCrop() == null
                || reference.getCrop().getId() == null) {

            throw new IllegalStateException(
                    "Reference crop is missing."
            );
        }


        if (!hasText(
                reference.getUnit()
        )) {

            throw new IllegalStateException(
                    "Reference unit is missing."
            );
        }


        if (!hasText(
                reference.getSourceOrganization()
        )) {

            throw new IllegalStateException(
                    "Source organization is missing."
            );
        }


        if (!hasText(
                reference.getSourceDocument()
        )) {

            throw new IllegalStateException(
                    "Source document is missing."
            );
        }


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

            throw new IllegalStateException(
                    "At least one valid nutrient value is required "
                            + "before verification."
            );
        }


        validateStoredNutrient(
                reference.getNitrogen(),
                "Nitrogen"
        );

        validateStoredNutrient(
                reference.getPhosphorus(),
                "Phosphorus"
        );

        validateStoredNutrient(
                reference.getPotassium(),
                "Potassium"
        );

        validateStoredNutrient(
                reference.getSulphur(),
                "Sulphur"
        );

        validateStoredNutrient(
                reference.getZinc(),
                "Zinc"
        );
    }


    /*
     * =============================================================
     * NUTRIENT VALIDATION
     * =============================================================
     */
    private void validateNutrient(
            Double value,
            String name
    ) {

        if (value == null) {
            return;
        }


        if (!Double.isFinite(value)) {

            throw new IllegalArgumentException(
                    name
                            + " must be a finite number"
            );
        }


        if (value < 0.0) {

            throw new IllegalArgumentException(
                    name
                            + " cannot be negative"
            );
        }
    }


    private void validateStoredNutrient(
            Double value,
            String name
    ) {

        if (value == null) {
            return;
        }


        if (!Double.isFinite(value)
                || value < 0.0) {

            throw new IllegalStateException(
                    name
                            + " contains an invalid value."
            );
        }
    }


    private boolean hasValidNutrient(
            Double value
    ) {

        return value != null
                && Double.isFinite(value)
                && value >= 0.0;
    }


    /*
     * =============================================================
     * LENGTH VALIDATION
     * =============================================================
     */
    private void validateLength(
            String value,
            int maxLength,
            String fieldName
    ) {

        if (value != null
                && value.length() > maxLength) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot exceed "
                            + maxLength
                            + " characters"
            );
        }
    }


    /*
     * =============================================================
     * CROP VALIDATION
     * =============================================================
     */
    private void verifyCropExists(
            UUID cropId
    ) {

        if (cropId == null) {

            throw new IllegalArgumentException(
                    "Crop ID is required"
            );
        }


        if (!cropRepository.existsById(
                cropId
        )) {

            throw new IllegalArgumentException(
                    "Crop not found with ID: "
                            + cropId
            );
        }
    }


    /*
     * =============================================================
     * TEXT HELPERS
     * =============================================================
     */
    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }


    private String normalizeNullable(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }


    private String safeString(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "UNKNOWN";
        }

        return value
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }
}