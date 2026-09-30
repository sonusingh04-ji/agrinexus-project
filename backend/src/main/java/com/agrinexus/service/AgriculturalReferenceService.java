package com.agrinexus.service;

import com.agrinexus.dto.AgriculturalReferenceRequest;
import com.agrinexus.dto.AgriculturalReferenceResponse;
import com.agrinexus.entity.AgriculturalReference;
import com.agrinexus.entity.Crop;
import com.agrinexus.enums.AgriculturalReferenceType;
import com.agrinexus.mapper.AgriculturalReferenceMapper;
import com.agrinexus.repository.AgriculturalReferenceRepository;
import com.agrinexus.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgriculturalReferenceService {

    private final AgriculturalReferenceRepository repository;
    private final CropRepository cropRepository;

    @Transactional
    public AgriculturalReferenceResponse create(AgriculturalReferenceRequest request) {
        validate(request);

        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new RuntimeException(
                        "Crop not found with ID: " + request.getCropId()));

        boolean duplicate = repository.findByCropIdAndReferenceType(
                        request.getCropId(), request.getReferenceType())
                .stream()
                .anyMatch(item -> sameScope(item, request));

        if (duplicate) {
            throw new RuntimeException(
                    "An agricultural reference already exists for this crop, type and scope");
        }

        return AgriculturalReferenceMapper.toResponse(
                repository.save(AgriculturalReferenceMapper.toEntity(crop, request))
        );
    }

    @Transactional(readOnly = true)
    public AgriculturalReferenceResponse getById(UUID id) {
        return repository.findById(id)
                .map(AgriculturalReferenceMapper::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AgriculturalReferenceResponse> getByCrop(UUID cropId) {
        return repository.findByCropId(cropId)
                .stream()
                .map(AgriculturalReferenceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgriculturalReferenceResponse> getByCropAndType(
            UUID cropId,
            AgriculturalReferenceType type
    ) {
        return repository.findByCropIdAndReferenceType(cropId, type)
                .stream()
                .map(AgriculturalReferenceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgriculturalReferenceResponse resolve(
            UUID cropId,
            AgriculturalReferenceType type,
            String stateName,
            String districtName,
            String subDistrictName,
            String season,
            String cropStage
    ) {
        List<AgriculturalReference> references =
                repository.findByCropIdAndReferenceType(cropId, type);

        return references.stream()
                .filter(item -> matches(item.getStateName(), stateName))
                .filter(item -> matches(item.getDistrictName(), districtName))
                .filter(item -> matches(item.getSubDistrictName(), subDistrictName))
                .filter(item -> matches(item.getSeason(), season))
                .filter(item -> matches(item.getCropStage(), cropStage))
                .max(Comparator.comparingInt(item -> score(
                        item,
                        stateName,
                        districtName,
                        subDistrictName,
                        season,
                        cropStage
                )))
                .map(AgriculturalReferenceMapper::toResponse)
                .orElse(null);
    }

    @Transactional
    public AgriculturalReferenceResponse update(
            UUID id,
            AgriculturalReferenceRequest request
    ) {
        validate(request);

        AgriculturalReference reference = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Agricultural reference not found with ID: " + id));

        if (!reference.getCrop().getId().equals(request.getCropId())) {
            throw new RuntimeException("Reference crop ID and request crop ID must match");
        }

        boolean duplicate = repository.findByCropIdAndReferenceType(
                        request.getCropId(), request.getReferenceType())
                .stream()
                .filter(item -> !item.getId().equals(id))
                .anyMatch(item -> sameScope(item, request));

        if (duplicate) {
            throw new RuntimeException(
                    "An agricultural reference already exists for this crop, type and scope");
        }

        AgriculturalReferenceMapper.apply(reference, request);
        return AgriculturalReferenceMapper.toResponse(repository.save(reference));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Agricultural reference not found");
        }
        repository.deleteById(id);
    }

    private void validate(AgriculturalReferenceRequest request) {
        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }
        if (request.getCropId() == null) {
            throw new RuntimeException("Crop ID is required");
        }
        if (request.getReferenceType() == null) {
            throw new RuntimeException("Reference type is required");
        }

        validateRange(request.getSoilPhMin(), request.getSoilPhMax(), "Soil pH");
        validateRange(request.getTemperatureMin(), request.getTemperatureMax(), "Temperature");
        validateNonNegative(request.getNitrogen(), "Nitrogen");
        validateNonNegative(request.getPhosphorus(), "Phosphorus");
        validateNonNegative(request.getPotassium(), "Potassium");
        validateNonNegative(request.getSulphur(), "Sulphur");
        validateNonNegative(request.getZinc(), "Zinc");

        validateKc(request.getKcInitial(), "Kc Initial");
        validateKc(request.getKcDevelopment(), "Kc Development");
        validateKc(request.getKcMid(), "Kc Mid");
        validateKc(request.getKcEnd(), "Kc End");

        validatePositive(request.getRootDepthInitialCm(), "Initial root depth");
        validatePositive(request.getRootDepthMidCm(), "Mid root depth");
        validatePositive(request.getRootDepthEndCm(), "End root depth");

        if (request.getPublicationYear() != null
                && (request.getPublicationYear() < 1900 || request.getPublicationYear() > 2100)) {
            throw new RuntimeException("Publication year is out of range");
        }
    }

    private boolean sameScope(
            AgriculturalReference reference,
            AgriculturalReferenceRequest request
    ) {
        return equal(reference.getStateName(), request.getStateName())
                && equal(reference.getDistrictName(), request.getDistrictName())
                && equal(reference.getSubDistrictName(), request.getSubDistrictName())
                && equal(reference.getSeason(), request.getSeason())
                && equal(reference.getCropStage(), request.getCropStage());
    }

    private boolean matches(String referenceValue, String requestedValue) {
        if (referenceValue == null || referenceValue.isBlank()) {
            return true;
        }
        return requestedValue != null
                && normalize(referenceValue).equals(normalize(requestedValue));
    }

    private int score(
            AgriculturalReference reference,
            String stateName,
            String districtName,
            String subDistrictName,
            String season,
            String cropStage
    ) {
        int score = 0;
        if (equalRequested(reference.getStateName(), stateName)) score += 16;
        if (equalRequested(reference.getDistrictName(), districtName)) score += 8;
        if (equalRequested(reference.getSubDistrictName(), subDistrictName)) score += 4;
        if (equalRequested(reference.getSeason(), season)) score += 2;
        if (equalRequested(reference.getCropStage(), cropStage)) score += 1;
        return score;
    }

    private boolean equalRequested(String stored, String requested) {
        return stored != null && !stored.isBlank()
                && requested != null && !requested.isBlank()
                && normalize(stored).equals(normalize(requested));
    }

    private boolean equal(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private void validateRange(Double min, Double max, String field) {
        if (min != null && max != null && min > max) {
            throw new RuntimeException(field + " minimum cannot exceed maximum");
        }
    }

    private void validateNonNegative(Double value, String field) {
        if (value != null && (value < 0 || value.isNaN() || value.isInfinite())) {
            throw new RuntimeException(field + " must be a finite non-negative number");
        }
    }

    private void validatePositive(Double value, String field) {
        if (value != null && (value <= 0 || value.isNaN() || value.isInfinite())) {
            throw new RuntimeException(field + " must be a finite positive number");
        }
    }

    private void validateKc(Double value, String field) {
        if (value != null && (value < 0 || value > 2.5 || value.isNaN() || value.isInfinite())) {
            throw new RuntimeException(field + " must be finite and between 0 and 2.5");
        }
    }
}
