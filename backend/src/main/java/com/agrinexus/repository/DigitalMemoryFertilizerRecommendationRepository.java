package com.agrinexus.repository;

import com.agrinexus.entity.FertilizerRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryFertilizerRecommendationRepository
        extends JpaRepository<FertilizerRecommendation, UUID> {

    List<FertilizerRecommendation>
    findByFarmIdOrderByGeneratedAtDesc(
            UUID farmId
    );
}