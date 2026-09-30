package com.agrinexus.repository;

import com.agrinexus.entity.SolarPumpProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolarPumpProfileRepository
        extends JpaRepository<SolarPumpProfile, UUID> {

    SolarPumpProfile findByFarmId(UUID farmId);

    boolean existsByFarmId(UUID farmId);
}