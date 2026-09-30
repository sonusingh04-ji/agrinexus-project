package com.agrinexus.repository;

import com.agrinexus.entity.NutrientBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DigitalMemoryNutrientBalanceRepository
        extends JpaRepository<NutrientBalance, UUID> {

    List<NutrientBalance>
    findByFarmIdOrderByPeriodStartDesc(
            UUID farmId
    );
}