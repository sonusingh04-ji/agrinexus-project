package com.agrinexus.service;

import com.agrinexus.dto.SoilSampleRequest;
import com.agrinexus.dto.SoilSampleResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.mapper.SoilSampleMapper;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoilSampleService {

    private final SoilSampleRepository soilSampleRepository;

    private final FarmRepository farmRepository;

    /*
     * =========================================================
     * CREATE SOIL SAMPLE
     * =========================================================
     *
     * Only real/observed soil measurements are accepted.
     *
     * Allowed sources:
     * LABORATORY
     * FARMER_ENTERED
     * SENSOR
     *
     * ESTIMATED is intentionally rejected.
     */
    @Transactional
    public SoilSampleResponse createSoilSample(
            SoilSampleRequest request
    ) {

        validateRequest(request);

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

        SoilSample sample =
                SoilSampleMapper.toEntity(
                        request,
                        farm
                );

        SoilSample savedSample =
                soilSampleRepository.save(sample);

        return SoilSampleMapper.toResponse(
                savedSample
        );
    }

    /*
     * =========================================================
     * GET SOIL SAMPLE BY ID
     * =========================================================
     */
    @Transactional(readOnly = true)
    public SoilSampleResponse getSoilSampleById(
            UUID sampleId
    ) {

        SoilSample sample =
                soilSampleRepository.findById(
                                sampleId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Soil sample not found with ID: "
                                                + sampleId
                                )
                        );

        return SoilSampleMapper.toResponse(
                sample
        );
    }

    /*
     * =========================================================
     * GET ALL SOIL SAMPLES FOR FARM
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<SoilSampleResponse> getFarmSoilSamples(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        return soilSampleRepository
                .findByFarmIdOrderBySampleDateDesc(
                        farmId
                )
                .stream()
                .map(SoilSampleMapper::toResponse)
                .toList();
    }

    /*
     * =========================================================
     * GET LATEST SOIL SAMPLE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public SoilSampleResponse getLatestSoilSample(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        SoilSample sample =
                soilSampleRepository
                        .findFirstByFarmIdOrderBySampleDateDesc(
                                farmId
                        );

        if (sample == null) {
            return null;
        }

        return SoilSampleMapper.toResponse(
                sample
        );
    }

    /*
     * =========================================================
     * GET BY SOURCE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<SoilSampleResponse> getSoilSamplesBySource(
            UUID farmId,
            String source
    ) {

        verifyFarmExists(farmId);

        if (source == null || source.isBlank()) {
            throw new RuntimeException(
                    "Source is required"
            );
        }

        return soilSampleRepository
                .findByFarmIdAndSourceIgnoreCaseOrderBySampleDateDesc(
                        farmId,
                        source.trim()
                )
                .stream()
                .map(SoilSampleMapper::toResponse)
                .toList();
    }

    /*
     * =========================================================
     * GET BY DATE RANGE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<SoilSampleResponse> getSoilSamplesByDateRange(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        verifyFarmExists(farmId);

        if (startDate == null || endDate == null) {
            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        return soilSampleRepository
                .findByFarmIdAndSampleDateBetweenOrderBySampleDateDesc(
                        farmId,
                        startDate,
                        endDate
                )
                .stream()
                .map(SoilSampleMapper::toResponse)
                .toList();
    }

    /*
     * =========================================================
     * UPDATE SOIL SAMPLE
     * =========================================================
     *
     * Updating a soil record follows the same real-data rules
     * as creation.
     */
    @Transactional
    public SoilSampleResponse updateSoilSample(
            UUID sampleId,
            SoilSampleRequest request
    ) {

        validateRequest(request);

        SoilSample sample =
                soilSampleRepository.findById(
                                sampleId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Soil sample not found with ID: "
                                                + sampleId
                                )
                        );

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

        sample.setFarm(farm);

        sample.setSampleDate(
                request.getSampleDate()
        );

        sample.setSoilType(
                cleanString(request.getSoilType())
        );

        sample.setPH(
                request.getPH()
        );

        sample.setElectricalConductivity(
                request.getElectricalConductivity()
        );

        sample.setOrganicCarbon(
                request.getOrganicCarbon()
        );

        sample.setNitrogen(
                request.getNitrogen()
        );

        sample.setPhosphorus(
                request.getPhosphorus()
        );

        sample.setPotassium(
                request.getPotassium()
        );

        sample.setZinc(
                request.getZinc()
        );

        sample.setSulphur(
                request.getSulphur()
        );

        sample.setIron(
                request.getIron()
        );

        sample.setManganese(
                request.getManganese()
        );

        sample.setCopper(
                request.getCopper()
        );

        sample.setSource(
                normalizeSource(
                        request.getSource()
                )
        );

        sample.setNotes(
                cleanString(request.getNotes())
        );

        SoilSample updatedSample =
                soilSampleRepository.save(
                        sample
                );

        return SoilSampleMapper.toResponse(
                updatedSample
        );
    }

    /*
     * =========================================================
     * DELETE SOIL SAMPLE
     * =========================================================
     */
    @Transactional
    public void deleteSoilSample(
            UUID sampleId
    ) {

        if (!soilSampleRepository.existsById(
                sampleId
        )) {

            throw new RuntimeException(
                    "Soil sample not found with ID: "
                            + sampleId
            );
        }

        soilSampleRepository.deleteById(
                sampleId
        );
    }

    /*
     * =========================================================
     * REQUEST VALIDATION
     * =========================================================
     */
    private void validateRequest(
            SoilSampleRequest request
    ) {

        if (request == null) {
            throw new RuntimeException(
                    "Soil sample request is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * Farm
         * ---------------------------------------------------------
         */
        if (request.getFarmId() == null) {
            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * Sample date
         * ---------------------------------------------------------
         *
         * A soil observation without a collection/testing date
         * cannot be reliably used for chronological soil trends.
         */
        if (request.getSampleDate() == null) {
            throw new RuntimeException(
                    "Sample date is required"
            );
        }

        /*
         * Prevent future-dated measurements.
         */
        if (request.getSampleDate()
                .isAfter(LocalDate.now())) {

            throw new RuntimeException(
                    "Sample date cannot be in the future"
            );
        }

        /*
         * ---------------------------------------------------------
         * Source
         * ---------------------------------------------------------
         */
        String source =
                normalizeSource(
                        request.getSource()
                );

        if (source == null) {
            throw new RuntimeException(
                    "Measurement source is required"
            );
        }

        if (!isAllowedRealSource(source)) {

            throw new RuntimeException(
                    "Invalid soil measurement source: "
                            + source
                            + ". Allowed sources are "
                            + "LABORATORY, FARMER_ENTERED and SENSOR."
            );
        }

        /*
         * ---------------------------------------------------------
         * pH
         * ---------------------------------------------------------
         */
        validatePH(
                request.getPH()
        );

        /*
         * ---------------------------------------------------------
         * Other measured values
         * ---------------------------------------------------------
         */
        validateNonNegative(
                "Electrical conductivity",
                request.getElectricalConductivity()
        );

        validateNonNegative(
                "Organic carbon",
                request.getOrganicCarbon()
        );

        validateNonNegative(
                "Nitrogen",
                request.getNitrogen()
        );

        validateNonNegative(
                "Phosphorus",
                request.getPhosphorus()
        );

        validateNonNegative(
                "Potassium",
                request.getPotassium()
        );

        validateNonNegative(
                "Zinc",
                request.getZinc()
        );

        validateNonNegative(
                "Sulphur",
                request.getSulphur()
        );

        validateNonNegative(
                "Iron",
                request.getIron()
        );

        validateNonNegative(
                "Manganese",
                request.getManganese()
        );

        validateNonNegative(
                "Copper",
                request.getCopper()
        );

        /*
         * ---------------------------------------------------------
         * At least one actual measurement
         * ---------------------------------------------------------
         */
        if (!hasAtLeastOneMeasurement(request)) {

            throw new RuntimeException(
                    "At least one measured soil parameter "
                            + "is required"
            );
        }

        /*
         * ---------------------------------------------------------
         * Text length protection
         * ---------------------------------------------------------
         */
        validateTextLength(
                "Soil type",
                request.getSoilType(),
                200
        );

        validateTextLength(
                "Source",
                request.getSource(),
                50
        );

        validateTextLength(
                "Notes",
                request.getNotes(),
                1000
        );
    }

    /*
     * =========================================================
     * REAL SOURCE VALIDATION
     * =========================================================
     */
    private boolean isAllowedRealSource(
            String source
    ) {

        return "LABORATORY".equals(source)
                || "FARMER_ENTERED".equals(source)
                || "SENSOR".equals(source);
    }

    /*
     * =========================================================
     * pH VALIDATION
     * =========================================================
     */
    private void validatePH(
            Double value
    ) {

        if (value == null) {
            return;
        }

        if (!Double.isFinite(value)) {
            throw new RuntimeException(
                    "Soil pH must be a finite number"
            );
        }

        if (value < 0.0 || value > 14.0) {
            throw new RuntimeException(
                    "Soil pH must be between 0 and 14"
            );
        }
    }

    /*
     * =========================================================
     * NON-NEGATIVE VALIDATION
     * =========================================================
     */
    private void validateNonNegative(
            String fieldName,
            Double value
    ) {

        if (value == null) {
            return;
        }

        if (!Double.isFinite(value)) {
            throw new RuntimeException(
                    fieldName
                            + " must be a finite number"
            );
        }

        if (value < 0.0) {
            throw new RuntimeException(
                    fieldName
                            + " cannot be negative"
            );
        }
    }

    /*
     * =========================================================
     * AT LEAST ONE MEASUREMENT
     * =========================================================
     */
    private boolean hasAtLeastOneMeasurement(
            SoilSampleRequest request
    ) {

        return request.getPH() != null
                || request.getElectricalConductivity() != null
                || request.getOrganicCarbon() != null
                || request.getNitrogen() != null
                || request.getPhosphorus() != null
                || request.getPotassium() != null
                || request.getZinc() != null
                || request.getSulphur() != null
                || request.getIron() != null
                || request.getManganese() != null
                || request.getCopper() != null;
    }

    /*
     * =========================================================
     * TEXT VALIDATION
     * =========================================================
     */
    private void validateTextLength(
            String fieldName,
            String value,
            int maximumLength
    ) {

        if (value == null) {
            return;
        }

        if (value.length() > maximumLength) {
            throw new RuntimeException(
                    fieldName
                            + " cannot exceed "
                            + maximumLength
                            + " characters"
            );
        }
    }

    /*
     * =========================================================
     * SOURCE NORMALIZATION
     * =========================================================
     */
    private String normalizeSource(
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

    /*
     * =========================================================
     * STRING CLEANING
     * =========================================================
     */
    private String cleanString(
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
     * FARM VALIDATION
     * =========================================================
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
}