package com.agrinexus.service;

import com.agrinexus.dto.SolarPumpProfileRequest;
import com.agrinexus.dto.SolarPumpProfileResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SolarPumpProfile;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SolarPumpProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolarPumpProfileService {

    private final SolarPumpProfileRepository repository;
    private final FarmRepository farmRepository;

    public SolarPumpProfileResponse create(
            SolarPumpProfileRequest request
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
                    "Solar pump profile already exists"
            );
        }

        SolarPumpProfile profile =
                SolarPumpProfile.builder()
                        .farm(farm)
                        .panelAreaM2(
                                request.getPanelAreaM2()
                        )
                        .panelEfficiencyPercent(
                                request.getPanelEfficiencyPercent()
                        )
                        .ratedSolarPowerKw(
                                request.getRatedSolarPowerKw()
                        )
                        .pumpRatedPowerKw(
                                request.getPumpRatedPowerKw()
                        )
                        .pumpEfficiencyPercent(
                                request.getPumpEfficiencyPercent()
                        )
                        .pumpHeadMeters(
                                request.getPumpHeadMeters()
                        )
                        .pumpRatedFlowLpm(
                                request.getPumpRatedFlowLpm()
                        )
                        .batteryCapacityKwh(
                                request.getBatteryCapacityKwh()
                        )
                        .batterySocPercent(
                                request.getBatterySocPercent()
                        )
                        .tankCapacityLiters(
                                request.getTankCapacityLiters()
                        )
                        .currentTankLevelLiters(
                                request.getCurrentTankLevelLiters()
                        )
                        .minimumSolarIrradianceWm2(
                                request.getMinimumSolarIrradianceWm2()
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

    public SolarPumpProfileResponse getByFarmId(
            UUID farmId
    ) {

        SolarPumpProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {
            return null;
        }

        return toResponse(profile);
    }

    public SolarPumpProfileResponse update(
            UUID farmId,
            SolarPumpProfileRequest request
    ) {

        validate(request);

        SolarPumpProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {

            throw new RuntimeException(
                    "Solar pump profile not found"
            );
        }

        profile.setPanelAreaM2(
                request.getPanelAreaM2()
        );

        profile.setPanelEfficiencyPercent(
                request.getPanelEfficiencyPercent()
        );

        profile.setRatedSolarPowerKw(
                request.getRatedSolarPowerKw()
        );

        profile.setPumpRatedPowerKw(
                request.getPumpRatedPowerKw()
        );

        profile.setPumpEfficiencyPercent(
                request.getPumpEfficiencyPercent()
        );

        profile.setPumpHeadMeters(
                request.getPumpHeadMeters()
        );

        profile.setPumpRatedFlowLpm(
                request.getPumpRatedFlowLpm()
        );

        profile.setBatteryCapacityKwh(
                request.getBatteryCapacityKwh()
        );

        profile.setBatterySocPercent(
                request.getBatterySocPercent()
        );

        profile.setTankCapacityLiters(
                request.getTankCapacityLiters()
        );

        profile.setCurrentTankLevelLiters(
                request.getCurrentTankLevelLiters()
        );

        profile.setMinimumSolarIrradianceWm2(
                request.getMinimumSolarIrradianceWm2()
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

        SolarPumpProfile profile =
                repository.findByFarmId(farmId);

        if (profile == null) {

            throw new RuntimeException(
                    "Solar pump profile not found"
            );
        }

        repository.delete(profile);
    }

    private void validate(
            SolarPumpProfileRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }

        if (request.getFarmId() == null) {
            throw new RuntimeException("Farm ID is required");
        }

        validateRange(
                request.getPanelEfficiencyPercent(),
                0.0,
                100.0,
                "Panel efficiency"
        );

        validateRange(
                request.getPumpEfficiencyPercent(),
                0.0,
                100.0,
                "Pump efficiency"
        );

        validateRange(
                request.getBatterySocPercent(),
                0.0,
                100.0,
                "Battery SoC"
        );

        validateNonNegative(
                request.getPanelAreaM2(),
                "Panel area"
        );

        validateNonNegative(
                request.getRatedSolarPowerKw(),
                "Rated solar power"
        );

        validateNonNegative(
                request.getPumpRatedPowerKw(),
                "Pump rated power"
        );

        validateNonNegative(
                request.getPumpHeadMeters(),
                "Pump head"
        );

        validateNonNegative(
                request.getPumpRatedFlowLpm(),
                "Pump flow"
        );

        validateNonNegative(
                request.getBatteryCapacityKwh(),
                "Battery capacity"
        );

        validateNonNegative(
                request.getTankCapacityLiters(),
                "Tank capacity"
        );

        validateNonNegative(
                request.getCurrentTankLevelLiters(),
                "Current tank level"
        );

        validateNonNegative(
                request.getMinimumSolarIrradianceWm2(),
                "Minimum solar irradiance"
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

    private SolarPumpProfileResponse toResponse(
            SolarPumpProfile profile
    ) {

        return SolarPumpProfileResponse.builder()
                .id(profile.getId())
                .farmId(
                        profile.getFarm() != null
                                ? profile.getFarm().getId()
                                : null
                )
                .panelAreaM2(
                        profile.getPanelAreaM2()
                )
                .panelEfficiencyPercent(
                        profile.getPanelEfficiencyPercent()
                )
                .ratedSolarPowerKw(
                        profile.getRatedSolarPowerKw()
                )
                .pumpRatedPowerKw(
                        profile.getPumpRatedPowerKw()
                )
                .pumpEfficiencyPercent(
                        profile.getPumpEfficiencyPercent()
                )
                .pumpHeadMeters(
                        profile.getPumpHeadMeters()
                )
                .pumpRatedFlowLpm(
                        profile.getPumpRatedFlowLpm()
                )
                .batteryCapacityKwh(
                        profile.getBatteryCapacityKwh()
                )
                .batterySocPercent(
                        profile.getBatterySocPercent()
                )
                .tankCapacityLiters(
                        profile.getTankCapacityLiters()
                )
                .currentTankLevelLiters(
                        profile.getCurrentTankLevelLiters()
                )
                .minimumSolarIrradianceWm2(
                        profile.getMinimumSolarIrradianceWm2()
                )
                .source(profile.getSource())
                .dataStatus(profile.getDataStatus())
                .notes(profile.getNotes())
                .build();
    }
}