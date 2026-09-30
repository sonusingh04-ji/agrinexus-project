package com.agrinexus.repository;

import com.agrinexus.entity.IrrigationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IrrigationConfigRepository
        extends JpaRepository<IrrigationConfig, UUID> {

    /*
     * Get irrigation configuration for a farm.
     *
     * Farm has one active configuration in the current MVP.
     */
    IrrigationConfig findByFarmId(
            UUID farmId
    );

    /*
     * Check whether configuration already exists
     * for a farm.
     */
    boolean existsByFarmId(
            UUID farmId
    );
}