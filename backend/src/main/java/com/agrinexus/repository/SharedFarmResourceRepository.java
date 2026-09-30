package com.agrinexus.repository;

import com.agrinexus.entity.SharedFarmResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SharedFarmResourceRepository
        extends JpaRepository<SharedFarmResource, UUID> {

    List<SharedFarmResource>
    findByCooperativeId(UUID cooperativeId);

    List<SharedFarmResource>
    findByCooperativeIdAndStatusIgnoreCase(
            UUID cooperativeId,
            String status
    );

    List<SharedFarmResource>
    findByResourceTypeIgnoreCase(
            String resourceType
    );
}