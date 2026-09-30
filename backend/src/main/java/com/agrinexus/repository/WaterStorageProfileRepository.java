package com.agrinexus.repository;

import com.agrinexus.entity.WaterStorageProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WaterStorageProfileRepository
        extends JpaRepository<WaterStorageProfile, UUID> {

    WaterStorageProfile findByFarmId(UUID farmId);

    boolean existsByFarmId(UUID farmId);
}