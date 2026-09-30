package com.agrinexus.repository;

import com.agrinexus.entity.FarmDecisionSimulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FarmDecisionSimulationRepository
        extends JpaRepository<FarmDecisionSimulation, UUID> {

    /*
     * -------------------------------------------------------------
     * Get all simulations for a farm.
     * Newest simulation first.
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdOrderBySimulatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get the latest simulation for a farm.
     * -------------------------------------------------------------
     */
    Optional<FarmDecisionSimulation>
    findFirstByFarmIdOrderBySimulatedAtDesc(
            UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Get simulations for a particular crop.
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdAndCropIdOrderBySimulatedAtDesc(
            UUID farmId,
            UUID cropId
    );

    /*
     * -------------------------------------------------------------
     * Get simulations by scenario.
     *
     * Examples:
     * IRRIGATE_NOW
     * WAIT_FOR_RAIN
     * DELAY_OPERATION
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdAndScenarioOrderBySimulatedAtDesc(
            UUID farmId,
            String scenario
    );

    /*
     * -------------------------------------------------------------
     * Get simulations by conclusion.
     *
     * Examples:
     * SCENARIO_A_FAVORED
     * SCENARIO_B_FAVORED
     * TOO_CLOSE_TO_CALL
     * INSUFFICIENT_DATA
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdAndConclusionOrderBySimulatedAtDesc(
            UUID farmId,
            String conclusion
    );

    /*
     * -------------------------------------------------------------
     * Get simulations by data status.
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdAndDataStatusOrderBySimulatedAtDesc(
            UUID farmId,
            String dataStatus
    );

    /*
     * -------------------------------------------------------------
     * Get simulations by methodology version.
     *
     * Useful for scientific auditability when the simulation
     * methodology changes in future versions.
     * -------------------------------------------------------------
     */
    List<FarmDecisionSimulation>
    findByFarmIdAndMethodologyVersionOrderBySimulatedAtDesc(
            UUID farmId,
            String methodologyVersion
    );
}