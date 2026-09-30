package com.agrinexus.service;

import com.agrinexus.dto.WaterStorageProfileRequest;
import com.agrinexus.dto.WaterStorageProfileResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WaterStorageProfile;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WaterStorageProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaterStorageProfileService {

    private final WaterStorageProfileRepository repository;
    private final FarmRepository farmRepository;

    public WaterStorageProfileResponse create(
            WaterStorageProfileRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found"
                                )
                        );

        if (repository.existsByFarmId(farm.getId())) {

            throw new RuntimeException(
                    "Water storage profile already exists"
            );
        }

        WaterStorageProfile profile =
                WaterStorageProfile.builder()
                        .farm(farm)
                        .catchmentAreaM2(
                                request.getCatchmentAreaM2()
                        )
                        .slopePercent(
                                request.getSlopePercent()
                        )
                        .runoffCoefficient(
                                request.getRunoffCoefficient()
                        )
                        .infiltrationRateMmPerHour(
                                request.getInfiltrationRateMmPerHour()
                        )
                        .storageCapacityLiters(
                                request.getStorageCapacityLiters()
                        )
                        .currentStorageLiters(
                                request.getCurrentStorageLiters()
                        )
                        .storageSafetyLimitPercent(
                                request.getStorageSafetyLimitPercent()
                        )
                        .storageType(
                                request.getStorageType()
                        )
                        .source(
                                request.getSource()
                        )
                        .dataStatus(
                                request.getDataStatus()
                        )
                        .notes(
                                request.getNotes()
                        )
                        .build();

        return toResponse(
                repository.save(profile)
        );
    }

    public WaterStorageProfileResponse getByFarmId(
            UUID farmId
    ) {

        WaterStorageProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {
            return null;
        }

        return toResponse(profile);
    }

    public WaterStorageProfileResponse update(
            UUID farmId,
            WaterStorageProfileRequest request
    ) {

        validate(request);

        WaterStorageProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {

            throw new RuntimeException(
                    "Water storage profile not found"
            );
        }

        profile.setCatchmentAreaM2(
                request.getCatchmentAreaM2()
        );

        profile.setSlopePercent(
                request.getSlopePercent()
        );

        profile.setRunoffCoefficient(
                request.getRunoffCoefficient()
        );

        profile.setInfiltrationRateMmPerHour(
                request.getInfiltrationRateMmPerHour()
        );

        profile.setStorageCapacityLiters(
                request.getStorageCapacityLiters()
        );

        profile.setCurrentStorageLiters(
                request.getCurrentStorageLiters()
        );

        profile.setStorageSafetyLimitPercent(
                request.getStorageSafetyLimitPercent()
        );

        profile.setStorageType(
                request.getStorageType()
        );

        profile.setSource(
                request.getSource()
        );

        profile.setDataStatus(
                request.getDataStatus()
        );

        profile.setNotes(
                request.getNotes()
        );

        return toResponse(
                repository.save(profile)
        );
    }

    public void delete(
            UUID farmId
    ) {

        WaterStorageProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {

            throw new RuntimeException(
                    "Water storage profile not found"
            );
        }

        repository.delete(profile);
    }

    private void validate(
            WaterStorageProfileRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }

        if (request.getFarmId() == null) {
            throw new RuntimeException("Farm ID is required");
        }

        validateNonNegative(
                request.getCatchmentAreaM2(),
                "Catchment area"
        );

        validateNonNegative(
                request.getSlopePercent(),
                "Slope"
        );

        validateRange(
                request.getRunoffCoefficient(),
                0.0,
                1.0,
                "Runoff coefficient"
        );

        validateNonNegative(
                request.getInfiltrationRateMmPerHour(),
                "Infiltration rate"
        );

        validateNonNegative(
                request.getStorageCapacityLiters(),
                "Storage capacity"
        );

        validateNonNegative(
                request.getCurrentStorageLiters(),
                "Current storage"
        );

        validateRange(
                request.getStorageSafetyLimitPercent(),
                0.0,
                100.0,
                "Storage safety limit"
        );
    }

    private void validateNonNegative(
            Double value,
            String field
    ) {

        if (value != null && value < 0) {

            throw new RuntimeException(
                    field + " cannot be negative"
            );
        }
    }

    private void validateRange(
            Double value,
            double min,
            double max,
            String field
    ) {

        if (value != null
                && (value < min || value > max)) {

            throw new RuntimeException(
                    field
                            + " must be between "
                            + min
                            + " and "
                            + max
            );
        }
    }

    private WaterStorageProfileResponse toResponse(
            WaterStorageProfile profile
    ) {

        return WaterStorageProfileResponse.builder()
                .id(profile.getId())
                .farmId(
                        profile.getFarm() != null
                                ? profile.getFarm().getId()
                                : null
                )
                .catchmentAreaM2(
                        profile.getCatchmentAreaM2()
                )
                .slopePercent(
                        profile.getSlopePercent()
                )
                .runoffCoefficient(
                        profile.getRunoffCoefficient()
                )
                .infiltrationRateMmPerHour(
                        profile.getInfiltrationRateMmPerHour()
                )
                .storageCapacityLiters(
                        profile.getStorageCapacityLiters()
                )
                .currentStorageLiters(
                        profile.getCurrentStorageLiters()
                )
                .storageSafetyLimitPercent(
                        profile.getStorageSafetyLimitPercent()
                )
                .storageType(
                        profile.getStorageType()
                )
                .source(profile.getSource())
                .dataStatus(profile.getDataStatus())
                .notes(profile.getNotes())
                .build();
    }
}