package com.agrinexus.repository;

import com.agrinexus.entity.CropWaterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CropWaterProfileRepository
        extends JpaRepository<CropWaterProfile, UUID> {

    List<CropWaterProfile> findByCropId(UUID cropId);

    List<CropWaterProfile> findByCropIdOrderByIdAsc(UUID cropId);

    long countByCropId(UUID cropId);
}
