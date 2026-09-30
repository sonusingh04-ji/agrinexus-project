package com.agrinexus.service;

import com.agrinexus.dto.IrrigationConfigRequest;
import com.agrinexus.dto.IrrigationConfigResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationConfig;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IrrigationConfigService {

    private final IrrigationConfigRepository repository;
    private final FarmRepository farmRepository;

    public IrrigationConfigResponse create(
            IrrigationConfigRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        if (repository.existsByFarmId(farm.getId())) {

            throw new RuntimeException(
                    "Irrigation configuration already exists for this farm"
            );
        }

        IrrigationConfig config =
                IrrigationConfig.builder()
                        .farm(farm)
                        .fieldCapacity(request.getFieldCapacity())
                        .wiltingPoint(request.getWiltingPoint())
                        .maxAllowableDepletion(
                                request.getMaxAllowableDepletion()
                        )
                        .readilyAvailableWater(
                                request.getReadilyAvailableWater()
                        )
                        .rootZoneDepthCm(
                                request.getRootZoneDepthCm()
                        )
                        .targetFieldCapacityFraction(
                                request.getTargetFieldCapacityFraction()
                        )
                        .forecastHorizonDays(
                                request.getForecastHorizonDays()
                        )
                        .minimumForecastConfidence(
                                request.getMinimumForecastConfidence()
                        )
                        .et0Method(
                                request.getEt0Method()
                        )
                        .sensorBased(
                                request.getSensorBased()
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
                repository.save(config)
        );
    }

    public IrrigationConfigResponse getByFarmId(
            UUID farmId
    ) {

        verifyFarm(farmId);

        IrrigationConfig config =
                repository.findByFarmId(farmId);

        if (config == null) {
            return null;
        }

        return toResponse(config);
    }

    public IrrigationConfigResponse update(
            UUID farmId,
            IrrigationConfigRequest request
    ) {

        validate(request);

        if (!farmId.equals(request.getFarmId())) {

            throw new RuntimeException(
                    "Path farm ID and request farm ID must match"
            );
        }

        IrrigationConfig config =
                repository.findByFarmId(farmId);

        if (config == null) {

            throw new RuntimeException(
                    "Irrigation configuration not found for farm"
            );
        }

        config.setFieldCapacity(
                request.getFieldCapacity()
        );

        config.setWiltingPoint(
                request.getWiltingPoint()
        );

        config.setMaxAllowableDepletion(
                request.getMaxAllowableDepletion()
        );

        config.setReadilyAvailableWater(
                request.getReadilyAvailableWater()
        );

        config.setRootZoneDepthCm(
                request.getRootZoneDepthCm()
        );

        config.setTargetFieldCapacityFraction(
                request.getTargetFieldCapacityFraction()
        );

        config.setForecastHorizonDays(
                request.getForecastHorizonDays()
        );

        config.setMinimumForecastConfidence(
                request.getMinimumForecastConfidence()
        );

        config.setEt0Method(
                request.getEt0Method()
        );

        config.setSensorBased(
                request.getSensorBased()
        );

        config.setSource(
                request.getSource()
        );

        config.setDataStatus(
                request.getDataStatus()
        );

        config.setNotes(
                request.getNotes()
        );

        return toResponse(
                repository.save(config)
        );
    }

    public void delete(
            UUID farmId
    ) {

        IrrigationConfig config =
                repository.findByFarmId(farmId);

        if (config == null) {

            throw new RuntimeException(
                    "Irrigation configuration not found"
            );
        }

        repository.delete(config);
    }

    private void validate(
            IrrigationConfigRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }

        if (request.getFarmId() == null) {
            throw new RuntimeException("Farm ID is required");
        }

        if (request.getFieldCapacity() != null
                && request.getWiltingPoint() != null
                && request.getFieldCapacity()
                <= request.getWiltingPoint()) {

            throw new RuntimeException(
                    "Field capacity must be greater than wilting point"
            );
        }

        validateRange(
                request.getMaxAllowableDepletion(),
                0.0,
                1.0,
                "Maximum allowable depletion"
        );

        validateRange(
                request.getTargetFieldCapacityFraction(),
                0.0,
                1.0,
                "Target field capacity fraction"
        );

        validateRange(
                request.getMinimumForecastConfidence(),
                0.0,
                1.0,
                "Minimum forecast confidence"
        );

        if (request.getForecastHorizonDays() != null
                && (request.getForecastHorizonDays() < 1
                || request.getForecastHorizonDays() > 7)) {

            throw new RuntimeException(
                    "Forecast horizon must be between 1 and 7 days"
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

    private void verifyFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }

    private IrrigationConfigResponse toResponse(
            IrrigationConfig config
    ) {

        return IrrigationConfigResponse.builder()
                .id(config.getId())
                .farmId(
                        config.getFarm() != null
                                ? config.getFarm().getId()
                                : null
                )
                .fieldCapacity(
                        config.getFieldCapacity()
                )
                .wiltingPoint(
                        config.getWiltingPoint()
                )
                .maxAllowableDepletion(
                        config.getMaxAllowableDepletion()
                )
                .readilyAvailableWater(
                        config.getReadilyAvailableWater()
                )
                .rootZoneDepthCm(
                        config.getRootZoneDepthCm()
                )
                .targetFieldCapacityFraction(
                        config.getTargetFieldCapacityFraction()
                )
                .forecastHorizonDays(
                        config.getForecastHorizonDays()
                )
                .minimumForecastConfidence(
                        config.getMinimumForecastConfidence()
                )
                .et0Method(
                        config.getEt0Method()
                )
                .sensorBased(
                        config.getSensorBased()
                )
                .source(
                        config.getSource()
                )
                .dataStatus(
                        config.getDataStatus()
                )
                .notes(
                        config.getNotes()
                )
                .build();
    }
}