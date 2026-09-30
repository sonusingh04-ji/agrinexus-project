package com.agrinexus.service;

import com.agrinexus.dto.FarmRequest;
import com.agrinexus.dto.FarmResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.mapper.FarmMapper;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;


    /*
     * ============================================================
     * CREATE FARM
     * ============================================================
     *
     * Creates a new farm and stores it in the database.
     */
    public FarmResponse createFarm(
            FarmRequest request
    ) {

        Farm farm =
                FarmMapper.toEntity(request);

        Farm savedFarm =
                farmRepository.save(farm);

        return FarmMapper.toResponse(savedFarm);
    }


    /*
     * ============================================================
     * GET ALL FARMS OF A FARMER
     * ============================================================
     *
     * Returns all farms belonging to a particular farmer.
     *
     * One farmer can have:
     *
     * Farmer
     *   ├── Farm 1
     *   ├── Farm 2
     *   ├── Farm 3
     *   └── Farm 4
     */
    public List<FarmResponse> getFarmsByFarmer(
            UUID farmerId
    ) {

        return farmRepository
                .findByFarmerId(farmerId)
                .stream()
                .map(FarmMapper::toResponse)
                .toList();
    }


    /*
     * ============================================================
     * GET FARM BY ID
     * ============================================================
     *
     * Returns a single farm using its farm ID.
     */
    public FarmResponse getFarmById(
            UUID farmId
    ) {

        Farm farm =
                farmRepository
                        .findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );

        return FarmMapper.toResponse(farm);
    }


    /*
     * ============================================================
     * DELETE FARM
     * ============================================================
     *
     * Deletes a farm using its farm ID.
     *
     * The farm must exist before deletion.
     */
    public void deleteFarm(
            UUID farmId
    ) {

        Farm farm =
                farmRepository
                        .findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );

        farmRepository.delete(farm);
    }
}