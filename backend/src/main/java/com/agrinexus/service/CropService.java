package com.agrinexus.service;

import com.agrinexus.dto.CropRequest;
import com.agrinexus.dto.CropResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.mapper.CropMapper;
import com.agrinexus.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropRepository cropRepository;

    /*
     * Create a new crop knowledge record.
     */
    public CropResponse createCrop(
            CropRequest request
    ) {

        if (request.getName() == null
                || request.getName().isBlank()) {

            throw new RuntimeException(
                    "Crop name is required"
            );
        }

        if (cropRepository.findByName(request.getName()) != null) {

            throw new RuntimeException(
                    "Crop already exists: "
                            + request.getName()
            );
        }

        Crop crop =
                CropMapper.toEntity(request);

        Crop savedCrop =
                cropRepository.save(crop);

        return CropMapper.toResponse(savedCrop);
    }

    /*
     * Get crop by ID.
     */
    public CropResponse getCropById(
            UUID cropId
    ) {

        Crop crop =
                cropRepository.findById(cropId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop not found with ID: "
                                                + cropId
                                )
                        );

        return CropMapper.toResponse(crop);
    }

    /*
     * Get all crops.
     */
    public List<CropResponse> getAllCrops() {

        return cropRepository
                .findAll()
                .stream()
                .map(CropMapper::toResponse)
                .toList();
    }

    /*
     * Get crops by season.
     *
     * Supports values such as:
     * Rabi
     * Kharif
     * Summer
     *
     * Also supports multi-season database values such as:
     * Kharif, Rabi, Spring/Summer
     */
    public List<CropResponse> getCropsBySeason(
            String season
    ) {

        if (season == null
                || season.isBlank()) {

            throw new RuntimeException(
                    "Season is required"
            );
        }

        return cropRepository
                .findBySeasonContainingIgnoreCase(season)
                .stream()
                .map(CropMapper::toResponse)
                .toList();
    }

    /*
     * Get crops by soil type.
     *
     * Example:
     * Loam
     * Clay
     * Sandy-loam
     */
    public List<CropResponse> getCropsBySoilType(
            String soilType
    ) {

        if (soilType == null
                || soilType.isBlank()) {

            throw new RuntimeException(
                    "Soil type is required"
            );
        }

        return cropRepository
                .findBySoilTypeContainingIgnoreCase(
                        soilType
                )
                .stream()
                .map(CropMapper::toResponse)
                .toList();
    }

    /*
     * Get crops suitable for a given soil pH.
     *
     * Example:
     * pH = 6.8
     */
    public List<CropResponse> getCropsBySoilPh(
            Double pH
    ) {

        if (pH == null) {

            throw new RuntimeException(
                    "Soil pH is required"
            );
        }

        if (pH < 0 || pH > 14) {

            throw new RuntimeException(
                    "Soil pH must be between 0 and 14"
            );
        }

        return cropRepository
                .findBySoilPhMinLessThanEqualAndSoilPhMaxGreaterThanEqual(
                        pH,
                        pH
                )
                .stream()
                .map(CropMapper::toResponse)
                .toList();
    }
}