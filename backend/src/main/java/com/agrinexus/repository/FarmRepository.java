package com.agrinexus.repository;

import com.agrinexus.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FarmRepository extends JpaRepository<Farm, UUID> {

    List<Farm> findByFarmerId(UUID farmerId);
}