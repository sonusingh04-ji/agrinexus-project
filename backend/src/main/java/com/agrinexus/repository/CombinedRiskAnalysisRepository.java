package com.agrinexus.repository;

import com.agrinexus.entity.CombinedRiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CombinedRiskAnalysisRepository
        extends JpaRepository<CombinedRiskAnalysis, UUID> {

    List<CombinedRiskAnalysis>
    findByFarmIdOrderByAnalyzedAtDesc(UUID farmId);
}