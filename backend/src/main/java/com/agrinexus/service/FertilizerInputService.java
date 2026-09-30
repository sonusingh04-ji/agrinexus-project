package com.agrinexus.service;

import com.agrinexus.dto.FertilizerInputRequest;
import com.agrinexus.dto.FertilizerInputResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FertilizerInput;
import com.agrinexus.mapper.FertilizerInputMapper;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.FertilizerInputRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FertilizerInputService {

    private final FertilizerInputRepository fertilizerInputRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;


    /*
     * =========================================================
     * CREATE
     * =========================================================
     *
     * Create a real fertilizer/input record.
     */
    public FertilizerInputResponse createFertilizerInput(
            FertilizerInputRequest request
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

        Crop crop = null;

        /*
         * Crop is optional.
         *
         * A fertilizer input may be recorded at farm level
         * without being linked to a specific crop.
         */
        if (request.getCropId() != null) {

            crop =
                    cropRepository.findById(
                                    request.getCropId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }

        FertilizerInput input =
                FertilizerInputMapper.toEntity(
                        request,
                        farm,
                        crop
                );

        FertilizerInput savedInput =
                fertilizerInputRepository.save(
                        input
                );

        return FertilizerInputMapper.toResponse(
                savedInput
        );
    }


    /*
     * =========================================================
     * GET BY ID
     * =========================================================
     */
    public FertilizerInputResponse getFertilizerInputById(
            UUID inputId
    ) {

        if (inputId == null) {
            throw new RuntimeException(
                    "Input ID is required"
            );
        }

        FertilizerInput input =
                fertilizerInputRepository.findById(
                                inputId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer input not found with ID: "
                                                + inputId
                                )
                        );

        return FertilizerInputMapper.toResponse(
                input
        );
    }


    /*
     * =========================================================
     * GET ALL FOR FARM
     * =========================================================
     */
    public List<FertilizerInputResponse> getFarmFertilizerInputs(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        return fertilizerInputRepository
                .findByFarmIdOrderByApplicationDateDesc(
                        farmId
                )
                .stream()
                .map(
                        FertilizerInputMapper::toResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET FOR CROP
     * =========================================================
     */
    public List<FertilizerInputResponse> getCropFertilizerInputs(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );

        return fertilizerInputRepository
                .findByFarmIdAndCropIdOrderByApplicationDateDesc(
                        farmId,
                        cropId
                )
                .stream()
                .map(
                        FertilizerInputMapper::toResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET BY INPUT TYPE
     * =========================================================
     *
     * Example:
     * ORGANIC
     * CHEMICAL
     * BIOLOGICAL
     * MANURE
     */
    public List<FertilizerInputResponse> getInputsByType(
            UUID farmId,
            String inputType
    ) {

        verifyFarmExists(
                farmId
        );

        if (inputType == null
                || inputType.isBlank()) {

            throw new RuntimeException(
                    "Input type is required"
            );
        }

        return fertilizerInputRepository
                .findByFarmIdAndInputTypeIgnoreCaseOrderByApplicationDateDesc(
                        farmId,
                        inputType
                )
                .stream()
                .map(
                        FertilizerInputMapper::toResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET BY DATE RANGE
     * =========================================================
     */
    public List<FertilizerInputResponse> getInputsByDateRange(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        verifyFarmExists(
                farmId
        );

        if (startDate == null
                || endDate == null) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {

            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        return fertilizerInputRepository
                .findByFarmIdAndApplicationDateBetweenOrderByApplicationDateDesc(
                        farmId,
                        startDate,
                        endDate
                )
                .stream()
                .map(
                        FertilizerInputMapper::toResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * UPDATE
     * =========================================================
     */
    public FertilizerInputResponse updateFertilizerInput(
            UUID inputId,
            FertilizerInputRequest request
    ) {

        if (inputId == null) {
            throw new RuntimeException(
                    "Input ID is required"
            );
        }

        validateRequest(
                request
        );

        FertilizerInput input =
                fertilizerInputRepository.findById(
                                inputId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Fertilizer input not found with ID: "
                                                + inputId
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

        Crop crop = null;

        if (request.getCropId() != null) {

            crop =
                    cropRepository.findById(
                                    request.getCropId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }

        input.setFarm(
                farm
        );

        input.setCrop(
                crop
        );

        input.setApplicationDate(
                request.getApplicationDate()
        );

        input.setProductName(
                request.getProductName()
        );

        input.setInputType(
                request.getInputType()
        );

        input.setQuantity(
                request.getQuantity()
        );

        input.setQuantityUnit(
                request.getQuantityUnit()
        );

        input.setNitrogen(
                request.getNitrogen()
        );

        input.setPhosphorus(
                request.getPhosphorus()
        );

        input.setPotassium(
                request.getPotassium()
        );

        input.setSulphur(
                request.getSulphur()
        );

        input.setZinc(
                request.getZinc()
        );

        input.setSource(
                request.getSource()
        );

        input.setNotes(
                request.getNotes()
        );

        FertilizerInput updatedInput =
                fertilizerInputRepository.save(
                        input
                );

        return FertilizerInputMapper.toResponse(
                updatedInput
        );
    }


    /*
     * =========================================================
     * DELETE
     * =========================================================
     */
    public void deleteFertilizerInput(
            UUID inputId
    ) {

        if (inputId == null) {
            throw new RuntimeException(
                    "Input ID is required"
            );
        }

        if (!fertilizerInputRepository.existsById(
                inputId
        )) {

            throw new RuntimeException(
                    "Fertilizer input not found with ID: "
                            + inputId
            );
        }

        fertilizerInputRepository.deleteById(
                inputId
        );
    }


    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */
    private void validateRequest(
            FertilizerInputRequest request
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

        if (request.getApplicationDate() == null) {

            throw new RuntimeException(
                    "Application date is required"
            );
        }

        if (request.getProductName() == null
                || request.getProductName().isBlank()) {

            throw new RuntimeException(
                    "Product name is required"
            );
        }

        if (request.getQuantity() != null
                && request.getQuantity() < 0) {

            throw new RuntimeException(
                    "Quantity cannot be negative"
            );
        }

        if (request.getNitrogen() != null
                && request.getNitrogen() < 0) {

            throw new RuntimeException(
                    "Nitrogen amount cannot be negative"
            );
        }

        if (request.getPhosphorus() != null
                && request.getPhosphorus() < 0) {

            throw new RuntimeException(
                    "Phosphorus amount cannot be negative"
            );
        }

        if (request.getPotassium() != null
                && request.getPotassium() < 0) {

            throw new RuntimeException(
                    "Potassium amount cannot be negative"
            );
        }

        if (request.getSulphur() != null
                && request.getSulphur() < 0) {

            throw new RuntimeException(
                    "Sulphur amount cannot be negative"
            );
        }

        if (request.getZinc() != null
                && request.getZinc() < 0) {

            throw new RuntimeException(
                    "Zinc amount cannot be negative"
            );
        }
    }


    /*
     * =========================================================
     * VERIFY FARM
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


    /*
     * =========================================================
     * VERIFY CROP
     * =========================================================
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
}