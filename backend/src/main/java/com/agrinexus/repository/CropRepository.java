package com.agrinexus.repository;

import com.agrinexus.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropRepository
        extends JpaRepository<Crop, UUID> {

    Crop findByName(String name);

    Crop findByNameIgnoreCase(String name);

    List<Crop> findBySeasonContainingIgnoreCase(
            String season
    );

    List<Crop> findBySoilTypeContainingIgnoreCase(
            String soilType
    );

    List<Crop>
    findBySoilPhMinLessThanEqualAndSoilPhMaxGreaterThanEqual(
            Double pH,
            Double pH2
    );
}