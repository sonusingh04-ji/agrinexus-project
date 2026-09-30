package com.agrinexus.repository;

import com.agrinexus.entity.AffordableCropOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AffordableCropOptionRepository
        extends JpaRepository<AffordableCropOption, UUID> {

    List<AffordableCropOption>
    findByCropId(UUID cropId);

    List<AffordableCropOption>
    findByCrop_SeasonIgnoreCase(String season);

    List<AffordableCropOption>
    findByWaterDemandClassIgnoreCase(
            String waterDemandClass
    );

    AffordableCropOption
    findFirstByCropId(UUID cropId);
}