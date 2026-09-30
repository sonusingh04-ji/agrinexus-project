package com.agrinexus.service;

import com.agrinexus.dto.CropWaterProfileRequest;
import com.agrinexus.dto.CropWaterProfileResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropWaterProfile;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.CropWaterProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CropWaterProfileService {

    private final CropWaterProfileRepository repository;
    private final CropRepository cropRepository;

    public CropWaterProfileResponse create(CropWaterProfileRequest request) {
        validate(request);

        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new RuntimeException(
                        "Crop not found with ID: " + request.getCropId()));

        List<CropWaterProfile> existing = repository.findByCropId(crop.getId());
        if (existing.stream().anyMatch(profile -> sameScope(
                profile.getStateName(), request.getStateName(),
                profile.getDistrictName(), request.getDistrictName(),
                profile.getSeason(), request.getSeason()))) {
            throw new RuntimeException(
                    "A crop-water profile already exists for this crop and geographic/season scope");
        }

        CropWaterProfile profile = fromRequest(crop, request);
        return toResponse(repository.save(profile));
    }

    public CropWaterProfileResponse getByCropId(UUID cropId) {
        List<CropWaterProfile> profiles = repository.findByCropId(cropId);
        return profiles.stream()
                .sorted(Comparator.comparingInt(this::scopeSpecificity).reversed())
                .map(this::toResponse)
                .findFirst()
                .orElse(null);
    }

    public List<CropWaterProfileResponse> getAllByCropId(UUID cropId) {
        return repository.findByCropIdOrderByIdAsc(cropId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CropWaterProfileResponse resolve(
            UUID cropId,
            String stateName,
            String districtName,
            String season
    ) {
        List<CropWaterProfile> profiles = repository.findByCropId(cropId);

        if (profiles.isEmpty()) {
            return null;
        }

        CropWaterProfile best = profiles.stream()
                .filter(profile -> matches(profile, stateName, districtName, season))
                .max(Comparator.comparingInt(profile -> matchScore(
                        profile, stateName, districtName, season)))
                .orElse(null);

        return best == null ? null : toResponse(best);
    }

    public CropWaterProfileResponse update(UUID profileId, CropWaterProfileRequest request) {
        validate(request);

        CropWaterProfile profile = repository.findById(profileId)
                .orElseThrow(() -> new RuntimeException(
                        "Crop water profile not found with ID: " + profileId));

        if (!profile.getCrop().getId().equals(request.getCropId())) {
            throw new RuntimeException("Profile crop ID and request crop ID must match");
        }

        List<CropWaterProfile> existing = repository.findByCropId(request.getCropId());
        boolean duplicate = existing.stream()
                .filter(item -> !item.getId().equals(profileId))
                .anyMatch(item -> sameScope(
                        item.getStateName(), request.getStateName(),
                        item.getDistrictName(), request.getDistrictName(),
                        item.getSeason(), request.getSeason()));

        if (duplicate) {
            throw new RuntimeException(
                    "A crop-water profile already exists for this crop and geographic/season scope");
        }

        apply(profile, request);
        return toResponse(repository.save(profile));
    }

    public void delete(UUID profileId) {
        if (!repository.existsById(profileId)) {
            throw new RuntimeException("Crop water profile not found");
        }
        repository.deleteById(profileId);
    }

    private CropWaterProfile fromRequest(Crop crop, CropWaterProfileRequest request) {
        CropWaterProfile profile = new CropWaterProfile();
        profile.setCrop(crop);
        apply(profile, request);
        return profile;
    }

    private void apply(CropWaterProfile profile, CropWaterProfileRequest request) {
        profile.setStateName(clean(request.getStateName()));
        profile.setDistrictName(clean(request.getDistrictName()));
        profile.setSeason(clean(request.getSeason()));

        profile.setKcInitial(request.getKcInitial());
        profile.setKcDevelopment(request.getKcDevelopment());
        profile.setKcMid(request.getKcMid());
        profile.setKcEnd(request.getKcEnd());

        profile.setRootDepthInitialCm(request.getRootDepthInitialCm());
        profile.setRootDepthMidCm(request.getRootDepthMidCm());
        profile.setRootDepthEndCm(request.getRootDepthEndCm());

        profile.setInitialDays(request.getInitialDays());
        profile.setDevelopmentDays(request.getDevelopmentDays());
        profile.setMidDays(request.getMidDays());
        profile.setLateDays(request.getLateDays());

        profile.setSource(clean(request.getSource()));
        profile.setSourceDocument(clean(request.getSourceDocument()));
        profile.setSourceUrl(clean(request.getSourceUrl()));
        profile.setPublicationYear(request.getPublicationYear());
        profile.setDataStatus(clean(request.getDataStatus()));
        profile.setVersion(clean(request.getVersion()));
        profile.setValidFrom(clean(request.getValidFrom()));
        profile.setValidTo(clean(request.getValidTo()));
        profile.setNotes(clean(request.getNotes()));
    }

    private void validate(CropWaterProfileRequest request) {
        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }
        if (request.getCropId() == null) {
            throw new RuntimeException("Crop ID is required");
        }

        if (allNull(
                request.getKcInitial(), request.getKcDevelopment(),
                request.getKcMid(), request.getKcEnd(),
                request.getRootDepthInitialCm(), request.getRootDepthMidCm(),
                request.getRootDepthEndCm(), request.getInitialDays(),
                request.getDevelopmentDays(), request.getMidDays(),
                request.getLateDays())) {
            throw new RuntimeException(
                    "At least one crop-water reference value is required");
        }

        validateKc(request.getKcInitial(), "Kc Initial");
        validateKc(request.getKcDevelopment(), "Kc Development");
        validateKc(request.getKcMid(), "Kc Mid");
        validateKc(request.getKcEnd(), "Kc End");

        validatePositive(request.getRootDepthInitialCm(), "Initial root depth");
        validatePositive(request.getRootDepthMidCm(), "Mid root depth");
        validatePositive(request.getRootDepthEndCm(), "End root depth");

        validateDays(request.getInitialDays(), "Initial days");
        validateDays(request.getDevelopmentDays(), "Development days");
        validateDays(request.getMidDays(), "Mid days");
        validateDays(request.getLateDays(), "Late days");

        if (request.getPublicationYear() != null &&
                (request.getPublicationYear() < 1900 || request.getPublicationYear() > 2100)) {
            throw new RuntimeException("Publication year is out of range");
        }
    }

    private boolean allNull(Object... values) {
        for (Object value : values) {
            if (value != null) return false;
        }
        return true;
    }

    private void validateKc(Double value, String field) {
        if (value != null && (value < 0 || value > 2.5)) {
            throw new RuntimeException(field + " must be between 0 and 2.5");
        }
    }

    private void validatePositive(Double value, String field) {
        if (value != null && value <= 0) {
            throw new RuntimeException(field + " must be greater than zero");
        }
    }

    private void validateDays(Integer value, String field) {
        if (value != null && value < 0) {
            throw new RuntimeException(field + " cannot be negative");
        }
    }

    private boolean sameScope(
            String stateA, String stateB,
            String districtA, String districtB,
            String seasonA, String seasonB
    ) {
        return equalNormalized(stateA, stateB)
                && equalNormalized(districtA, districtB)
                && equalNormalized(seasonA, seasonB);
    }

    private boolean matches(
            CropWaterProfile profile,
            String stateName,
            String districtName,
            String season
    ) {
        return matchesScope(profile.getStateName(), stateName)
                && matchesScope(profile.getDistrictName(), districtName)
                && matchesScope(profile.getSeason(), season);
    }

    private boolean matchesScope(String profileValue, String requestedValue) {
        String p = normalize(profileValue);
        String r = normalize(requestedValue);
        return p.isEmpty() || (!r.isEmpty() && p.equals(r));
    }

    private int matchScore(
            CropWaterProfile profile,
            String stateName,
            String districtName,
            String season
    ) {
        int score = 0;
        if (equalsRequested(profile.getStateName(), stateName)) score += 4;
        if (equalsRequested(profile.getDistrictName(), districtName)) score += 4;
        if (equalsRequested(profile.getSeason(), season)) score += 2;
        if (isBlank(profile.getStateName())) score += 1;
        if (isBlank(profile.getDistrictName())) score += 1;
        if (isBlank(profile.getSeason())) score += 1;
        return score;
    }

    private int scopeSpecificity(CropWaterProfile profile) {
        int score = 0;
        if (!isBlank(profile.getStateName())) score += 4;
        if (!isBlank(profile.getDistrictName())) score += 4;
        if (!isBlank(profile.getSeason())) score += 2;
        return score;
    }

    private boolean equalsRequested(String profileValue, String requestedValue) {
        return !isBlank(profileValue)
                && !isBlank(requestedValue)
                && normalize(profileValue).equals(normalize(requestedValue));
    }

    private boolean equalNormalized(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String clean(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private CropWaterProfileResponse toResponse(CropWaterProfile profile) {
        return CropWaterProfileResponse.builder()
                .id(profile.getId())
                .cropId(profile.getCrop() != null ? profile.getCrop().getId() : null)
                .cropName(profile.getCrop() != null ? profile.getCrop().getName() : null)
                .stateName(profile.getStateName())
                .districtName(profile.getDistrictName())
                .season(profile.getSeason())
                .kcInitial(profile.getKcInitial())
                .kcDevelopment(profile.getKcDevelopment())
                .kcMid(profile.getKcMid())
                .kcEnd(profile.getKcEnd())
                .rootDepthInitialCm(profile.getRootDepthInitialCm())
                .rootDepthMidCm(profile.getRootDepthMidCm())
                .rootDepthEndCm(profile.getRootDepthEndCm())
                .initialDays(profile.getInitialDays())
                .developmentDays(profile.getDevelopmentDays())
                .midDays(profile.getMidDays())
                .lateDays(profile.getLateDays())
                .source(profile.getSource())
                .sourceDocument(profile.getSourceDocument())
                .sourceUrl(profile.getSourceUrl())
                .publicationYear(profile.getPublicationYear())
                .dataStatus(profile.getDataStatus())
                .version(profile.getVersion())
                .validFrom(profile.getValidFrom())
                .validTo(profile.getValidTo())
                .notes(profile.getNotes())
                .build();
    }
}
