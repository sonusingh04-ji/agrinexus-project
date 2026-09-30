package com.agrinexus.repository;

import com.agrinexus.entity.SoilSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SoilSampleRepository
        extends JpaRepository<SoilSample, UUID> {

    /*
     * -------------------------------------------------------------
     * All soil samples for a farm.
     *
     * Dated records first.
     * NULL dates last.
     * -------------------------------------------------------------
     */
    @Query("""
            SELECT s
            FROM SoilSample s
            WHERE s.farm.id = :farmId
            ORDER BY
                CASE WHEN s.sampleDate IS NULL THEN 1 ELSE 0 END,
                s.sampleDate DESC
            """)
    List<SoilSample> findByFarmIdOrderBySampleDateDesc(
            @Param("farmId") UUID farmId
    );

    /*
     * -------------------------------------------------------------
     * Latest valid soil sample.
     *
     * Dated samples take priority over undated samples.
     * -------------------------------------------------------------
     */
    default SoilSample findFirstByFarmIdOrderBySampleDateDesc(
            UUID farmId
    ) {

        return findByFarmIdOrderBySampleDateDesc(
                farmId
        )
                .stream()
                .findFirst()
                .orElse(null);
    }

    /*
     * -------------------------------------------------------------
     * Soil samples by source.
     * -------------------------------------------------------------
     */
    List<SoilSample>
    findByFarmIdAndSourceIgnoreCaseOrderBySampleDateDesc(
            UUID farmId,
            String source
    );

    /*
     * -------------------------------------------------------------
     * Soil samples within date range.
     * -------------------------------------------------------------
     */
    List<SoilSample>
    findByFarmIdAndSampleDateBetweenOrderBySampleDateDesc(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    );
}