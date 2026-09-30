package com.agrinexus.repository;

import com.agrinexus.entity.SatelliteFieldData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SatelliteFieldDataRepository
        extends JpaRepository<SatelliteFieldData, UUID> {

    List<SatelliteFieldData> findByFarmIdOrderByRecordedAtDesc(
            UUID farmId
    );
}