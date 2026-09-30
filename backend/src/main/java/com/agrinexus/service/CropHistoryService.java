package com.agrinexus.service;

import com.agrinexus.dto.CropHistoryRequest;
import com.agrinexus.dto.CropHistoryResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.CropHistory;
import com.agrinexus.entity.Farm;
import com.agrinexus.mapper.CropHistoryMapper;
import com.agrinexus.repository.CropHistoryRepository;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CropHistoryService {

    private final CropHistoryRepository cropHistoryRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;

    /*
     * Create a crop history record.
     */
    @Transactional
    public CropHistoryResponse createCropHistory(
            CropHistoryRequest request
    ) {

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

        CropHistory history =
                CropHistoryMapper.toEntity(
                        request,
                        farm,
                        crop
                );

        CropHistory savedHistory =
                cropHistoryRepository.save(history);

        return CropHistoryMapper.toResponse(
                savedHistory
        );
    }

    /*
     * Get crop history by ID.
     */
    public CropHistoryResponse getCropHistoryById(
            UUID historyId
    ) {

        CropHistory history =
                cropHistoryRepository.findById(historyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop history not found with ID: "
                                                + historyId
                                )
                        );

        return CropHistoryMapper.toResponse(history);
    }

    /*
     * Get complete crop history for a farm.
     */
    public List<CropHistoryResponse> getFarmCropHistory(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        return cropHistoryRepository
                .findByFarmIdOrderBySowingDateDesc(farmId)
                .stream()
                .map(CropHistoryMapper::toResponse)
                .toList();
    }

    /*
     * Get the latest crop history for a farm.
     */
    public CropHistoryResponse getLatestCropHistory(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        CropHistory history =
                cropHistoryRepository
                        .findFirstByFarmIdOrderBySowingDateDesc(
                                farmId
                        );

        if (history == null) {
            return null;
        }

        return CropHistoryMapper.toResponse(history);
    }

    /*
     * Get crop history for a specific crop.
     */
    public List<CropHistoryResponse> getCropHistoryByCrop(
            UUID cropId
    ) {

        return cropHistoryRepository
                .findByCropIdOrderBySowingDateDesc(cropId)
                .stream()
                .map(CropHistoryMapper::toResponse)
                .toList();
    }

    /*
     * Get farm crop cycles where crop residue is present.
     */
    public List<CropHistoryResponse> getFarmHistoryWithResidue(
            UUID farmId
    ) {

        verifyFarmExists(farmId);

        return cropHistoryRepository
                .findByFarmIdAndResiduePresentTrueOrderBySowingDateDesc(
                        farmId
                )
                .stream()
                .map(CropHistoryMapper::toResponse)
                .toList();
    }

    /*
     * Update an existing crop history record.
     */
    public CropHistoryResponse updateCropHistory(
            UUID historyId,
            CropHistoryRequest request
    ) {

        CropHistory history =
                cropHistoryRepository.findById(historyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop history not found with ID: "
                                                + historyId
                                )
                        );

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

        history.setFarm(farm);
        history.setCrop(crop);
        history.setSeason(request.getSeason());
        history.setSowingDate(request.getSowingDate());
        history.setHarvestDate(request.getHarvestDate());
        history.setCropStage(request.getCropStage());
        history.setResiduePresent(
                request.getResiduePresent() != null
                        ? request.getResiduePresent()
                        : false
        );
        history.setResidueType(request.getResidueType());
        history.setNotes(request.getNotes());

        CropHistory updatedHistory =
                cropHistoryRepository.save(history);

        return CropHistoryMapper.toResponse(
                updatedHistory
        );
    }

    /*
     * Delete crop history.
     */
    public void deleteCropHistory(
            UUID historyId
    ) {

        if (!cropHistoryRepository.existsById(historyId)) {
            throw new RuntimeException(
                    "Crop history not found with ID: "
                            + historyId
            );
        }

        cropHistoryRepository.deleteById(historyId);
    }

    /*
     * Verify that a farm exists.
     */
    private void verifyFarmExists(
            UUID farmId
    ) {

        if (!farmRepository.existsById(farmId)) {
            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }
}