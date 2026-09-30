package com.agrinexus.repository;

import com.agrinexus.entity.SharedResourceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SharedResourceUsageRepository
        extends JpaRepository<SharedResourceUsage, UUID> {

    List<SharedResourceUsage>
    findByResourceIdOrderByStartTimeDesc(
            UUID resourceId
    );

    List<SharedResourceUsage>
    findByFarmerIdOrderByStartTimeDesc(
            UUID farmerId
    );

    List<SharedResourceUsage>
    findByFarmIdOrderByStartTimeDesc(
            UUID farmId
    );

    List<SharedResourceUsage>
    findByResourceIdAndStartTimeBetween(
            UUID resourceId,
            LocalDateTime start,
            LocalDateTime end
    );
}